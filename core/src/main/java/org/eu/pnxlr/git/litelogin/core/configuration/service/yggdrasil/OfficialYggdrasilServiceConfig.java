package org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil;

import java.io.IOException;

import org.eu.pnxlr.git.litelogin.api.service.ServiceType;
import org.eu.pnxlr.git.litelogin.core.configuration.SkinRestorerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Official Yggdrasil service.
 */
public class OfficialYggdrasilServiceConfig extends BaseYggdrasilServiceConfig {
    private final String customSessionServer;

    public OfficialYggdrasilServiceConfig(int id, String name, boolean whitelist, SkinRestorerConfig skinRestorer, boolean trackIp, String customSessionServer) throws IOException {
        super(id, name, whitelist, skinRestorer, trackIp);
        if (!customSessionServer.endsWith("/")) {
            customSessionServer = customSessionServer.concat("/");
        }
        this.customSessionServer = customSessionServer;
    }


    @Override
    protected String getAuthURL() {
	    String baseUrl = customSessionServer;
	    return baseUrl.concat("session/minecraft/hasJoined?username={0}&serverId={1}{2}");
    }

    @Override
    protected String getAuthTrackIpContent() {
        return "&ip={0}";
    }

    @NotNull
    @Override
    public ServiceType getServiceType() {
        return ServiceType.OFFICIAL;
    }
}
