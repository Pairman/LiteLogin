package org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil;

import org.eu.pnxlr.git.litelogin.api.service.ServiceType;
import org.eu.pnxlr.git.litelogin.core.configuration.ConfException;
import org.eu.pnxlr.git.litelogin.core.configuration.ProxyConfig;
import org.eu.pnxlr.git.litelogin.core.configuration.SkinRestorerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Blessing Skin 皮肤站 Yggdrasil
 */
public class BlessingSkinYggdrasilServiceConfig extends BaseYggdrasilServiceConfig {
    private final String apiRoot;

    public BlessingSkinYggdrasilServiceConfig(int id, String name, InitUUID initUUID, String initNameFormat, boolean whitelist, SkinRestorerConfig skinRestorer, boolean trackIp, int timeout, int retry, long retryDelay, ProxyConfig authProxy, String apiRoot) throws ConfException {
        super(id, name, initUUID,initNameFormat, whitelist, skinRestorer, trackIp, timeout, retry, retryDelay, authProxy);
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
    protected String getAuthPostContent() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getAuthTrackIpContent() {
        return "&ip={0}";
    }

    @Override
    public HttpRequestMethod getHttpRequestMethod() {
        return HttpRequestMethod.GET;
    }

    @NotNull
    @Override
    public ServiceType getServiceType() {
        return ServiceType.BLESSING_SKIN;
    }
}
