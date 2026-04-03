package org.eu.pnxlr.git.litelogin.core.auth.validate.entry;

import org.eu.pnxlr.git.litelogin.api.internal.util.tuple.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.auth.validate.ValidateContext;
import java.util.regex.Pattern;

/**
 * Player name regex validator.
 */
public class NameAllowedRegexCheckStep {
    private static final String NAME_ALLOWED_REGEX = "^[a-zA-Z0-9_]+$";

    public boolean run(ValidateContext validateContext) {
        if (ValueUtil.isEmpty(NAME_ALLOWED_REGEX)) {
            return true;
        }
        if (!Pattern.matches(NAME_ALLOWED_REGEX, validateContext.getBaseServiceAuthenticationResult().getResponse().getName())) {
            validateContext.setDisallowMessage(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "Your username {name} does not match the required pattern {regular}.",
                    new Pair<>("name", validateContext.getBaseServiceAuthenticationResult().getResponse().getName()),
                    new Pair<>("regular", NAME_ALLOWED_REGEX)
            ));
            return false;
        }
        return true;
    }
}
