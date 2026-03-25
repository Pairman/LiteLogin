package org.eu.pnxlr.git.litelogin.api.internal.injector;

import org.eu.pnxlr.git.litelogin.api.internal.main.CoreAPI;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 子模块注入接口
 */
@ApiStatus.Internal
public interface Injector {

    /**
     * 开始注入
     */
    void inject(CoreAPI api) throws Throwable;
    void registerChatSession(Map<Integer,Integer> packetMapping);
}
