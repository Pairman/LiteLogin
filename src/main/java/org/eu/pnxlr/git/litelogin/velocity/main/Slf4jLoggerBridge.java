package org.eu.pnxlr.git.litelogin.velocity.main;

import org.eu.pnxlr.git.litelogin.api.internal.logger.Level;
import org.eu.pnxlr.git.litelogin.api.internal.logger.Logger;

/**
 * Slf4J logger bridge.
 */
public class Slf4jLoggerBridge implements Logger {
    private final org.slf4j.Logger logger;

    public Slf4jLoggerBridge(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(Level level, String message, Throwable throwable) {
        if (level == Level.DEBUG) {
            logger.debug(message, throwable);
        } else if (level == Level.INFO) {
            logger.info(message, throwable);
        } else if (level == Level.WARN) {
            logger.warn(message, throwable);
        } else if (level == Level.ERROR) {
            logger.error(message, throwable);
        }
    }
}
