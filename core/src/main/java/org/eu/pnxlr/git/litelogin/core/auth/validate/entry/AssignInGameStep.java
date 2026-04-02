package org.eu.pnxlr.git.litelogin.core.auth.validate.entry;

import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.auth.validate.ValidateContext;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.UUID;

/**
 * Initial in-game UUID assignment step.
 */
public class AssignInGameStep {
    private final Core core;

    public AssignInGameStep(Core core) {
        this.core = core;
    }

    @SneakyThrows
    public boolean run(ValidateContext validateContext) {

        // Read the in-game UUID of the login profile from the database
        UUID inGameUUID = core.getSqlManager().getUserDataTable().getInGameUUID(
                validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId()
        );

        // If this UUID does not exist, the player is either new or their profile was cleaned up.
        // In this case, assign a new in-game identity.
        if (inGameUUID == null) {
            inGameUUID = validateContext.getBaseServiceAuthenticationResult().getResponse().getId();

            // Must be thread-safe
            synchronized (AssignInGameStep.class) {
                // Find an unused UUID
                while (core.getSqlManager().getInGameProfileTable().dataExists(inGameUUID)) {
                    LoggerProvider.getLogger().warn(String.format("UUID %s has been used and will take a random UUID instead.", inGameUUID));
                    inGameUUID = UUID.randomUUID();
                }
                // The identity UUID has been determined
                // Update the data
                core.getSqlManager().getUserDataTable().setInGameUUID(
                        validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                        validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId(),
                        inGameUUID);
            }
        }
        if (validateContext.isOnlineNameUpdated()) {
            String username = core.getSqlManager().getInGameProfileTable().getUsername(inGameUUID);
            if (!ValueUtil.isEmpty(username)) {
                core.getSqlManager().getInGameProfileTable().eraseUsername(username);
            }
        }

        // The identity UUID exists, check whether a matching record exists in the database
        boolean exist = core.getSqlManager().getInGameProfileTable().dataExists(inGameUUID);
        if (exist) {
            String username = core.getSqlManager().getInGameProfileTable().getUsername(inGameUUID);
            if (!ValueUtil.isEmpty(username)) {
                validateContext.getInGameProfile().setId(inGameUUID);
                validateContext.getInGameProfile().setName(username);
                return true;
            }
        }

        String loginName = validateContext.getBaseServiceAuthenticationResult().getResponse().getName();

        // Username needs to be updated
        if (exist) {
            try {
                core.getSqlManager().getInGameProfileTable().updateUsername(inGameUUID,
                        loginName);
                validateContext.getInGameProfile().setId(inGameUUID);
                validateContext.getInGameProfile().setName(loginName);
                return true;
            } catch (SQLIntegrityConstraintViolationException e) {
                validateContext.setDisallowMessage(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "Username {name} is already used.",
                        new Pair<>("name", loginName)
                ));
                return false;
            }
        } else {
            try {
                core.getSqlManager().getInGameProfileTable().insertNewData(inGameUUID,
                        loginName);
                validateContext.getInGameProfile().setId(inGameUUID);
                validateContext.getInGameProfile().setName(loginName);
                return true;
            } catch (SQLIntegrityConstraintViolationException e) {
                validateContext.setDisallowMessage(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "Your username {name} is already used.",
                        new Pair<>("name", loginName)
                ));
                return false;
            }
        }
    }
}
