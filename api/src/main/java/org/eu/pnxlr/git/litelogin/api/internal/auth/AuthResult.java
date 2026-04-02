package org.eu.pnxlr.git.litelogin.api.internal.auth;

import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.jetbrains.annotations.ApiStatus;

/**
 * Authentication result.
 */
@ApiStatus.Internal
public interface AuthResult {

    /**
     * Returns the final approved game profile.
     *
     * @return the final approved game profile
     */
    GameProfile getResponse();

    /**
     * Returns the kick message when authentication fails.
     *
     * @return the kick message when authentication fails
     */
    String getKickMessage();

    /**
     * Returns the login result.
     *
     * @return login result
     */
    Result getResult();

    enum Result {
        ALLOW,
        DISALLOW_BY_YGGDRASIL_AUTHENTICATOR,
        DISALLOW_BY_VALIDATE_AUTHENTICATOR,
        ERROR
    }
}
