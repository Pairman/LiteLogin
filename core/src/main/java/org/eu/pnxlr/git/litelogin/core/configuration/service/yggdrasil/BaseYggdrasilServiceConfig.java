package org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.configuration.SkinRestorerConfig;
import org.eu.pnxlr.git.litelogin.core.configuration.BaseServiceConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
public abstract class BaseYggdrasilServiceConfig extends BaseServiceConfig {
    private static final int AUTH_TIMEOUT_MILLIS = 15000;
    private static final int AUTH_RETRY_COUNT = 3;
    private static final long AUTH_RETRY_DELAY_MILLIS = 0L;

    private final boolean trackIp;
    private final int timeout = AUTH_TIMEOUT_MILLIS;
    private final int retry = AUTH_RETRY_COUNT;
    private final long retryDelay = AUTH_RETRY_DELAY_MILLIS;

    protected BaseYggdrasilServiceConfig(int id, String name, boolean whitelist, SkinRestorerConfig skinRestorer,
                                         boolean trackIp) throws IOException {
        super(id, name, whitelist, skinRestorer);
        this.trackIp = trackIp;
    }


    /**
     * Generates the authentication URL.
     */
    public String generateAuthURL(String username, String serverId, String ip) {
        return ValueUtil.transPapi(getAuthURL(),
                new Pair<>("username", URLEncoder.encode(username, StandardCharsets.UTF_8)),
                new Pair<>("serverId", URLEncoder.encode(serverId, StandardCharsets.UTF_8)),
                new Pair<>("ip", generateTraceIpContent(ip)));
    }

    private String generateTraceIpContent(String ip) {
        if (!trackIp) {
            return "";
        }
        if (ValueUtil.isEmpty(ip)) {
            return "";
        }
        String trackIpContent = getAuthTrackIpContent();
        if (ValueUtil.isEmpty(trackIpContent)) {
            return "";
        }
        return ValueUtil.transPapi(trackIpContent,
                new Pair<>("ip", ip));
    }

    /**
     * Returns the base authentication URL template.
     */
    protected abstract String getAuthURL();

    /**
     * Returns the IP portion of the authentication request.
     */
    protected abstract String getAuthTrackIpContent();
}
