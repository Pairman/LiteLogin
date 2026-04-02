package org.eu.pnxlr.git.litelogin.core.auth.validate.entry;

import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.core.auth.validate.ValidateContext;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.util.Locale;

/**
 * Whitelist checker.
 */
public class WhitelistCheckStep {

    private final Core core;

    public WhitelistCheckStep(Core core) {
        this.core = core;
    }

    @SneakyThrows
    public boolean run(ValidateContext validateContext) {
        boolean removed = core.getCachedWhitelist().remove(validateContext.getBaseServiceAuthenticationResult().getResponse().getName().toLowerCase(Locale.ROOT));
        if (removed) {
            core.getSqlManager().getUserDataTable().setWhitelist(validateContext.getBaseServiceAuthenticationResult().getResponse().getId(), validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId(), true);
        }
        // If whitelist validation is disabled
        if (!validateContext.getBaseServiceAuthenticationResult().getServiceConfig().isWhitelist()) {
            return true;
        }
        // If the player is whitelisted
        if (core.getSqlManager().getUserDataTable().hasWhitelist(validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId())) {
            return true;
        }
        // Kick the player
        validateContext.setDisallowMessage("You are not whitelisted for this service.");
        return false;
    }
}
