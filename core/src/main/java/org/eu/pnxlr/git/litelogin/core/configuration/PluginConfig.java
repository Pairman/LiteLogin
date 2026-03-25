package org.eu.pnxlr.git.litelogin.core.configuration;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.logger.bridges.DebugLoggerBridge;
import org.eu.pnxlr.git.litelogin.api.internal.util.IOUtil;
import org.eu.pnxlr.git.litelogin.api.service.ServiceType;
import org.eu.pnxlr.git.litelogin.core.configuration.service.BaseServiceConfig;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.BlessingSkinYggdrasilServiceConfig;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.OfficialYggdrasilServiceConfig;
import org.eu.pnxlr.git.litelogin.core.main.Core;
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
 * 表示插件配置处理程序
 */
public class PluginConfig {
    private final File dataFolder;
    private static final Map<ServiceType, String> onlyOneServiceInfoMap = Map.of(
            ServiceType.OFFICIAL, "official");
    @Getter
    private boolean forceUseLogin;
    @Getter
    private boolean nameCorrect;
    @Getter
    private boolean autoNameChange;
    @Getter
    private SqlConfig sqlConfig;
    @Getter
    private MapperConfig mapperConfig;
    @Getter
    private String nameAllowedRegular;
    private final Core core;
    @Getter
    private boolean welcomeMsg;
    @Getter
    private Map<Integer, BaseServiceConfig> serviceIdMap = new HashMap<>();
    @Getter
    private long confirmCommandValidTimeMills;
    @Getter
    private long linkAcceptValidTimeMills;

    public PluginConfig(File dataFolder, Core core) {
        this.dataFolder = dataFolder;
        this.core = core;
    }

