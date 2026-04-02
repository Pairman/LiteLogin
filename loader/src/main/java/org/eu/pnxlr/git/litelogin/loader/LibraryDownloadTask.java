package org.eu.pnxlr.git.litelogin.loader;

import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class LibraryDownloadTask {
    private final Library library;
    private final File librariesFolder;
    private final File tempLibrariesFolder;

    public LibraryDownloadTask(Library library, File librariesFolder, File tempLibrariesFolder) {
        this.library = library;
        this.librariesFolder = librariesFolder;
        this.tempLibrariesFolder = tempLibrariesFolder;
    }

    private static byte[] getBytes(URL url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(false);
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.connect();

        if (httpURLConnection.getResponseCode() == 200) {
            try (InputStream input = httpURLConnection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                IOUtil.copy(input, output);
                return output.toByteArray();
            }
        }
        throw new IOException(String.valueOf(httpURLConnection.getResponseCode()));
    }

    public boolean run() {
        File output = new File(librariesFolder, library.getFileName());
        File tmp = new File(tempLibrariesFolder, library.getFileName());
        byte[] bytes = null;

        List<Exception> exceptions = new ArrayList<>();
        for (String repository : PluginLoader.REPOSITORIES) {
            String downloadUrl = repository + library.getDownloadUrl();
            LoggerProvider.getLogger().debug("Downloading from " + downloadUrl);
            try {
                bytes = getBytes(URI.create(downloadUrl).toURL());
                break;
            } catch (Exception t) {
                exceptions.add(new RuntimeException(String.format("Download from %s failed.", downloadUrl), t));
            }
        }

        if (bytes == null) {
            String cause = String.format("Unable to download file %s.", library.getFileName());
            exceptions.forEach(e -> LoggerProvider.getLogger().error(cause, e));
            return false;
        }

        try {
            if (!tmp.exists()) {
                Files.createFile(tmp.toPath());
            } else {
                try (FileWriter fw = new FileWriter(tmp)) {
                    fw.write("");
                    fw.flush();
                }
            }
            if (output.exists()) {
                Files.delete(output.toPath());
            }

            Files.write(tmp.toPath(), bytes);
            Files.move(tmp.toPath(), output.toPath());
            LoggerProvider.getLogger().info("Downloaded " + output.getName());
            return true;
        } catch (Throwable t) {
            LoggerProvider.getLogger().error("Unable to process file " + library.getFileName(), t);
            return false;
        }
    }
}
