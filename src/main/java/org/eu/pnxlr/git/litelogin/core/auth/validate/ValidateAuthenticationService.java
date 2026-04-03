package org.eu.pnxlr.git.litelogin.core.auth.validate;

import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.core.auth.BaseServiceAuthenticationResult;
import org.eu.pnxlr.git.litelogin.core.auth.validate.entry.AssignInGameStep;
import org.eu.pnxlr.git.litelogin.core.auth.validate.entry.InitialLoginDataStep;
import org.eu.pnxlr.git.litelogin.core.auth.validate.entry.NameAllowedRegexCheckStep;
import org.eu.pnxlr.git.litelogin.core.auth.validate.entry.WhitelistCheckStep;
import org.eu.pnxlr.git.litelogin.core.main.Core;

/**
 * Central in-game validation handler.
 */
public class ValidateAuthenticationService {
    private final InitialLoginDataStep initialLoginDataStep;
    private final NameAllowedRegexCheckStep nameAllowedRegexCheckStep;
    private final WhitelistCheckStep whitelistCheckStep;
    private final AssignInGameStep assignInGameStep;

    public ValidateAuthenticationService(Core core) {
        this.initialLoginDataStep = new InitialLoginDataStep(core);
        this.nameAllowedRegexCheckStep = new NameAllowedRegexCheckStep();
        this.whitelistCheckStep = new WhitelistCheckStep(core);
        this.assignInGameStep = new AssignInGameStep(core);
    }

    /**
     * Starts in-game validation.
     */
    public ValidateAuthenticationResult checkIn(BaseServiceAuthenticationResult baseServiceAuthenticationResult) {
        ValidateContext context = new ValidateContext(baseServiceAuthenticationResult);
        try {
            if (!initialLoginDataStep.run(context)) return ValidateAuthenticationResult.ofDisallowed(context.getDisallowMessage());
            if (!nameAllowedRegexCheckStep.run(context)) return ValidateAuthenticationResult.ofDisallowed(context.getDisallowMessage());
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
