package org.eu.pnxlr.git.litelogin.api.internal.handle;

import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlayer;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

/**
 * Simple communication API. The target player must be online.
 */
@ApiStatus.Internal
public interface HandlerAPI {

    /**
     * Submits a player quit event.
     *
     * @param inGameUUID the player's in-game UUID
     */
    HandleResult pushPlayerQuitGame(UUID inGameUUID, String username);

    /**
     * Submits a player join event.
     *
     * @param inGameUUID the player's in-game UUID
     */
    HandleResult pushPlayerJoinGame(UUID inGameUUID, String username);

    void callPlayerJoinGame(IPlayer player);

    /**
     * Returns the player's online game profile.
     *
     * @param inGameUUID the player's in-game UUID
     * @return a pair containing the player's online data and the Yggdrasil service ID used for authentication
     */
    Pair<GameProfile, Integer> getPlayerOnlineProfile(UUID inGameUUID);

    /**
     * Returns the player's in-game UUID.
     *
     * @param onlineUUID the player's online UUID
     * @param serviceId  service ID
     * @return the player's in-game UUID
     */
    UUID getInGameUUID(UUID onlineUUID, int serviceId);

    /**
     * Returns the service name.
     *
     * @param serviceId service id
     * @return Yggdrasil name
     */
    String getServiceName(int serviceId);
}
