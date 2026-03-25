package org.eu.pnxlr.git.litelogin.core.auth.validate.entry;

import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.core.auth.validate.ValidateContext;
import org.eu.pnxlr.git.litelogin.core.database.table.UserDataTableV3;
import org.eu.pnxlr.git.litelogin.core.main.Core;

public class InitialLoginDataStep {
    private final Core core;

    public InitialLoginDataStep(Core core) {
        this.core = core;
    }

    @SneakyThrows
    public boolean run(ValidateContext validateContext) {
        UserDataTableV3 dataTable = core.getSqlManager().getUserDataTable();
        if (!dataTable.dataExists(
                validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId()
        )) {
            dataTable.insertNewData(
                    validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                    validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId(),
                    validateContext.getBaseServiceAuthenticationResult().getResponse().getName(),
                    null
            );
        } else {
            String currentName = dataTable.getOnlineName(validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                    validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId());
            if(!validateContext.getBaseServiceAuthenticationResult().getResponse().getName().equals(currentName)){
                dataTable.setOnlineName(
                        validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                        validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId(),
                        validateContext.getBaseServiceAuthenticationResult().getResponse().getName()
                );
                validateContext.setOnlineNameUpdated(true);
            }

        }
        return true;
    }
}
