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
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import io.netty.channel.Channel;
import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.core.main.Core;
import org.eu.pnxlr.git.litelogin.velocity.injector.ChatSessionHandler;
import org.eu.pnxlr.git.litelogin.velocity.injector.NewChatSessionPacketIdEvent;
import org.eu.pnxlr.git.litelogin.velocity.injector.VelocityInjector;
import org.slf4j.Logger;

import java.nio.file.Path;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;

/**
 * Velocity Main
 */
@Plugin(
        id = "litelogin",
        name = "LiteLogin",
        version = BuildConstants.VERSION,
        authors = {
                "CaaMoe",
                "Becods",
                "ksqeib445",
                "heartalborada-del",
                "actions-user",
                "half-nothing",
                "4o3F",
                "NaturalSelect",
                "Pairman",
                "ianchb",
                "IceBlues",
                "Lemon-miaow",
                "LYOfficial",
                "Mashirl",
                "MySoulcutting",
                "sdgedghdg"
        }
)
public class LiteLoginVelocity {
    @Getter
    private static LiteLoginVelocity instance;
    private final Path dataDirectory;
    @Getter
    private final com.velocitypowered.proxy.VelocityServer server;
    @Getter
    private Core core;
    private final VelocityInjector injector;

    @Inject
    public LiteLoginVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        instance = this;
        this.server = (com.velocitypowered.proxy.VelocityServer) server;
        this.dataDirectory = dataDirectory;
        this.injector = new VelocityInjector();
        LoggerProvider.setLogger(new Slf4jLoggerBridge(logger));
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        try {
            core = new Core(
                    dataDirectory.toFile(),
                    server,
                    ((VelocityConfiguration) server.getConfiguration()).getPlayerInfoForwardingMode() != PlayerInfoForwarding.NONE
            );
            core.load();
            injector.inject(core);
            injector.registerChatSession(core.getPacketMapping());
        } catch (Throwable e) {
            LoggerProvider.getLogger().error("An exception was encountered while loading LiteLogin.", e);
            server.shutdown();
            return;
        }
        new GlobalListener(this).register();
        new CommandHandler(this).register();
        // Automatically detect unmapped ChatSession packets. This may affect performance.
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
            server.getEventManager().register(this, NewChatSessionPacketIdEvent.class,
                    (AwaitingEventExecutor<NewChatSessionPacketIdEvent>) packetEvent -> EventTask.withContinuation(continuation -> {
                        try {
                            if (core.persistPacketMapping(packetEvent.getVersion().getProtocol(), packetEvent.getPacketId())) {
                                LoggerProvider.getLogger().info(String.format(
                                        "Discovered a ChatSession packet mapping for protocol %d -> 0x%02X. Saved it to mapper.yml; restart the proxy to apply it.",
                                        packetEvent.getVersion().getProtocol(),
                                        packetEvent.getPacketId()
                                ));
                            }
                        } finally {
                            continuation.resume();
                        }
                    })
            );
        }

    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        try {
            if (core != null) {
                core.close();
            }
        } catch (Exception e) {
            LoggerProvider.getLogger().error("An exception was encountered while closing LiteLogin.", e);
        } finally {
            core = null;
            server.shutdown();
        }
    }

    private void injectPlayer(final Player player) {
        final ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
        connectedPlayer.getConnection()
                .getChannel()
                .pipeline()
                .addBefore(Connections.HANDLER, LiteLoginConstants.CHAT_SESSION_PIPELINE_KEY, new ChatSessionHandler(player,server.getEventManager()));
    }

    private void removePlayer(final Player player) {
        final ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
        final Channel channel = connectedPlayer.getConnection().getChannel();
        channel.eventLoop().submit(() -> {
            if (channel.pipeline().context(LiteLoginConstants.CHAT_SESSION_PIPELINE_KEY) != null) {
                channel.pipeline().remove(LiteLoginConstants.CHAT_SESSION_PIPELINE_KEY);
            }
        });
    }
}
