package org.eu.pnxlr.git.litelogin.api.internal.logger.bridges;

import lombok.AllArgsConstructor;
import org.eu.pnxlr.git.litelogin.api.internal.logger.Level;
import org.jetbrains.annotations.ApiStatus;

import java.util.logging.Logger;

/**
 * Bridge for java.util.logging.Logger.
 */
@ApiStatus.Internal
@AllArgsConstructor
public class JavaLoggerBridge extends BaseLoggerBridge {
    private final Logger handler;

    @Override
    public void log(Level level, String message, Throwable throwable) {
//        if (level == Level.DEBUG) {
//            handler.log(java.util.logging.Level.FINER, message, throwable);
//        } else
        if (level == Level.INFO) {
            handler.log(java.util.logging.Level.INFO, message, throwable);
        } else if (level == Level.WARN) {
            handler.log(java.util.logging.Level.WARNING, message, throwable);
        } else if (level == Level.ERROR) {
            handler.log(java.util.logging.Level.SEVERE, message, throwable);
        }
    }
}
