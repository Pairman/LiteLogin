package org.eu.pnxlr.git.litelogin.api.internal.skinrestorer;

import org.eu.pnxlr.git.litelogin.api.internal.auth.AuthResult;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface SkinRestorerAPI {

    /**
     * Performs skin restoration.
     */
    SkinRestorerResult doRestorer(AuthResult result);

}
