package org.eu.pnxlr.git.litelogin.api.internal.main;

import org.eu.pnxlr.git.litelogin.api.internal.auth.AuthAPI;
import org.eu.pnxlr.git.litelogin.api.internal.command.CommandAPI;
import org.eu.pnxlr.git.litelogin.api.internal.handle.HandlerAPI;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlugin;
import org.eu.pnxlr.git.litelogin.api.internal.skinrestorer.SkinRestorerAPI;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

@ApiStatus.Internal
public interface CoreAPI {

    /**
     * Loads the LiteLogin core.
     */
    void load() throws Exception;

    /**
     * Shuts down the LiteLogin core.
     */
    void close() throws Exception;

    /**
     * Returns the command handler.
     */
    CommandAPI getCommandHandler();

    /**
     * Returns the mixed authentication handler.
     */
    AuthAPI getAuthHandler();

    /**
     * Returns the skin restoration handler.
     */
    SkinRestorerAPI getSkinRestorerHandler();

    /**
     * Returns the cache handler.
     */
    HandlerAPI getPlayerHandler();

    Map<Integer, Integer> getPacketMapping();

    boolean persistPacketMapping(int protocol, int packetId);

    /**
     * Returns the plugin instance.
     */
    IPlugin getPlugin();
}
