package org.eu.pnxlr.git.litelogin.api;

import org.eu.pnxlr.git.litelogin.api.data.LiteLoginPlayerData;
import org.eu.pnxlr.git.litelogin.api.service.IService;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * LiteLogin API. Everything is exposed here.
 */
@ApiStatus.NonExtendable
public interface LiteLoginAPI {

    /**
     * Returns all authentication services.
     * @return all authentication services
     */
    @NotNull Collection<? extends IService> getServices();

    /**
     * Returns the player's login data by in-game UUID.
     * @param inGameUUID in-game UUID
     * @return the player's login data
     */
    @Nullable
    LiteLoginPlayerData getPlayerData(@NotNull UUID inGameUUID);
}
