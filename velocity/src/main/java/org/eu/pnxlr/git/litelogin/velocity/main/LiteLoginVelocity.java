package org.eu.pnxlr.git.litelogin.velocity.main;

import com.google.inject.Inject;
import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import org.eu.pnxlr.git.litelogin.velocity.impl.ChatSessionHandler;
import org.eu.pnxlr.git.litelogin.velocity.impl.NewChatSessionPacketIDEvent;
import org.eu.pnxlr.git.litelogin.velocity.impl.VelocityServer;
import org.eu.pnxlr.git.litelogin.velocity.logger.Slf4jLoggerBridge;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.injector.Injector;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.main.CoreAPI;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlugin;
import org.eu.pnxlr.git.litelogin.loader.main.PluginLoader;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

/**
 * Velocity Main
 */
public class LiteLoginVelocity implements IPlugin {
    @Getter
    private static LiteLoginVelocity instance;
    private final Path dataDirectory;
    @Getter
    private final com.velocitypowered.proxy.VelocityServer server;
    @Getter
    private final VelocityServer runServer;
    private final PluginLoader pluginLoader;
    @Getter
    private CoreAPI coreApi;
    private static final String KEY = "LiteLoginChatSession";
    private Injector injector;
    @Inject
    public LiteLoginVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        instance = this;
        this.server = (com.velocitypowered.proxy.VelocityServer) server;
        this.runServer = new VelocityServer(this.server);
        this.dataDirectory = dataDirectory;
        LoggerProvider.setLogger(new Slf4jLoggerBridge(logger));
        this.pluginLoader = new PluginLoader(this);
        try {
            pluginLoader.load("LiteLogin-Velocity-Injector.JarFile");
        } catch (Exception e) {
            LoggerProvider.getLogger().error("An exception was encountered while initializing LiteLogin.", e);
            server.shutdown();
        }
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        try {
            coreApi = pluginLoader.getCoreObject();
            coreApi.load();
            injector = (Injector) pluginLoader.findClass("org.eu.pnxlr.git.litelogin.velocity.injector.VelocityInjector").getConstructor().newInstance();
            injector.inject(coreApi);
            injector.registerChatSession(coreApi.getMapperConfig().getPacketMapping());
        } catch (Throwable e) {
            LoggerProvider.getLogger().error("An exception was encountered while loading LiteLogin.", e);
            server.shutdown();
            return;
        }
        new GlobalListener(this).register();
        new CommandHandler(this).register("litelogin");
        //自动检测未映射的ChatSession Packet, 可能对性能有影响?
        {
            server.getEventManager().register(this, PostLoginEvent.class,
                    (AwaitingEventExecutor<PostLoginEvent>) postLoginEvent -> EventTask.withContinuation(continuation -> {
                        try {
                            if(postLoginEvent.getPlayer().getProtocolVersion().getProtocol() < 761) return;
                            injectPlayer(postLoginEvent.getPlayer());
                        } finally {
                            continuation.resume();
                        }
                    })
            );
            server.getEventManager().register(this, DisconnectEvent.class, PostOrder.LAST,
                    (AwaitingEventExecutor<DisconnectEvent>) disconnectEvent ->
                            disconnectEvent.getLoginStatus() == DisconnectEvent.LoginStatus.CONFLICTING_LOGIN
                                    ? null
                                    : EventTask.async(() -> removePlayer(disconnectEvent.getPlayer()))
            );
            server.getEventManager().register(this, NewChatSessionPacketIDEvent.class,
                    (AwaitingEventExecutor<NewChatSessionPacketIDEvent>) packetEvent -> EventTask.withContinuation(continuation -> {
                        runServer.getPlayerManager().kickPlayerIfOnline(packetEvent.getPlayer().getUniqueId(), "§cPlease reconnect to the server.");
                        coreApi.getMapperConfig().getPacketMapping().put(packetEvent.getVersion().getProtocol(),packetEvent.getPacketID());
                        coreApi.getMapperConfig().save();
                        injector.registerChatSession(coreApi.getMapperConfig().getPacketMapping());
                    })
            );
        }

    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        try {
            coreApi.close();
            pluginLoader.close();
        } catch (Exception e) {
            LoggerProvider.getLogger().error("An exception was encountered while closing LiteLogin.", e);
        } finally {
            coreApi = null;
            server.shutdown();
        }
    }


    @Override
    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    @Override
    public File getTempFolder() {
        return new File(getDataFolder(), "tmp");
    }

    private void injectPlayer(final Player player) {
        final ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
        connectedPlayer.getConnection()
                .getChannel()
                .pipeline()
                .addBefore(Connections.HANDLER, KEY, new ChatSessionHandler(player,server.getEventManager()));
    }

    private void removePlayer(final Player player) {
        final ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
        final Channel channel = connectedPlayer.getConnection().getChannel();
        channel.eventLoop().submit(() -> {
            channel.pipeline().remove(KEY);
        });
    }
}
