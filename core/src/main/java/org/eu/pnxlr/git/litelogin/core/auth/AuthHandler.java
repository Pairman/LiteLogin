package org.eu.pnxlr.git.litelogin.core.auth;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.auth.AuthAPI;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.core.auth.service.BaseServiceAuthenticationResult;
import org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil.YggdrasilAuthenticationResult;
import org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil.YggdrasilAuthenticationService;
import org.eu.pnxlr.git.litelogin.core.auth.validate.ValidateAuthenticationResult;
import org.eu.pnxlr.git.litelogin.core.auth.validate.ValidateAuthenticationService;
import org.eu.pnxlr.git.litelogin.core.handle.PlayerHandler;
import org.eu.pnxlr.git.litelogin.core.main.Core;

/**
 * 验证核心
 */
@Getter
public class AuthHandler implements AuthAPI {
    private final Core core;
    private final YggdrasilAuthenticationService yggdrasilAuthenticationService;
    private final ValidateAuthenticationService validateAuthenticationService;


    public AuthHandler(Core core) {
        this.core = core;
        this.yggdrasilAuthenticationService = new YggdrasilAuthenticationService(core);
        this.validateAuthenticationService = new ValidateAuthenticationService(core);
    }


    /**
     * 开始验证
     *
     * @param username 用户名
     * @param serverId 服务器ID
     * @param ip       用户IP
     */
    @Override
    public LoginAuthResult auth(String username, String serverId, String ip) {
        YggdrasilAuthenticationResult yggdrasilAuthenticationResult;
        try {
            yggdrasilAuthenticationResult = yggdrasilAuthenticationService.hasJoined(username, serverId, ip);
            if (yggdrasilAuthenticationResult.getReason() == YggdrasilAuthenticationResult.Reason.NO_SERVICE) {
                return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(yggdrasilAuthenticationResult, "§cNo Yggdrasil authentication service is configured on this server. Please contact the server administrator.");
            }
            if (yggdrasilAuthenticationResult.getReason() == YggdrasilAuthenticationResult.Reason.SERVER_BREAKDOWN) {
                return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(yggdrasilAuthenticationResult, "§cSession validation is temporarily unavailable due to a network issue or an unexpected response from the authentication server. Please try again.");
            }
            if (yggdrasilAuthenticationResult.getReason() == YggdrasilAuthenticationResult.Reason.VALIDATION_FAILED) {
                return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(yggdrasilAuthenticationResult, "§cInvalid session. Please check your external login client configuration.");
            }
            if (yggdrasilAuthenticationResult.getReason() != YggdrasilAuthenticationResult.Reason.ALLOWED ||
                    yggdrasilAuthenticationResult.getResponse() == null ||
                    yggdrasilAuthenticationResult.getServiceConfig().getId() == -1) {
                return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(yggdrasilAuthenticationResult, "§cAn unknown authentication error occurred. Please contact the server administrator.");
            }
        } catch (Exception e) {
            LoggerProvider.getLogger().error("An exception occurred while processing the hasJoined request.", e);
            return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(null, "§cAn error occurred while processing your login data. Please contact the server administrator.");
        }

        return checkIn(yggdrasilAuthenticationResult);
    }

    public LoginAuthResult checkIn(BaseServiceAuthenticationResult baseServiceAuthenticationResult) {
        try {
            ValidateAuthenticationResult validateAuthenticationResult = validateAuthenticationService.checkIn(baseServiceAuthenticationResult);
            if (validateAuthenticationResult.getReason() == ValidateAuthenticationResult.Reason.ALLOWED) {
                LoggerProvider.getLogger().info(
                        String.format("%s(uuid: %s) from authentication service %s(sid: %d) has been authenticated, profile redirected to %s(uuid: %s).",
                                baseServiceAuthenticationResult.getResponse().getName(),
                                baseServiceAuthenticationResult.getResponse().getId().toString(),
                                baseServiceAuthenticationResult.getServiceConfig().getName(),
                                baseServiceAuthenticationResult.getServiceConfig().getId(),
                                validateAuthenticationResult.getInGameProfile().getName(),
                                validateAuthenticationResult.getInGameProfile().getId().toString()
                        )
                );
                GameProfile finalProfile = validateAuthenticationResult.getInGameProfile();
                core.getPlayerHandler().getLoginCache().put(finalProfile.getId(), new PlayerHandler.Entry(
                        baseServiceAuthenticationResult.getResponse(),
                        baseServiceAuthenticationResult.getServiceConfig(),
                        System.currentTimeMillis()
                ));
                return LoginAuthResult.ofAllowed(baseServiceAuthenticationResult, validateAuthenticationResult, finalProfile);
            }
            return LoginAuthResult.ofDisallowedByValidateAuthenticator(baseServiceAuthenticationResult, validateAuthenticationResult, validateAuthenticationResult.getDisallowedMessage());
        } catch (Exception e) {
            LoggerProvider.getLogger().error("An exception occurred while processing the validation request.", e);
            return LoginAuthResult.ofDisallowedByValidateAuthenticator(baseServiceAuthenticationResult, null, "§cAn error occurred while checking whether you are allowed to join this server. Please contact the server administrator.");
        }
    }
}
