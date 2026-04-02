package org.eu.pnxlr.git.litelogin.core.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.LiteLoginAPI;
import org.eu.pnxlr.git.litelogin.api.LiteLoginAPIProvider;
import org.eu.pnxlr.git.litelogin.api.data.LiteLoginPlayerData;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.profile.Property;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.main.CoreAPI;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlugin;
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

import java.io.IOException;
import java.net.URISyntaxException;
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
public class Core implements CoreAPI, LiteLoginAPI {
    @Getter
    private final IPlugin plugin;
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
    public Core(IPlugin plugin) {
        this.plugin = plugin;
        this.pluginConfig = new PluginConfig(plugin.getDataFolder());
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
    @Override
    public void load() throws IOException, SQLException, ClassNotFoundException, URISyntaxException {
        LiteLoginAPIProvider.setApi(this);

        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream("/build.properties"));

        checkEnvironment();
        pluginConfig.reload();
        sqlManager.init();
        commandHandler.init();
        playerHandler.register();
        LoggerProvider.getLogger().info(
                String.format("Loaded LiteLogin v%s %s on %s - %s",
                        properties.getProperty("version"), properties.getProperty("build_type"),
                        plugin.getRunServer().getName(), plugin.getRunServer().getVersion()));
    }

    private void checkEnvironment() {
        if (!plugin.getRunServer().isOnlineMode()) {
            LoggerProvider.getLogger().error("Please enable online mode, otherwise the plugin will not work!!!");
            LoggerProvider.getLogger().error("Server is closing!!!");
            throw new RuntimeException("offline ");
        }
        if (!plugin.getRunServer().isForwarded()) {
            LoggerProvider.getLogger().error("Please enable forwarding, otherwise the plugin will not work!!!");
            LoggerProvider.getLogger().error("Server is closing!!!");
            throw new RuntimeException("do not forward.");
        }
    }

    public void reload() throws IOException, URISyntaxException {
        pluginConfig.reload();
    }

    /**
     * Shuts down the LiteLogin core.
     */
    @Override
    public void close() {
        sqlManager.close();
    }

    @Override
    public Map<Integer, Integer> getPacketMapping() {
        return pluginConfig.getPacketMapping();
    }

    @Override
    public boolean persistPacketMapping(int protocol, int packetId) {
        return pluginConfig.persistPacketMapping(protocol, packetId);
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
}
