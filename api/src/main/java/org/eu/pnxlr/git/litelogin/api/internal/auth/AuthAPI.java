package org.eu.pnxlr.git.litelogin.api.internal.auth;

import org.jetbrains.annotations.ApiStatus;

/**
 * Authentication API.
 */
@ApiStatus.Internal
public interface AuthAPI {

    /**
     * Performs authentication.
     *
     * @param username username
     * @param serverId server ID
     * @param ip       player IP
     * @return authentication result
     */
    AuthResult auth(String username, String serverId, String ip);
}
