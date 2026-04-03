package org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil;

import java.io.IOException;

import org.eu.pnxlr.git.litelogin.api.service.ServiceType;
import org.eu.pnxlr.git.litelogin.core.configuration.SkinRestorerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Blessing Skin Yggdrasil service.
 */
public class BlessingSkinYggdrasilServiceConfig extends BaseYggdrasilServiceConfig {
    private final String apiRoot;

    public BlessingSkinYggdrasilServiceConfig(int id, String name, boolean whitelist, SkinRestorerConfig skinRestorer, boolean trackIp, String apiRoot) throws IOException {
        super(id, name, whitelist, skinRestorer, trackIp);
        if (!apiRoot.endsWith("/")) {
            apiRoot = apiRoot.concat("/");
        }
        this.apiRoot = apiRoot;
    }


    @Override
    protected String getAuthURL() {
        return apiRoot.concat("session")
                .concat("server")
                .concat("/session")
                .concat("/minecraft")
                .concat("/hasJoined?")
                .concat("username={0}&serverId={1}{2}");
    }

    @Override
    protected String getAuthTrackIpContent() {
        return "&ip={0}";
    }

    @NotNull
    @Override
    public ServiceType getServiceType() {
        return ServiceType.BLESSING_SKIN;
    }
}
