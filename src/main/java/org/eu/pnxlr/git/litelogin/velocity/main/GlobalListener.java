package org.eu.pnxlr.git.litelogin.velocity.main;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import org.eu.pnxlr.git.litelogin.api.internal.result.HandleResult;
import net.kyori.adventure.text.Component;

/**
 * Velocity event listener.
 */
public class GlobalListener {
    private final LiteLoginVelocity plugin;

    public GlobalListener(LiteLoginVelocity plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getEventManager().register(plugin, this);
    }

    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        HandleResult result = plugin.getCore().getPlayerHandler().pushPlayerJoinGame(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getUsername()
        );
        if (result.getType() == HandleResult.Type.KICK) {
            if (result.getKickMessage() == null || result.getKickMessage().trim().length() == 0) {
                event.getPlayer().disconnect(Component.text(""));
            } else {
                event.getPlayer().disconnect(Component.text(result.getKickMessage()));
            }
            return;
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        plugin.getCore().getPlayerHandler().pushPlayerQuitGame(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getUsername()
        );
    }
}
