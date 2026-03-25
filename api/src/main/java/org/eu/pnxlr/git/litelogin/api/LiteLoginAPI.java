package org.eu.pnxlr.git.litelogin.api;

import org.eu.pnxlr.git.litelogin.api.data.LiteLoginPlayerData;
import org.eu.pnxlr.git.litelogin.api.service.IService;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * LiteLogin的API, 全部都在这里了
 */
@ApiStatus.NonExtendable
public interface LiteLoginAPI {

    /**
     * 返回所有验证服务列表
     * @return 所有验证服务列表
     */
    @NotNull Collection<? extends IService> getServices();

    /**
     * 通过游戏内 uuid, 返回玩家的登录数据
     * @param inGameUUID 游戏内uuid
     * @return 玩家的登录数据
     */
    @Nullable
    LiteLoginPlayerData getPlayerData(@NotNull UUID inGameUUID);
}
