package org.eu.pnxlr.git.litelogin.api.internal.injector;

import org.eu.pnxlr.git.litelogin.api.internal.main.CoreAPI;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * Injection interface for submodules.
 */
@ApiStatus.Internal
public interface Injector {

    /**
     * Starts injection.
     */
    void inject(CoreAPI api) throws Throwable;
    void registerChatSession(Map<Integer,Integer> packetMapping);
}
