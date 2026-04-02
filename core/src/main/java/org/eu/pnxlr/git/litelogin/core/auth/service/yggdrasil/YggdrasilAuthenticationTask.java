package org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil;

import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig;
import org.eu.pnxlr.git.litelogin.core.main.Core;
import org.eu.pnxlr.git.litelogin.core.ohc.LoggingInterceptor;
import org.eu.pnxlr.git.litelogin.core.ohc.RetryInterceptor;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

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
        return call0(config, new Request.Builder()
                .get()
                .url(url)
                .header("User-Agent", LiteLoginConstants.HTTP_USER_AGENT)
                .build());
    }


    private GameProfile call0(BaseYggdrasilServiceConfig config, Request request) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor(config.getRetry(), config.getRetryDelay()))
                .addInterceptor(new LoggingInterceptor())
                .writeTimeout(Duration.ofMillis(config.getTimeout()))
                .readTimeout(Duration.ofMillis(config.getTimeout()))
                .connectTimeout(Duration.ofMillis(config.getTimeout()))
                .build();
        Call call = client.newCall(request);
        try (Response execute = call.execute()) {
            return core.getGson().fromJson(Objects.requireNonNull(execute.body()).string(), GameProfile.class);
        }
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
