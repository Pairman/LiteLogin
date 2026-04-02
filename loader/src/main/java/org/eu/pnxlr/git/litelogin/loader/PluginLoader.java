package org.eu.pnxlr.git.litelogin.loader;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.main.CoreAPI;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlugin;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.api.internal.util.IOUtil;
import org.eu.pnxlr.git.litelogin.loader.classloader.PriorAllURLClassLoader;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the plugin loader.
 */
public class PluginLoader {
    private static final AtomicInteger DOWNLOAD_THREAD_ID = new AtomicInteger();
    private static final ExecutorService DOWNLOAD_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "LiteLogin Loader #" + DOWNLOAD_THREAD_ID.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<Library, String> LIBRARY_DIGEST_MAP;
    private static final Set<Library> LIBRARIES;
    static final List<String> REPOSITORIES;

    // Read dependency metadata here
    static {
        try (InputStream resourceAsStream = PluginLoader.class.getClassLoader().getResourceAsStream(".digests");
             InputStreamReader isr = new InputStreamReader(resourceAsStream);
             LineNumberReader lnr = new LineNumberReader(isr)
        ) {
            Map<Library, String> tMap = new HashMap<>();
            lnr.lines().filter(s -> !s.trim().isEmpty() && s.charAt(0) != '#').map(s -> s.split("="))
                    .forEach(ss -> tMap.put(Library.of(ss[0], ":"), ss[1]));
            LIBRARY_DIGEST_MAP = Collections.unmodifiableMap(tMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (InputStream resourceAsStream = PluginLoader.class.getClassLoader().getResourceAsStream("libraries");
             InputStreamReader isr = new InputStreamReader(resourceAsStream);
             LineNumberReader lnr = new LineNumberReader(isr)
        ) {
            LIBRARIES = lnr.lines().filter(s -> !s.trim().isEmpty() && s.charAt(0) != '#')
                    .map(ss -> Library.of(ss, "\\s+")).collect(Collectors.toUnmodifiableSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (InputStream resourceAsStream = PluginLoader.class.getClassLoader().getResourceAsStream("repositories");
             InputStreamReader isr = new InputStreamReader(resourceAsStream);
             LineNumberReader lnr = new LineNumberReader(isr)
        ) {
            LinkedList<String> tList = new LinkedList<>();
            lnr.lines().filter(s -> !s.trim().isEmpty() && s.charAt(0) != '#')
                    .map(s -> s.endsWith("/") ? s : s + '/')
                    .forEach(tList::add);

            REPOSITORIES = Collections.unmodifiableList(tList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Verify file completeness
        for (Library library : LIBRARIES) {
            if (!LIBRARY_DIGEST_MAP.containsKey(library)) {
                throw new RuntimeException("Missing digest for file " + library.getFileName() + ".");
            }
        }
    }

    private final File librariesFolder;
    private final IPlugin plugin;
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    @Getter
    private PriorAllURLClassLoader pluginClassLoader = new PriorAllURLClassLoader(new URL[0], PluginLoader.class.getClassLoader(),
            Stream.of("org.eu.pnxlr.git.litelogin.", "java.", "net.minecraft.", "com.mojang.").collect(Collectors.toSet()));
    @Getter
    private CoreAPI coreObject;

    public PluginLoader(IPlugin plugin) {
        this.plugin = plugin;
        this.librariesFolder = new File(plugin.getDataFolder(), "libraries");
    }

    /**
     * Starts loading.
     */
    public synchronized void load(String... additions) throws Exception {
        if (loaded.getAndSet(true)) {
            throw new UnsupportedOperationException("Repeated call.");
        }
        IOUtil.removeAllFiles(plugin.getTempFolder());
        generateFolder();

        List<Library> needDownload = new ArrayList<>();

        for (Library library : LIBRARIES) {
            File file = new File(librariesFolder, library.getFileName());
            if (file.exists() && file.length() != 0) {
                final String sha256 = getSha256(file);
                LoggerProvider.getLogger().debug(
                        String.format("The digest value of calculation file %s is %s.", file.getName(), sha256)
                );
                if (sha256.equals(LIBRARY_DIGEST_MAP.get(library))) {
                    pluginClassLoader.addURL(file.toURI().toURL());
                    continue;
                }
                LoggerProvider.getLogger().warn(
                        String.format("Failed to validate digest value of file %s, it will be re-downloaded.", file.getAbsolutePath())
                );
            }
            needDownload.add(library);
        }

        // Download missing files
        if (needDownload.size() != 0) {
            LoggerProvider.getLogger().info(
                    String.format("Downloading %d missing files...", needDownload.size())
            );
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Library library : needDownload) {
                LibraryDownloadTask task = new LibraryDownloadTask(library, librariesFolder, plugin.getTempFolder());
                futures.add(DOWNLOAD_EXECUTOR.submit(task::run));
            }
            for (Future<Boolean> future : futures) {
                try {
                    if (!future.get()) {
                        throw new RuntimeException("Failed to download the missing file.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        for (Library library : needDownload) {
            File file = new File(librariesFolder, library.getFileName());

            final String sha256 = getSha256(file);
            LoggerProvider.getLogger().debug(
                    String.format("The digest value of calculation file %s is %s.", file.getName(), sha256)
            );
            if (sha256.equals(LIBRARY_DIGEST_MAP.get(library))) {
                pluginClassLoader.addURL(file.toURI().toURL());
                continue;
            }
            throw new RuntimeException(
                    String.format("Failed to validate the digest value of the file %s that was just downloaded.", file.getAbsolutePath())
            );
        }


        // Extract nested JAR
        loadNestJar(LiteLoginConstants.CORE_NESTED_JAR_RESOURCE, pluginClassLoader);


        for (String addition : additions) {
            loadNestJar(addition, pluginClassLoader);
        }

        loadCore();
    }

    private void loadNestJar(String nestJarName, PriorAllURLClassLoader classLoader) throws IOException {
        final File output = File.createTempFile(nestJarName + ".", ".jar", plugin.getTempFolder());
        if (!output.exists()) {
            Files.createFile(output.toPath());
        }
        output.deleteOnExit();
        try (InputStream is = PluginLoader.class.getClassLoader().getResourceAsStream(nestJarName);
             FileOutputStream fos = new FileOutputStream(output);
        ) {
            IOUtil.copy(Objects.requireNonNull(is, nestJarName), fos);
        }
        classLoader.addURL(output.toURI().toURL());
    }

    private void loadCore() throws Exception {
        Class<?> coreClass = findClass(LiteLoginConstants.CORE_CLASS_NAME);
        for (Constructor<?> constructor : coreClass.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0] == IPlugin.class) {
                coreObject = (CoreAPI) constructor.newInstance(plugin);
                return;
            }
        }
        throw new RuntimeException("Not found constructor");
    }


    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, pluginClassLoader);
    }

    /**
     * Closes the loader.
     */
    public synchronized void close() throws Exception {
        if (pluginClassLoader != null) pluginClassLoader.close();
        plugin.getRunServer().getScheduler().shutdown();
        coreObject = null;
        pluginClassLoader = null;
        IOUtil.removeAllFiles(plugin.getTempFolder());
    }

    /**
     * Creates the dependency and temporary directories.
     */
    private void generateFolder() throws IOException {
        if (!librariesFolder.exists() && !librariesFolder.mkdirs()) {
            throw new IOException(String.format("Unable to create folder: %s", librariesFolder.getAbsolutePath()));
        }
        if (!plugin.getTempFolder().exists() && !plugin.getTempFolder().mkdirs()) {
            throw new IOException(String.format("Unable to create folder: %s", plugin.getTempFolder().getAbsolutePath()));
        }
    }

    // Returns the file SHA-256
    private String getSha256(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
            byte[] buff = new byte[1024];
            int n;
            while ((n = fis.read(buff)) > 0) {
                baos.write(buff, 0, n);
            }
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(baos.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte aByte : digest) {
                String temp = Integer.toHexString((aByte & 0xFF));
                if (temp.length() == 1) {
                    sb.append("0");
                }
                sb.append(temp);
            }
            return sb.toString();
        }
    }
}
