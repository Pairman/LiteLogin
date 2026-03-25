package org.eu.pnxlr.git.litelogin.loader.main;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.main.CoreAPI;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlugin;
import org.eu.pnxlr.git.litelogin.api.internal.util.IOUtil;
import org.eu.pnxlr.git.litelogin.loader.classloader.IExtURLClassLoader;
import org.eu.pnxlr.git.litelogin.loader.classloader.PriorAllURLClassLoader;
import org.eu.pnxlr.git.litelogin.loader.exception.InitialFailedException;
import org.eu.pnxlr.git.litelogin.loader.library.Library;
import org.eu.pnxlr.git.litelogin.loader.task.LibraryDownloadTask;

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
 * 表示插件加载器
 */
public class PluginLoader {
    public static final String nestJarName = "LiteLogin-Core.JarFile";
    public static final String coreClassName = "org.eu.pnxlr.git.litelogin.core.main.Core";
    private static final AtomicInteger downloadThreadId = new AtomicInteger();
    private static final ExecutorService DOWNLOAD_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "LiteLogin Loader #" + downloadThreadId.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    public static final Map<Library, String> libraryDigestMap;
    public static final Set<Library> libraries;
    public static final List<String> repositories;

    // 这里读取依赖数据
    static {
        try (InputStream resourceAsStream = PluginLoader.class.getClassLoader().getResourceAsStream(".digests");
             InputStreamReader isr = new InputStreamReader(resourceAsStream);
             LineNumberReader lnr = new LineNumberReader(isr)
        ) {
            Map<Library, String> tMap = new HashMap<>();
            lnr.lines().filter(s -> !s.trim().isEmpty() && s.charAt(0) != '#').map(s -> s.split("="))
                    .forEach(ss -> tMap.put(Library.of(ss[0], ":"), ss[1]));
            libraryDigestMap = Collections.unmodifiableMap(tMap);
        } catch (Exception e) {
            throw new InitialFailedException(e);
        }

        try (InputStream resourceAsStream = PluginLoader.class.getClassLoader().getResourceAsStream("libraries");
             InputStreamReader isr = new InputStreamReader(resourceAsStream);
             LineNumberReader lnr = new LineNumberReader(isr)
        ) {
            libraries = lnr.lines().filter(s -> !s.trim().isEmpty() && s.charAt(0) != '#')
                    .map(ss -> Library.of(ss, "\\s+")).collect(Collectors.toUnmodifiableSet());
        } catch (Exception e) {
            throw new InitialFailedException(e);
        }

        try (InputStream resourceAsStream = PluginLoader.class.getClassLoader().getResourceAsStream("repositories");
             InputStreamReader isr = new InputStreamReader(resourceAsStream);
             LineNumberReader lnr = new LineNumberReader(isr)
        ) {
            LinkedList<String> tList = new LinkedList<>();
            lnr.lines().filter(s -> !s.trim().isEmpty() && s.charAt(0) != '#')
                    .map(s -> s.endsWith("/") ? s : s + '/')
                    .forEach(tList::add);

            repositories = Collections.unmodifiableList(tList);
        } catch (Exception e) {
            throw new InitialFailedException(e);
        }

        // 判断文件完整
        for (Library library : libraries) {
            if (!libraryDigestMap.containsKey(library)) {
                throw new InitialFailedException("Missing digest for file " + library.getFileName() + ".");
            }
        }
    }

    private final File librariesFolder;
    private final IPlugin plugin;
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    @Getter
    private IExtURLClassLoader pluginClassLoader = new PriorAllURLClassLoader(new URL[0], PluginLoader.class.getClassLoader(),
            Stream.of("org.eu.pnxlr.git.litelogin.", "java.", "net.minecraft.", "com.mojang.").collect(Collectors.toSet()));
    @Getter
    private CoreAPI coreObject;

    public PluginLoader(IPlugin plugin) {
        this.plugin = plugin;
        this.librariesFolder = new File(plugin.getDataFolder(), "libraries");
    }

    /**
     * 开始加载
     */
    public synchronized void load(String... additions) throws Exception {
        if (loaded.getAndSet(true)) {
            throw new UnsupportedOperationException("Repeated call.");
        }
        IOUtil.removeAllFiles(plugin.getTempFolder());
        generateFolder();

        List<Library> needDownload = new ArrayList<>();

        for (Library library : libraries) {
            File file = new File(librariesFolder, library.getFileName());
            if (file.exists() && file.length() != 0) {
                final String sha256 = getSha256(file);
                LoggerProvider.getLogger().debug(
                        String.format("The digest value of calculation file %s is %s.", file.getName(), sha256)
                );
                if (sha256.equals(libraryDigestMap.get(library))) {
                    pluginClassLoader.addURL(file.toURI().toURL());
                    continue;
                }
                LoggerProvider.getLogger().warn(
                        String.format("Failed to validate digest value of file %s, it will be re-downloaded.", file.getAbsolutePath())
                );
            }
            needDownload.add(library);
        }

        // 下载缺失文件
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
                        throw new InitialFailedException("Failed to download the missing file.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InitialFailedException(e);
                } catch (ExecutionException e) {
                    throw new InitialFailedException(e.getCause());
                }
            }
        }

        for (Library library : needDownload) {
            File file = new File(librariesFolder, library.getFileName());

            final String sha256 = getSha256(file);
            LoggerProvider.getLogger().debug(
                    String.format("The digest value of calculation file %s is %s.", file.getName(), sha256)
            );
            if (sha256.equals(libraryDigestMap.get(library))) {
                pluginClassLoader.addURL(file.toURI().toURL());
                continue;
            }
            throw new InitialFailedException(
                    String.format("Failed to validate the digest value of the file %s that was just downloaded.", file.getAbsolutePath())
            );
        }


        // 提取 nest jar
        loadNestJar(nestJarName, pluginClassLoader);


        for (String addition : additions) {
            loadNestJar(addition, pluginClassLoader);
        }

        loadCore();
    }

    private void loadNestJar(String nestJarName, IExtURLClassLoader classLoader) throws IOException {
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
        Class<?> coreClass = findClass(coreClassName);
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
        return Class.forName(name, true, pluginClassLoader.self());
    }

    /**
     * 关闭
     */
    public synchronized void close() throws Exception {
        if (pluginClassLoader != null) pluginClassLoader.self().close();
        plugin.getRunServer().getScheduler().shutdown();
        coreObject = null;
        pluginClassLoader = null;
        IOUtil.removeAllFiles(plugin.getTempFolder());
    }

    /**
     * 生成依赖和临时目录文件夹
     */
    private void generateFolder() throws IOException {
        if (!librariesFolder.exists() && !librariesFolder.mkdirs()) {
            throw new IOException(String.format("Unable to create folder: %s", librariesFolder.getAbsolutePath()));
        }
        if (!plugin.getTempFolder().exists() && !plugin.getTempFolder().mkdirs()) {
            throw new IOException(String.format("Unable to create folder: %s", plugin.getTempFolder().getAbsolutePath()));
        }
    }

    // 获得文件sha256
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
