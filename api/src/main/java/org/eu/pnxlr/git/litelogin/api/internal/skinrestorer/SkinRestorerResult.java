package org.eu.pnxlr.git.litelogin.api.internal.skinrestorer;

import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface SkinRestorerResult {

    Reason getReason();

    GameProfile getResponse();

    Throwable getThrowable();

    enum Reason {
        // The profile does not contain a skin
        NO_SKIN,

        // Skin restoration is disabled
        NO_RESTORER,

        // Used cached restored skin data
        USE_CACHE,

        // The skin signature is valid and no repair is needed
        SIGNATURE_VALID,

        // The skin is invalid, for example HD or transparent skins
        BAD_SKIN,

        // Skin restoration succeeded
        RESTORER_SUCCEED,

        // Non-blocking restoration
        RESTORER_ASYNC;
    }
}
