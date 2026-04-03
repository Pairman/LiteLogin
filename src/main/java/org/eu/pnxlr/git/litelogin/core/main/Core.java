package org.eu.pnxlr.git.litelogin.core.main;

import com.velocitypowered.api.proxy.ProxyServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.LiteLoginAPI;
import org.eu.pnxlr.git.litelogin.api.LiteLoginAPIProvider;
import org.eu.pnxlr.git.litelogin.api.data.LiteLoginPlayerData;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.profile.Property;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.AsyncScheduler;
import org.eu.pnxlr.git.litelogin.core.auth.AuthHandler;
import org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil.serialize.GameProfileSerializer;
import org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil.serialize.PropertySerializer;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.configuration.PluginConfig;
import org.eu.pnxlr.git.litelogin.core.configuration.BaseServiceConfig;
import org.eu.pnxlr.git.litelogin.core.database.SQLManager;
import org.eu.pnxlr.git.litelogin.core.handle.PlayerHandler;
import org.eu.pnxlr.git.litelogin.core.skinrestorer.SkinRestorerCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LiteLogin core.
 */
public class Core implements LiteLoginAPI {
    @Getter
    private final File dataFolder;
    @Getter
    private final ProxyServer proxyServer;
    private final boolean forwarded;
    @Getter
    private final AsyncScheduler scheduler;
    @Getter
    private final SQLManager sqlManager;
    @Getter
    private final PluginConfig pluginConfig;
    @Getter
    private final AuthHandler authHandler;
    @Getter
    private final SkinRestorerCore skinRestorerHandler;
    @Getter
    private final CommandHandler commandHandler;
    @Getter
    private final PlayerHandler playerHandler;
    @Getter
    private final Set<String> cachedWhitelist = Collections.newSetFromMap(new ConcurrentHashMap<>());
    @Getter
    private final Gson gson;

    /**
     * Constructs the LiteLogin core. This method will be invoked by reflection.
     */
    public Core(File dataFolder, ProxyServer proxyServer, boolean forwarded) {
        this.dataFolder = dataFolder;
        this.proxyServer = proxyServer;
        this.forwarded = forwarded;
        this.scheduler = new AsyncScheduler();
        this.pluginConfig = new PluginConfig(dataFolder);
        this.sqlManager = new SQLManager(this);
        this.authHandler = new AuthHandler(this);
        this.skinRestorerHandler = new SkinRestorerCore(this);
        this.commandHandler = new CommandHandler(this);
        this.playerHandler = new PlayerHandler(this);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(GameProfile.class, new GameProfileSerializer())
                .registerTypeAdapter(Property.class, new PropertySerializer()).create();
    }

    /**
     * Loads the LiteLogin core.
     */
    public void load() throws java.io.IOException, SQLException, ClassNotFoundException {
        LiteLoginAPIProvider.setApi(this);

        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream("/build.properties"));

        checkEnvironment();
        pluginConfig.load();
        sqlManager.init();
        commandHandler.init();
        playerHandler.register();
        LoggerProvider.getLogger().info(
                String.format("Loaded LiteLogin v%s %s on %s - %s",
                        properties.getProperty("version"), properties.getProperty("build_type"),
                        proxyServer.getVersion().getName(), proxyServer.getVersion().getVersion()));
    }

    private void checkEnvironment() {
        if (!proxyServer.getConfiguration().isOnlineMode()) {
            LoggerProvider.getLogger().error("Please enable online mode, otherwise the plugin will not work!!!");
            LoggerProvider.getLogger().error("Server is closing!!!");
            throw new RuntimeException("offline ");
        }
        if (!forwarded) {
            LoggerProvider.getLogger().error("Please enable forwarding, otherwise the plugin will not work!!!");
            LoggerProvider.getLogger().error("Server is closing!!!");
            throw new RuntimeException("do not forward.");
        }
    }

    /**
     * Shuts down the LiteLogin core.
     */
    public void close() {
        scheduler.shutdown();
        sqlManager.close();
    }

    @NotNull
    @Override
    public Collection<BaseServiceConfig> getServices() {
        return Collections.unmodifiableCollection(pluginConfig.getServiceIdMap().values());
    }

    @Nullable
    @Override
    public LiteLoginPlayerData getPlayerData(@NotNull UUID inGameUUID) {
        return playerHandler.getPlayerData(inGameUUID);
    }

    public Map<Integer, Integer> getPacketMapping() {
        return pluginConfig.getPacketMapping();
    }

    public boolean persistPacketMapping(int protocol, int packetId) {
        return pluginConfig.persistPacketMapping(protocol, packetId);
    }
}
