package org.eu.pnxlr.git.litelogin.api.internal.logger;

import org.jetbrains.annotations.ApiStatus;

/**
 * Logger abstraction.
 */
@ApiStatus.Internal
public interface Logger {

    /**
     * Logs a message.
     *
     * @param level     log level
     * @param message   log message
     * @param throwable stack trace
     */
    void log(Level level, String message, Throwable throwable);

    default void log(Level level, String message) {
        log(level, message, null);
    }

    default void log(Level level, Throwable throwable) {
        log(level, null, throwable);
    }

    default void debug(String message, Throwable throwable) {
        log(Level.DEBUG, message, throwable);
    }

    default void debug(String message) {
        log(Level.DEBUG, message);
    }

    default void debug(Throwable throwable) {
        log(Level.DEBUG, null, throwable);
    }

    default void info(String message, Throwable throwable) {
        log(Level.INFO, message, throwable);
    }

    default void info(String message) {
        log(Level.INFO, message);
    }

    default void info(Throwable throwable) {
        log(Level.INFO, null, throwable);
    }

    default void warn(String message, Throwable throwable) {
        log(Level.WARN, message, throwable);
    }

    default void warn(String message) {
        log(Level.WARN, message);
    }

    default void warn(Throwable throwable) {
        log(Level.WARN, null, throwable);
    }

    default void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    default void error(String message) {
        log(Level.ERROR, message);
    }

    default void error(Throwable throwable) {
        log(Level.ERROR, null, throwable);
    }
}
