package org.eu.pnxlr.git.litelogin.core.auth.validate.entry;

import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.core.auth.validate.ValidateContext;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.util.Locale;

/**
 * 白名单检查器
 */
public class WhitelistCheckStep {

    private final Core core;

    public WhitelistCheckStep(Core core) {
        this.core = core;
    }

    @SneakyThrows
    public boolean run(ValidateContext validateContext) {
        boolean removed = core.getCacheWhitelistHandler().getCachedWhitelist().remove(validateContext.getBaseServiceAuthenticationResult().getResponse().getName().toLowerCase(Locale.ROOT));
        if (removed) {
            core.getSqlManager().getUserDataTable().setWhitelist(validateContext.getBaseServiceAuthenticationResult().getResponse().getId(), validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId(), true);
        }
        // 如果没有开启白名单验证
        if (!validateContext.getBaseServiceAuthenticationResult().getServiceConfig().isWhitelist()) {
            return true;
        }
        // 如果有白名单
        if (core.getSqlManager().getUserDataTable().hasWhitelist(validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId())) {
            return true;
        }
        // 踹了
        validateContext.setDisallowMessage("§cYou are not whitelisted for this service.");
        return false;
    }
}
