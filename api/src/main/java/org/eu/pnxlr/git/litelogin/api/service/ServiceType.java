package org.eu.pnxlr.git.litelogin.api.service;

import org.jetbrains.annotations.ApiStatus;

/**
 * 验证服务类型
 */
@ApiStatus.NonExtendable
public enum ServiceType {

    /**
     * 官方 Yggdrasil Java 版账号验证服务（Yggdrasil 实现）。
     */
    OFFICIAL,

    /**
     * Blessing Skin 的伪正版验证服务（Yggdrasil 实现）。
     */
    BLESSING_SKIN
}
