package org.eu.pnxlr.git.litelogin.api.internal.plugin;

import org.jetbrains.annotations.ApiStatus;

import java.util.Set;
import java.util.UUID;

/**
 * Shared player manager abstraction.
 */
@ApiStatus.Internal
public interface IPlayerManager {

    /**
     * Returns online players by the given name.
     *
     * @param name the given name
     * @return player sender objects
     */
    Set<IPlayer> getPlayers(String name);

    /**
     * Returns the online player by the given unique identifier.
     *
     * @param uuid the given unique identifier
     * @return player sender object
     */
    IPlayer getPlayer(UUID uuid);

    /**
     * Returns the list of all currently online players.
     *
     * @return online player list
     */
    Set<IPlayer> getOnlinePlayers();

    /**
     * Kicks the player if the player exists.
     *
     * @param name    player name
     * @param message kick message
     */
    default void kickPlayerIfOnline(String name, String message) {
        for (IPlayer player : getPlayers(name)) {
            player.kickPlayer(message);
        }
    }

    default void kickAll(String message) {
        for (IPlayer player : getOnlinePlayers()) {
            player.kickPlayer(message);
        }
    }

    /**
     * Kicks the player if the player exists.
     *
     * @param uuid    player UUID
     * @param message kick message
     */
    default void kickPlayerIfOnline(UUID uuid, String message) {
        IPlayer player = getPlayer(uuid);
        if (player != null) {
            player.kickPlayer(message);
        }
    }

    /**
     * Checks whether the player is online.
     *
     * @param redirectUuid player UUID
     * @return whether the player is online
     */
    default boolean hasOnline(UUID redirectUuid) {
        return getPlayer(redirectUuid) != null;
    }
}
