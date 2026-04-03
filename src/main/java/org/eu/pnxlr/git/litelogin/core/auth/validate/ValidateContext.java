package org.eu.pnxlr.git.litelogin.core.auth.validate;

import lombok.Data;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.core.auth.BaseServiceAuthenticationResult;

/**
 * In-game validation message context.
 */
@Data
public class ValidateContext {
    private final BaseServiceAuthenticationResult baseServiceAuthenticationResult;

    private final GameProfile inGameProfile;
    private String disallowMessage;
    private boolean needWait;
    private boolean onlineNameUpdated = false;


    protected ValidateContext(BaseServiceAuthenticationResult baseServiceAuthenticationResult) {
        this.baseServiceAuthenticationResult = baseServiceAuthenticationResult;
        this.inGameProfile = baseServiceAuthenticationResult.getResponse().clone();
    }
}
