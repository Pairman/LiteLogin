package org.eu.pnxlr.git.litelogin.core.auth;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.core.configuration.BaseServiceConfig;

@Getter
public abstract class BaseServiceAuthenticationResult {
    private final GameProfile response;
    private final BaseServiceConfig serviceConfig;

    public BaseServiceAuthenticationResult(GameProfile response, BaseServiceConfig serviceConfig) {
        this.response = response;
        this.serviceConfig = serviceConfig;
    }

    public abstract boolean isAllowed();
}
