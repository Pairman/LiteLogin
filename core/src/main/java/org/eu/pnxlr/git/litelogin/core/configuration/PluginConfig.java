package org.eu.pnxlr.git.litelogin.core.configuration;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
// import org.eu.pnxlr.git.litelogin.api.internal.logger.bridges.DebugLoggerBridge;
import org.eu.pnxlr.git.litelogin.api.internal.util.IOUtil;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.api.service.ServiceType;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.BlessingSkinYggdrasilServiceConfig;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.OfficialYggdrasilServiceConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the plugin configuration handler.
 */
public class PluginConfig {
    private final File dataFolder;
    private static final Map<ServiceType, String> ONLY_ONE_SERVICE_INFO_MAP = Map.of(
            ServiceType.OFFICIAL, "official");
    private MapperConfig mapperConfig;
    @Getter
    private Map<Integer, BaseServiceConfig> serviceIdMap = new HashMap<>();

    public PluginConfig(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void reload() throws IOException, URISyntaxException {
        File servicesFolder = new File(dataFolder, "services");
        if (!dataFolder.exists()) {
            Files.createDirectory(dataFolder.toPath());
        }
        if (!servicesFolder.exists()) {
            Files.createDirectory(servicesFolder.toPath());
        }

        saveResource("mapper.yml", false);
        saveResource("templates/template.yml", false);
        saveResource("templates/official.yml", false);
        saveResource("templates/littleskin.yml", false);

        if (mapperConfig == null) {
            mapperConfig = new MapperConfig(dataFolder);
        }
        mapperConfig.reload();
        // manual debugging toggle
        // DebugLoggerBridge.startDebugMode();
        // DebugLoggerBridge.cancelDebugMode();

        Map<Integer, BaseServiceConfig> idMap = new HashMap<>();
        try (Stream<Path> list = Files.list(servicesFolder.toPath())) {
            List<BaseServiceConfig> tmp = new ArrayList<>();
            list.forEach(path -> {
                if (!path.toFile().getName().toLowerCase().endsWith(".yml")) return;
                try {
                    tmp.add(readServiceConfig(YamlConfigurationLoader.builder().path(path).build().load()));
                } catch (Exception e) {
                    LoggerProvider.getLogger().error(new IOException("Unable to read authentication service config under file " + path, e));
                }
            });

            Set<ServiceType> notRepeat = new HashSet<>();
            for (BaseServiceConfig config : tmp) {
                if (ONLY_ONE_SERVICE_INFO_MAP.containsKey(config.getServiceType())) {
                    if (!notRepeat.add(config.getServiceType())) {
                        throw new IOException(
                                String.format("Duplicates are not allowed for authentication services of type %s, but more than one was configured.",
                                        ONLY_ONE_SERVICE_INFO_MAP.get(config.getServiceType())));
                    }
                }
            }

            for (BaseServiceConfig config : tmp) {
                if (idMap.containsKey(config.getId())) {
                    throw new IOException(String.format("The same authentication service id value %d was configured more than once.", config.getId()));
                }
                idMap.put(config.getId(), config);
            }
        }

        idMap.forEach((i, y) -> {
            if ((y.getName()).equalsIgnoreCase("unnamed")) {
                LoggerProvider.getLogger().warn(String.format("The name of authentication service whose id is %d has not been set.", i));
            }
            LoggerProvider.getLogger().info(String.format(
                    "Added an authentication service with id %d and name %s.", i, y.getName()
            ));
        });

        if (idMap.size() == 0) LoggerProvider.getLogger().warn(
                "The server has not added any authentication service, which will prevent all players from logging in."
        );
        else LoggerProvider.getLogger().info(String.format(
                "Added %d authentication services.", idMap.size()
        ));
        this.serviceIdMap = Collections.unmodifiableMap(idMap);


    }

    private BaseServiceConfig readServiceConfig(CommentedConfigurationNode load) throws SerializationException, IOException {
        CommentedConfigurationNode nodeId = load.node("id");
        if (nodeId.empty()) {
            throw new IOException("service id is ");
        }
        int id = nodeId.getInt();
        String name = load.node("name").getString("Unnamed");
        boolean whitelist = load.node("whitelist").getBoolean(false);
        SkinRestorerConfig skinRestorer = SkinRestorerConfig.read(load.node("skinRestorer"));
        boolean trackIp = load.node("trackIp").getBoolean(false);
        String officialSessionServer = load.node("officialSessionServer").getString("");
        String blessingSkinApiRoot = load.node("blessingSkinApiRoot").getString("");

        boolean hasOfficialSessionServer = !ValueUtil.isEmpty(officialSessionServer);
        boolean hasBlessingSkinApiRoot = !ValueUtil.isEmpty(blessingSkinApiRoot);

        if (hasOfficialSessionServer == hasBlessingSkinApiRoot) {
            throw new IOException("Exactly one of officialSessionServer and blessingSkinApiRoot must be specified.");
        }

        if (hasOfficialSessionServer) {
            return new OfficialYggdrasilServiceConfig(id, name,
                    whitelist, skinRestorer, trackIp, officialSessionServer);
        }

        return new BlessingSkinYggdrasilServiceConfig(id, name,
                whitelist, skinRestorer, trackIp, blessingSkinApiRoot);
    }

    public void saveResource(String path, boolean cover) throws IOException {
        saveResource(cover, dataFolder, path, path);
    }

    private void saveResource(boolean cover, File file, String realName, String fileName) throws IOException {
        File subFile = new File(file, fileName);
        File parentFile = subFile.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            Files.createDirectories(parentFile.toPath());
        }
        boolean exists = subFile.exists();
        if (exists && !cover) {
            return;
        } else {
            if (!exists) Files.createFile(subFile.toPath());
        }
        try (InputStream is = Objects.requireNonNull(getClass().getResourceAsStream("/" + realName));
             FileOutputStream fs = new FileOutputStream(subFile)) {
            IOUtil.copy(is, fs);
        }
        if (!exists) {
            LoggerProvider.getLogger().info("Extract: " + realName);
        } else {
            LoggerProvider.getLogger().info("Cover: " + realName);
        }
    }

    public Map<Integer, Integer> getPacketMapping() {
        return mapperConfig.getPacketMapping();
    }

    public boolean persistPacketMapping(int protocol, int packetId) {
        return mapperConfig.persistPacketMapping(protocol, packetId);
    }
}
