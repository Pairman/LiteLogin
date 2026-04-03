package org.eu.pnxlr.git.litelogin.api.service;

import org.jetbrains.annotations.ApiStatus;

/**
 * Authentication service types.
 */
@ApiStatus.NonExtendable
public enum ServiceType {

    /**
     * Official Yggdrasil account authentication service for Minecraft: Java Edition.
     */
    OFFICIAL,

    /**
     * Blessing Skin Yggdrasil-compatible authentication service.
     */
    BLESSING_SKIN
}
