package org.eu.pnxlr.git.litelogin.api.data;

import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.service.IService;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the login data of a player authenticated through LiteLogin.
 */
@ApiStatus.NonExtendable
public interface LiteLoginPlayerData {

    /**
     * Returns this player's online game profile.
     * @return this player's online game profile
     */
    @NotNull GameProfile getOnlineProfile();

    /**
     * Returns the authentication service used by this player.
     * @return the authentication service used by this player
     */
    @NotNull IService getLoginService();
}
