package org.eu.pnxlr.git.litelogin.api.internal.command;

import org.eu.pnxlr.git.litelogin.api.internal.plugin.ISender;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Command handler.
 */
@ApiStatus.Internal
public interface CommandAPI {

    /**
     * Executes a command.
     *
     * @param sender command sender
     * @param args   command arguments
     */
    void execute(ISender sender, String[] args);

    void execute(ISender sender, String args);

    /**
     * Provides command completion suggestions.
     *
     * @param sender command sender
     * @param args   command arguments
     */
    List<String> tabComplete(ISender sender, String[] args);

    List<String> tabComplete(ISender sender, String args);
}
