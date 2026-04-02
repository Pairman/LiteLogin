package org.eu.pnxlr.git.litelogin.api.internal.plugin;

import org.jetbrains.annotations.ApiStatus;

import java.net.SocketAddress;
import java.util.UUID;

@ApiStatus.Internal
public interface IPlayer extends ISender {
    /**
     * Kicks the player for the given reason.
     *
     * @param message the reason
     */
    void kickPlayer(String message);

    /**
     * Returns this player's in-game UUID.
     *
     * @return the player's in-game UUID
     */
    UUID getUniqueId();

    /**
     * Returns this player's IP address.
     *
     * @return the player's IP address
     */
    SocketAddress getAddress();

    /**
     * Returns whether the player is still online.
     */
    boolean isOnline();
}
