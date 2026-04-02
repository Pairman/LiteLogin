package org.eu.pnxlr.git.litelogin.api.internal.plugin;

import org.jetbrains.annotations.ApiStatus;

import java.io.File;

@ApiStatus.Internal
public interface IPlugin {

    /**
     * Returns the config and data directory.
     *
     * @return the config and data directory
     */
    File getDataFolder();

    /**
     * Returns the temporary directory.
     *
     * @return the temporary directory
     */
    File getTempFolder();

    /**
     * Returns the server instance.
     *
     * @return the server instance
     */
    IServer getRunServer();
}
