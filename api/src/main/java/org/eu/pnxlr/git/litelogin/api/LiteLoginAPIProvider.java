package org.eu.pnxlr.git.litelogin.api;

import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

/**
 * 提供API的地方.................................................
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
