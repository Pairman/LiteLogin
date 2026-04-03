package org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil;

import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.internal.util.tuple.Pair;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig;
import org.eu.pnxlr.git.litelogin.core.http.HttpClientHelper;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.io.IOException;

/**
 * Performs a hasJoined request against one Yggdrasil service.
 */
public class YggdrasilAuthenticationTask {
    private final Core core;
    private final String username;
    private final String serverId;
    private final String ip;
    private final BaseYggdrasilServiceConfig config;

    protected YggdrasilAuthenticationTask(Core core, String username, String serverId, String ip, BaseYggdrasilServiceConfig config) {
        this.core = core;
        this.username = username;
        this.serverId = serverId;
        this.ip = ip;
        this.config = config;
    }

    // Perform authentication
    public GameProfile call() throws Exception {
        String url = config.generateAuthURL(username, serverId, ip);
        return call0(config, url);
    }


    private GameProfile call0(BaseYggdrasilServiceConfig config, String url) throws IOException {
        String response = HttpClientHelper.getString(url, config.getTimeout(), config.getRetry(), config.getRetryDelay());
        return core.getGson().fromJson(response, GameProfile.class);
    }

    public boolean run(HasJoinedContext hasJoinedContext) {
        try {
            GameProfile call = call();
            if (call != null && call.getId() != null) {
                hasJoinedContext.getResponse().set(new Pair<>(call, (config)));
                return true;
            }
            return false;
        } catch (Throwable e) {
            hasJoinedContext.getServiceUnavailable().put(config, e);
            return false;
        }
    }
}
