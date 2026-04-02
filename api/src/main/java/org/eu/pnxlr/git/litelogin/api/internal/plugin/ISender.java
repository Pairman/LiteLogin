package org.eu.pnxlr.git.litelogin.api.internal.plugin;

import org.jetbrains.annotations.ApiStatus;

/**
 * Shared command sender abstraction.
 */
@ApiStatus.Internal
public interface ISender {

    /**
     * Returns whether this command sender is a player.
     *
     * @return whether this sender is a player
     */
    boolean isPlayer();

    /**
     * Returns whether this sender is the console.
     */
    boolean isConsole();

    /**
     * Returns whether this sender has a permission.
     *
     * @return whether this sender has a permission
     */
    boolean hasPermission(String permission);

    /**
     * Sends a formatted string message to the sender.
     *
     * @param message message to send
     */
    /*
     * for (String s : message.split("\\r?\\n"))
     *     self.sendMessage(s);
     */
    void sendMessagePL(String message);

    /**
     * Returns the name of this command sender.
     *
     * @return the command sender name
     */
    String getName();

    /**
     * Returns the corresponding player object.
     *
     * @return the corresponding player object
     */
    IPlayer getAsPlayer();
}
