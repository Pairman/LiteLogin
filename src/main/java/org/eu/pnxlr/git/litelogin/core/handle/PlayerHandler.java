package org.eu.pnxlr.git.litelogin.core.handle;

import com.velocitypowered.api.proxy.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.data.LiteLoginPlayerData;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.internal.result.HandleResult;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.tuple.Pair;
import org.eu.pnxlr.git.litelogin.api.service.IService;
import org.eu.pnxlr.git.litelogin.core.configuration.BaseServiceConfig;
import org.eu.pnxlr.git.litelogin.core.main.Core;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Data cache center.
 */
public class PlayerHandler {

    private final Core core;

    // inGameUUID \ Entry
    private final Map<UUID, Entry> cache;

    // inGameUUID \ Entry
    // Login cache
    @Getter
    private final Map<UUID, Entry> loginCache;

    public PlayerHandler(Core core) {
        this.core = core;
        this.cache = new ConcurrentHashMap<>();
        this.loginCache = new ConcurrentHashMap<>();
    }

    public HandleResult pushPlayerQuitGame(UUID inGameUUID, String username) {
        return new HandleResult(HandleResult.Type.NONE, null);
    }

    public HandleResult pushPlayerJoinGame(UUID inGameUUID, String username) {
        Entry remove = loginCache.remove(inGameUUID);
        if (remove == null) {
            return new HandleResult(HandleResult.Type.KICK, "You must authenticate through LiteLogin to join this server.");
        } else {
            long l = System.currentTimeMillis() - remove.signTimeMillis;
            if (l > 5 * 1000) {
                LoggerProvider.getLogger().warn(String.format(
                        "Players with in game UUID %s and name %s are taking too long to log in after verification, reached %d milliseconds. Is it the same person?",
                        inGameUUID.toString(), username, l
                ));
            }
            cache.put(inGameUUID, remove);
        }

        return new HandleResult(HandleResult.Type.NONE, null);
    }

    public LiteLoginPlayerData getPlayerData(UUID inGameUUID){
        return cache.get(inGameUUID);
    }

    public Pair<GameProfile, Integer> getPlayerOnlineProfile(UUID inGameUUID) {
        Entry entry = cache.get(inGameUUID);
        if (entry == null) return null;
        return new Pair<>(entry.onlineProfile, entry.serviceConfig.getId());
    }

    public UUID getInGameUUID(UUID onlineUUID, int serviceId) {
        for (Map.Entry<UUID, Entry> entry : cache.entrySet()) {
            if (entry.getValue().onlineProfile.getId().equals(onlineUUID)
                    && entry.getValue().serviceConfig.getId() == serviceId)
                return entry.getKey();
        }
        return null;
    }

    public String getServiceName(int serviceId) {
        BaseServiceConfig config = core.getPluginConfig().getServiceIdMap().get(serviceId);
        if (config == null) return null;
        return config.getName();
    }

    public void register() {
        core.getScheduler().runTaskAsyncTimer(() -> {
            // Store all online players
            Set<UUID> onlinePlayerUUIDs = core.getProxyServer().getAllPlayers().stream()
                    .map(Player::getUniqueId).collect(Collectors.toSet());

            // Traverse the current cache and collect invalid entries
            Set<Map.Entry<UUID, Entry>> noExists = cache.entrySet().stream().filter(e -> !onlinePlayerUUIDs.contains(e.getKey())).collect(Collectors.toSet());

            try {
                Thread.sleep(1000 * 10);
            } catch (InterruptedException e) {
                LoggerProvider.getLogger().error("An exception occurred on the delayed cache clearing.", e);
            }

            // Remove invalid data
            for (Map.Entry<UUID, Entry> e : noExists) {
                Entry entry = cache.get(e.getKey());

                // Data has already been removed
                if (entry == null) continue;

                // Data changed before removal
                if (!e.getValue().equals(entry)) continue;

                // Remove the entry
                cache.remove(e.getKey());
            }

        }, 0, 1000 * 60);
    }

    @AllArgsConstructor
    public static class Entry implements LiteLoginPlayerData {
        private final GameProfile onlineProfile;
        private final BaseServiceConfig serviceConfig;
        private final long signTimeMillis;

        @NotNull
        @Override
        public GameProfile getOnlineProfile() {
            return onlineProfile;
        }

        @NotNull
        @Override
        public IService getLoginService() {
            return serviceConfig;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return Objects.equals(serviceConfig, entry.serviceConfig) && signTimeMillis == entry.signTimeMillis && Objects.equals(onlineProfile, entry.onlineProfile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(onlineProfile, serviceConfig, signTimeMillis);
        }
    }
}
