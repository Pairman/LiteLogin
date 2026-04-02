package org.eu.pnxlr.git.litelogin.core.auth.validate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;

/**
 * In-game validation result.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class ValidateAuthenticationResult {
    private final Reason reason;
    private final GameProfile inGameProfile;
    private final String disallowedMessage;


    /**
     * Returns a result that allows login.
     */
    public static ValidateAuthenticationResult ofAllowed(GameProfile response) {
        return new ValidateAuthenticationResult(Reason.ALLOWED, response, null);
    }

    /**
     * Returns a result that disallows login.
     */
    public static ValidateAuthenticationResult ofDisallowed(String disallowedMessage) {
        return new ValidateAuthenticationResult(Reason.DISALLOWED, null, disallowedMessage);
    }

    public enum Reason {
        // Login allowed
        ALLOWED,
        // Login disallowed
        DISALLOWED;
    }
}
