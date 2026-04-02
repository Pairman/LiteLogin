package org.eu.pnxlr.git.litelogin.velocity.main;

import org.eu.pnxlr.git.litelogin.api.internal.logger.Level;
import org.eu.pnxlr.git.litelogin.api.internal.logger.bridges.BaseLoggerBridge;
import org.slf4j.Logger;

/**
 * Slf4J logger bridge.
 */
public class Slf4jLoggerBridge extends BaseLoggerBridge {
    private final Logger logger;

    public Slf4jLoggerBridge(Logger logger) {
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
