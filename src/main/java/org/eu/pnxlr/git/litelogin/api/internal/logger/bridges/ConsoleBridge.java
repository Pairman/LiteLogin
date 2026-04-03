package org.eu.pnxlr.git.litelogin.api.internal.logger.bridges;

import lombok.NoArgsConstructor;
import org.eu.pnxlr.git.litelogin.api.internal.logger.Level;
import org.eu.pnxlr.git.litelogin.api.internal.logger.Logger;
import org.jetbrains.annotations.ApiStatus;

/**
 * Console logger bridge.
 */
@ApiStatus.Internal
@NoArgsConstructor
public class ConsoleBridge implements Logger {


    @Override
    public void log(Level level, String message, Throwable throwable) {
        if (level == Level.DEBUG) {
            System.out.println("[DEBUG] " + message);
            if (throwable != null)
                throwable.printStackTrace(System.out);
        } else if (level == Level.INFO) {
            System.out.println("[INFO] " + message);
            if (throwable != null)
                throwable.printStackTrace(System.out);
        } else if (level == Level.WARN) {
            System.out.println("[WARN] " + message);
            if (throwable != null)
                throwable.printStackTrace(System.err);
        } else if (level == Level.ERROR) {
            System.out.println("[ERROR] " + message);
            if (throwable != null)
                throwable.printStackTrace(System.err);
        }
    }
}
