package org.eu.pnxlr.git.litelogin.velocity.impl;

import com.velocitypowered.api.proxy.Player;
import org.eu.pnxlr.git.litelogin.velocity.main.LiteLoginVelocity;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlayer;
import org.eu.pnxlr.git.litelogin.api.internal.util.reflect.ReflectUtil;
import net.kyori.adventure.text.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.UUID;

/**
 * Velocity 玩家对象
 */
public class VelocityPlayer extends VelocitySender implements IPlayer {
    private final Player player;

    public VelocityPlayer(Player player) {
        super(player);
        this.player = player;
    }

    @Override
    public void kickPlayer(String message) {
        player.disconnect(Component.text(message));
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public SocketAddress getAddress() {
        return player.getRemoteAddress();
    }

    @Override
    public boolean isOnline() {
        return LiteLoginVelocity.getInstance().getRunServer().getPlayerManager().getPlayer(player.getUniqueId()) != null;
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VelocityPlayer that = (VelocityPlayer) o;
        return Objects.equals(player.getUniqueId(), that.player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(player.getUniqueId());
    }
}
