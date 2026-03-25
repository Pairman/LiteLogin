package org.eu.pnxlr.git.litelogin.core.auth.validate.entry;

import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.auth.validate.ValidateContext;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.util.regex.Pattern;

/**
 * 玩家名字正则检查器
 */
public class NameAllowedRegularCheckStep {
    private final Core core;

    public NameAllowedRegularCheckStep(Core core) {
        this.core = core;
    }

    public boolean run(ValidateContext validateContext) {
        String nameAllowedRegular = core.getPluginConfig().getNameAllowedRegular();
        if (ValueUtil.isEmpty(nameAllowedRegular)) {
            return true;
        }
        if (!Pattern.matches(nameAllowedRegular, validateContext.getBaseServiceAuthenticationResult().getResponse().getName())) {
            validateContext.setDisallowMessage(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§cYour username §e{name}§c does not match the required pattern §e{regular}§c.",
                    new Pair<>("name", validateContext.getBaseServiceAuthenticationResult().getResponse().getName()),
                    new Pair<>("regular", nameAllowedRegular)
            ));
            return false;
        }
        return true;
    }
}
