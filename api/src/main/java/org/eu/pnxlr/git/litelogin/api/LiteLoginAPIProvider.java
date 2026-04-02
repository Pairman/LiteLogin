package org.eu.pnxlr.git.litelogin.api;

import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

/**
 * Provides access to the API.
 */
@ApiStatus.NonExtendable
public class LiteLoginAPIProvider {
    @Getter
    private static LiteLoginAPI api;

    @ApiStatus.Internal
    public synchronized static void setApi(LiteLoginAPI api) {
        if (LiteLoginAPIProvider.api != null) throw new UnsupportedOperationException("duplicate api.");
        LiteLoginAPIProvider.api = api;
    }
}