    public void reload() throws IOException, URISyntaxException {
        File servicesFolder = new File(dataFolder, "services");
        if (!dataFolder.exists()) {
            Files.createDirectory(dataFolder.toPath());
        }
        if (!servicesFolder.exists()) {
            Files.createDirectory(servicesFolder.toPath());
        }

        IOUtil.removeAllFiles(new File(dataFolder, "examples"));
        saveResource("config.yml", false);
        saveResource("mapper.yml", false);
        saveResourceDir("examples", true);
        if (mapperConfig != null)
            mapperConfig.save();
        mapperConfig = new MapperConfig(dataFolder);
        mapperConfig.reload();

        CommentedConfigurationNode configConfigurationNode =
                YamlConfigurationLoader.builder().file(new File(dataFolder, "config.yml")).build().load();

        if (configConfigurationNode.node("debug").getBoolean(false)) {
            DebugLoggerBridge.startDebugMode();
        } else {
            DebugLoggerBridge.cancelDebugMode();
        }

        forceUseLogin = configConfigurationNode.node("forceUseLogin").getBoolean(true);
        sqlConfig = SqlConfig.read(configConfigurationNode.node("sql"));
        nameAllowedRegular = configConfigurationNode.node("nameAllowedRegular").getString("^[0-9a-zA-Z_]{3,16}$");
        welcomeMsg = configConfigurationNode.node("welcomeMsg").getBoolean(true);
        nameCorrect = configConfigurationNode.node("nameCorrect").getBoolean(true);
        autoNameChange = configConfigurationNode.node("autoNameChange").getBoolean(true);
        confirmCommandValidTimeMills = configConfigurationNode.node("confirmCommandValidTimeMills").getLong(15000);
        linkAcceptValidTimeMills = configConfigurationNode.node("linkAcceptValidTimeMills").getLong(30000);

        Map<Integer, BaseServiceConfig> idMap = new HashMap<>();
        try (Stream<Path> list = Files.list(servicesFolder.toPath())) {
            List<BaseServiceConfig> tmp = new ArrayList<>();
            list.forEach(path -> {
                if (!path.toFile().getName().toLowerCase().endsWith(".yml")) return;
                try {
                    tmp.add(readServiceConfig(YamlConfigurationLoader.builder().path(path).build().load()));
                } catch (Exception e) {
                    LoggerProvider.getLogger().error(new ConfException("Unable to read authentication service config under file " + path, e));
                }
            });

            Set<ServiceType> notRepeat = new HashSet<>();
            for (BaseServiceConfig config : tmp) {
                if (onlyOneServiceInfoMap.containsKey(config.getServiceType())) {
                    if (!notRepeat.add(config.getServiceType())) {
                        throw new ConfException(
                                String.format("Duplicates are not allowed for authentication services of type %s, but more than one was configured.",
                                        onlyOneServiceInfoMap.get(config.getServiceType())));
                    }
                }
            }

            for (BaseServiceConfig config : tmp) {
                if (idMap.containsKey(config.getId())) {
                    throw new ConfException(String.format("The same authentication service id value %d was configured more than once.", config.getId()));
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

    private BaseServiceConfig readServiceConfig(CommentedConfigurationNode load) throws SerializationException, ConfException {
        CommentedConfigurationNode nodeId = load.node("id");
        if (nodeId.empty()) {
            throw new ConfException("service id is ");
        }
        int id = nodeId.getInt();
        String name = load.node("name").getString("Unnamed");
        ServiceType serviceType = load.node("serviceType").get(ServiceType.class);

        if (serviceType == null) {
            throw new ConfException("service type is ");
        }

        BaseServiceConfig.InitUUID initUUID = load.node("initUUID").get(BaseServiceConfig.InitUUID.class, BaseServiceConfig.InitUUID.DEFAULT);
        boolean whitelist = load.node("whitelist").getBoolean(false);
        SkinRestorerConfig skinRestorer = SkinRestorerConfig.read(load.node("skinRestorer"));

        String initNameFormat = load.node("initNameFormat").getString("{name}");
        CommentedConfigurationNode yggdrasilAuthNode = load.node("yggdrasilAuth");
        boolean trackIp = yggdrasilAuthNode.node("trackIp").getBoolean(false);
        int timeout = yggdrasilAuthNode.node("timeout").getInt(10000);
        int retry = yggdrasilAuthNode.node("retry").getInt(0);
        long retryDelay = yggdrasilAuthNode.node("retryDelay").getLong(0L);
        ProxyConfig authProxy = ProxyConfig.read(yggdrasilAuthNode.node("authProxy"));

        if (serviceType == ServiceType.OFFICIAL) {
            String customSessionServer = yggdrasilAuthNode.node("official").node("sessionServer").getString("https://sessionserver.mojang.com");
            return new OfficialYggdrasilServiceConfig(id, name,
                    initUUID, initNameFormat, whitelist,
                    skinRestorer, trackIp, timeout, retry, retryDelay, authProxy, customSessionServer);
        }

        if (serviceType == ServiceType.BLESSING_SKIN) {
            return new BlessingSkinYggdrasilServiceConfig(id, name,
                    initUUID, initNameFormat, whitelist,
                    skinRestorer, trackIp, timeout, retry, retryDelay, authProxy,
                    yggdrasilAuthNode.node("blessingSkin").node("apiRoot").getString());
        }

        throw new ConfException("Unsupported service type " + serviceType.name() + ". LiteLogin only supports OFFICIAL and ");
    }

    public void saveResource(String path, boolean cover) throws IOException {
        saveResource(cover, dataFolder, path, path);
    }

    public void saveResourceDir(String path, boolean cover) throws IOException, URISyntaxException {
        File file = new File(dataFolder, path);
        if (!file.exists()) Files.createDirectory(file.toPath());
        try (JarFile jarFile = new JarFile(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))) {
            List<JarEntry> jarFiles = jarFile.stream().filter(jarEntry -> jarEntry.getRealName().startsWith(path)).filter(jarEntry -> !jarEntry.getRealName().equals(path + "/")).collect(Collectors.toList());
            for (JarEntry je : jarFiles) {
//                if (je.isDirectory()) {
//                暂时不考虑目录下目录情况
//                    预留
//                } else {
//                下属文件
                String realName = je.getRealName();
                String fileName = realName.substring(path.length());
                saveResource(cover, file, realName, fileName);
//
            }
        }
    }

    private void saveResource(boolean cover, File file, String realName, String fileName) throws IOException {
        File subFile = new File(file, fileName);
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
}
