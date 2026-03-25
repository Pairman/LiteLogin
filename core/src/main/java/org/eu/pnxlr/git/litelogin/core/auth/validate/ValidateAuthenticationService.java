package org.eu.pnxlr.git.litelogin.core.auth.validate;

import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.core.auth.service.BaseServiceAuthenticationResult;
import org.eu.pnxlr.git.litelogin.core.auth.validate.entry.AssignInGameStep;
import org.eu.pnxlr.git.litelogin.core.auth.validate.entry.InitialLoginDataStep;
import org.eu.pnxlr.git.litelogin.core.auth.validate.entry.NameAllowedRegularCheckStep;
import org.eu.pnxlr.git.litelogin.core.auth.validate.entry.WhitelistCheckStep;
import org.eu.pnxlr.git.litelogin.core.main.Core;

/**
 * 游戏内验证集中处理程序
 */
public class ValidateAuthenticationService {
    private final Core core;
    private final InitialLoginDataStep initialLoginDataStep;
    private final NameAllowedRegularCheckStep nameAllowedRegularCheckStep;
    private final WhitelistCheckStep whitelistCheckStep;
    private final AssignInGameStep assignInGameStep;

    public ValidateAuthenticationService(Core core) {
        this.core = core;
        this.initialLoginDataStep = new InitialLoginDataStep(core);
        this.nameAllowedRegularCheckStep = new NameAllowedRegularCheckStep(core);
        this.whitelistCheckStep = new WhitelistCheckStep(core);
        this.assignInGameStep = new AssignInGameStep(core);
    }

    /**
     * 开始游戏内验证
     */
    public ValidateAuthenticationResult checkIn(BaseServiceAuthenticationResult baseServiceAuthenticationResult) {
        ValidateContext context = new ValidateContext(baseServiceAuthenticationResult);
        try {
            if (!initialLoginDataStep.run(context)) return ValidateAuthenticationResult.ofDisallowed(context.getDisallowMessage());
            if (!nameAllowedRegularCheckStep.run(context)) return ValidateAuthenticationResult.ofDisallowed(context.getDisallowMessage());
            if (!whitelistCheckStep.run(context)) return ValidateAuthenticationResult.ofDisallowed(context.getDisallowMessage());
            if (!assignInGameStep.run(context)) return ValidateAuthenticationResult.ofDisallowed(context.getDisallowMessage());
            if (context.isNeedWait()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LoggerProvider.getLogger().debug(e);
                }
            }
            return ValidateAuthenticationResult.ofAllowed(context.getInGameProfile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
