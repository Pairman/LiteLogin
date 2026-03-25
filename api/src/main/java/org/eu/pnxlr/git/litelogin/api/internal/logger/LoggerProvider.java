package org.eu.pnxlr.git.litelogin.api.internal.logger;

import lombok.Getter;
import lombok.Setter;
import org.eu.pnxlr.git.litelogin.api.internal.logger.bridges.ConsoleBridge;
import org.jetbrains.annotations.ApiStatus;

/**
 * 日志提供程序
 */
@ApiStatus.Internal
public class LoggerProvider {
    @Getter
    @Setter
    private static Logger logger = new ConsoleBridge();
}
