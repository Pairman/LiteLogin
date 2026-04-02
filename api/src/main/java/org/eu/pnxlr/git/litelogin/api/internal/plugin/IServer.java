package org.eu.pnxlr.git.litelogin.api.internal.plugin;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IServer {

    /**
     * Returns the thread scheduler.
     *
     * @return the thread scheduler
     */
    BaseScheduler getScheduler();

    /**
     * Returns the player manager.
     *
     * @return the player manager
     */
    IPlayerManager getPlayerManager();

    /**
     * Returns whether online-mode authentication is enabled.
     *
     * @return online-mode status
     */
    boolean isOnlineMode();

    /**
     * Returns whether the basic UUID forwarding feature is enabled.
     */
    boolean isForwarded();

    /**
     * Returns the server core name.
     *
     * @return the server core name
     */
    String getName();

    /**
     * Returns the server version.
     *
     * @return the server version
     */
    String getVersion();

    /**
     * Shuts down the server.
     */
    void shutdown();

    /**
     * Returns the console sender.
     */
    ISender getConsoleSender();

    /**
     * Returns whether a plugin is loaded.
     */
    boolean pluginHasEnabled(String id);
}
