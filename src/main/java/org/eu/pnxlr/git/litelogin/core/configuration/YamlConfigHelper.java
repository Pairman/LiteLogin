package org.eu.pnxlr.git.litelogin.core.configuration;

import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class YamlConfigHelper {
    private static final DumperOptions DUMPER_OPTIONS = createDumperOptions();
    private static final Yaml YAML = new Yaml(
            new SafeConstructor(new LoaderOptions()),
            new Representer(DUMPER_OPTIONS),
            DUMPER_OPTIONS
    );

    private YamlConfigHelper() {
    }

    static Map<String, Object> loadMap(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new LinkedHashMap<>();
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            Object raw = YAML.load(inputStream);
            if (raw == null) {
                return new LinkedHashMap<>();
            }
            if (!(raw instanceof Map<?, ?> map)) {
                throw new IOException("Expected YAML object in " + path);
            }
            return toStringMap(map);
        }
    }

    static void saveMap(Path path, Map<String, Object> content) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            YAML.dump(content, writer);
        }
    }

    static void copyResourceIfMissing(Class<?> owner, String resourcePath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            return;
        }
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream inputStream = owner.getResourceAsStream("/" + resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing bundled resource " + resourcePath);
            }
            Files.copy(inputStream, targetPath);
        }
        LoggerProvider.getLogger().info("Extract: " + resourcePath);
    }

    static String getString(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    static boolean getBoolean(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    static int getInt(Map<String, Object> map, String key) throws IOException {
        Object value = map.get(key);
        if (value == null) {
            throw new IOException("Missing required config key: " + key);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid integer for config key: " + key, exception);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map<?, ?> child) {
            return toStringMap(child);
        }
        return new LinkedHashMap<>();
    }

    private static LinkedHashMap<String, Object> toStringMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> childMap) {
                result.put(String.valueOf(entry.getKey()), toStringMap(childMap));
            } else {
                result.put(String.valueOf(entry.getKey()), value);
            }
        }
        return result;
    }

    private static DumperOptions createDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(1);
        return options;
    }
}
