package org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil;

import lombok.Data;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.internal.util.tuple.Pair;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HasJoined authentication context.
 */
@Data
public class HasJoinedContext {
    private final String username;
    private final String serverId;
    private final String ip;

    // Successful response marker
    private final AtomicReference<Pair<GameProfile, BaseYggdrasilServiceConfig>> response = new AtomicReference<>();

    // Exceptions captured during authentication
    private final Map<BaseYggdrasilServiceConfig, Throwable> serviceUnavailable = new ConcurrentHashMap<>();

    protected HasJoinedContext(String username, String serverId, String ip) {
        this.username = username;
        this.serverId = serverId;
        this.ip = ip;
    }
}
