package org.eu.pnxlr.git.litelogin.api.internal.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a handler result.
 */
@Getter
@ApiStatus.Internal
@AllArgsConstructor
public class HandleResult {
    // Handler result type
    private final Type type;
    // Kick message to display when the player is disconnected
    private final String kickMessage;

    public enum Type {
        NONE,
        KICK;
    }
}
