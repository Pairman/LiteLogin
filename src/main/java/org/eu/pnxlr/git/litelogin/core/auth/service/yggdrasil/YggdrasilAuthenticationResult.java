package org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil;

import lombok.Getter;
import lombok.ToString;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.core.auth.BaseServiceAuthenticationResult;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig;

/**
 * HasJoined authentication result.
 */
@Getter
@ToString
public class YggdrasilAuthenticationResult extends BaseServiceAuthenticationResult {
    private final Reason reason;

    public YggdrasilAuthenticationResult(Reason reason, GameProfile response, BaseYggdrasilServiceConfig serviceConfig) {
        super(response, serviceConfig);
        this.reason = reason;
    }

    /**
     * Creates a result that allows login.
     */
    protected static YggdrasilAuthenticationResult ofAllowed(GameProfile response, BaseYggdrasilServiceConfig serviceConfig) {
        return new YggdrasilAuthenticationResult(Reason.ALLOWED, response, serviceConfig);
    }

    /**
     * Creates a result for server outage.
     */
    protected static YggdrasilAuthenticationResult ofServerBreakdown() {
        return new YggdrasilAuthenticationResult(Reason.SERVER_BREAKDOWN, null, null);
    }

    /**
     * Creates a result for authentication failure.
     */
    protected static YggdrasilAuthenticationResult ofValidationFailed() {
        return new YggdrasilAuthenticationResult(Reason.VALIDATION_FAILED, null, null);
    }

    /**
     * Creates a result for the case where no authentication service is configured.
     */
    protected static YggdrasilAuthenticationResult ofNoService() {
        return new YggdrasilAuthenticationResult(Reason.NO_SERVICE, null, null);
    }

    @Override
    public boolean isAllowed() {
        return reason == Reason.ALLOWED;
    }

    public enum Reason {
        // Allowed
        ALLOWED,
        // Server outage or invalid response
        SERVER_BREAKDOWN,
        // Authentication failed
        VALIDATION_FAILED,
        // No authentication service configured
        NO_SERVICE;
    }
}
