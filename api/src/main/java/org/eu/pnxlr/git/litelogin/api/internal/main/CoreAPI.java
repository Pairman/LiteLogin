package org.eu.pnxlr.git.litelogin.api.internal.main;

import org.eu.pnxlr.git.litelogin.api.MapperConfigAPI;
import org.eu.pnxlr.git.litelogin.api.internal.auth.AuthAPI;
import org.eu.pnxlr.git.litelogin.api.internal.command.CommandAPI;
import org.eu.pnxlr.git.litelogin.api.internal.handle.HandlerAPI;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlugin;
import org.eu.pnxlr.git.litelogin.api.internal.skinrestorer.SkinRestorerAPI;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface CoreAPI {

    /**
     * 加载LiteLogin核心
     */
    void load() throws Exception;

    /**
     * 关闭LiteLogin核心
     */
    void close() throws Exception;

    /**
     * 返回命令处理程序
     */
    CommandAPI getCommandHandler();

    /**
     * 返回混合验证处理程序
     */
    AuthAPI getAuthHandler();

    /**
     * 返回皮肤修复程序
     */
    SkinRestorerAPI getSkinRestorerHandler();

    /**
     * 获得缓存
     */
    HandlerAPI getPlayerHandler();

    /**
     * 获得版本映射
     */
    MapperConfigAPI getMapperConfig();

    /**
     * 获得插件对象
     */
    IPlugin getPlugin();
}
