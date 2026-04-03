package org.eu.pnxlr.git.litelogin.core.configuration;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.api.service.ServiceType;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.BlessingSkinYggdrasilServiceConfig;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.OfficialYggdrasilServiceConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents the plugin configuration handler.
 */
public class PluginConfig {
    private static final String SERVICES_DIRECTORY_NAME = "services";
    private static final String TEMPLATES_DIRECTORY_NAME = "templates";
    private static final String MAPPER_RESOURCE_PATH = "mapper.yml";
    private static final List<String> TEMPLATE_RESOURCE_PATHS = List.of(
            "templates/template.yml",
            "templates/official.yml",
            "templates/littleskin.yml"
    );

    private final File dataFolder;
    private static final Map<ServiceType, String> ONLY_ONE_SERVICE_INFO_MAP = Map.of(
            ServiceType.OFFICIAL, "official");
    private MapperConfig mapperConfig;
    @Getter
    private Map<Integer, BaseServiceConfig> serviceIdMap = new HashMap<>();

    public PluginConfig(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void load() throws IOException {
        File servicesFolder = new File(dataFolder, SERVICES_DIRECTORY_NAME);
        ensureDirectories(servicesFolder);
        saveDefaultResources();

        if (mapperConfig == null) {
            mapperConfig = new MapperConfig(new File(dataFolder, MAPPER_RESOURCE_PATH));
        }
        mapperConfig.load();
        // manual debugging toggle
        // org.eu.pnxlr.git.litelogin.api.internal.logger.bridges.DebugLoggerBridge.startDebugMode();
        // org.eu.pnxlr.git.litelogin.api.internal.logger.bridges.DebugLoggerBridge.cancelDebugMode();

        Map<Integer, BaseServiceConfig> idMap = new HashMap<>();
        try (Stream<Path> list = Files.list(servicesFolder.toPath())) {
            List<BaseServiceConfig> tmp = new ArrayList<>();
            list.forEach(path -> {
                if (!path.toFile().getName().toLowerCase().endsWith(".yml")) return;
                try {
                    tmp.add(readServiceConfig(YamlConfigHelper.loadMap(path)));
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

    private void ensureDirectories(File servicesFolder) throws IOException {
        Files.createDirectories(dataFolder.toPath());
        Files.createDirectories(servicesFolder.toPath());
        Files.createDirectories(new File(dataFolder, TEMPLATES_DIRECTORY_NAME).toPath());
    }

    private void saveDefaultResources() throws IOException {
        saveBundledResource(MAPPER_RESOURCE_PATH, MAPPER_RESOURCE_PATH);
        for (String templateResourcePath : TEMPLATE_RESOURCE_PATHS) {
            saveBundledResource(templateResourcePath, templateResourcePath);
        }
    }

    private BaseServiceConfig readServiceConfig(Map<String, Object> load) throws IOException {
        int id = YamlConfigHelper.getInt(load, "id");
        String name = YamlConfigHelper.getString(load, "name", "Unnamed");
        boolean whitelist = YamlConfigHelper.getBoolean(load, "whitelist", false);
        SkinRestorerConfig skinRestorer = SkinRestorerConfig.fromBoolean(YamlConfigHelper.getBoolean(load, "skinRestorer", false));
        boolean trackIp = YamlConfigHelper.getBoolean(load, "trackIp", false);
        String officialSessionServer = YamlConfigHelper.getString(load, "officialSessionServer", "");
        String blessingSkinApiRoot = YamlConfigHelper.getString(load, "blessingSkinApiRoot", "");

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

    private void saveBundledResource(String resourcePath, String targetPath) throws IOException {
        YamlConfigHelper.copyResourceIfMissing(getClass(), resourcePath, new File(dataFolder, targetPath).toPath());
    }

    public Map<Integer, Integer> getPacketMapping() {
        return mapperConfig.getPacketMapping();
    }

    public boolean persistPacketMapping(int protocol, int packetId) {
        return mapperConfig.persistPacketMapping(protocol, packetId);
    }
}
