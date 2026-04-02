package org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil;

import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.configuration.BaseServiceConfig;
import org.eu.pnxlr.git.litelogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Central HasJoined handler.
 */
public class YggdrasilAuthenticationService {
    private static final AtomicInteger WORKER_ID = new AtomicInteger();
    private static final ExecutorService AUTH_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "LiteLogin Auth #" + WORKER_ID.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    private final Core core;

    public YggdrasilAuthenticationService(Core core) {
        this.core = core;
    }

    /**
     * Starts authentication.
     */
    public YggdrasilAuthenticationResult hasJoined(String username, String serverId, String ip) throws SQLException {
        final Set<Integer> ids = core.getPluginConfig().getServiceIdMap().entrySet().stream()
                .filter(e -> e.getValue() instanceof BaseYggdrasilServiceConfig)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        if (ids.size() == 0) return YggdrasilAuthenticationResult.ofNoService();


        // Primary authentication service ID set
        // Verified first during HasJoined
        Set<Integer> primaries = new HashSet<>();

        // If only one authentication service exists, use it as primary directly.
        // Otherwise, read the database and pick the most recent service as primary.
        if (ids.size() == 1) {
            primaries.add(ids.iterator().next());
        } else {
            // First, get the in-game UUID stored in the database
            UUID inGameUUID = core.getSqlManager().getInGameProfileTable().getInGameUUIDIgnoreCase(username);

            // If an in-game UUID is found, then retrieve the Yggdrasil service IDs
            if (inGameUUID != null) {
                // There may be more than one
                primaries.addAll(core.getSqlManager().getUserDataTable().getOnlineServiceIds(inGameUUID));
            }
        }

        // Secondary authentication service ID set
        // Verified last during HasJoined
        Set<Integer> secondaries = ids.stream().filter(i -> !primaries.contains(i)).collect(Collectors.toSet());

        LoggerProvider.getLogger().debug(String.format(
                "%s's hasJoined verification order: [%s], [%s]", username,
                ValueUtil.join(", ", ", ", primaries),
                ValueUtil.join(", ", ", ", secondaries)
        ));

        boolean serverBreakdown = false;
        if (primaries.size() != 0) {
            YggdrasilAuthenticationResult result = hasJoined0(username, serverId, ip, primaries);
            if (result.getReason() == YggdrasilAuthenticationResult.Reason.ALLOWED) return result;
            if (result.getReason() == YggdrasilAuthenticationResult.Reason.SERVER_BREAKDOWN) {
                serverBreakdown = true;
            }
        }
        if (secondaries.size() != 0) {
            YggdrasilAuthenticationResult result = hasJoined0(username, serverId, ip, secondaries);
            if (result.getReason() == YggdrasilAuthenticationResult.Reason.ALLOWED) return result;
            if (result.getReason() == YggdrasilAuthenticationResult.Reason.SERVER_BREAKDOWN) {
                serverBreakdown = true;
            }
        }
        if (serverBreakdown) return YggdrasilAuthenticationResult.ofServerBreakdown();
        return YggdrasilAuthenticationResult.ofValidationFailed();
    }

    private YggdrasilAuthenticationResult hasJoined0(String username, String serverId, String ip, Set<Integer> ids) {
        Set<BaseYggdrasilServiceConfig> serviceConfigs = new HashSet<>();
        for (Integer id : ids) {
            BaseServiceConfig config = core.getPluginConfig().getServiceIdMap().get(id);
            if (config instanceof BaseYggdrasilServiceConfig) {
                serviceConfigs.add((BaseYggdrasilServiceConfig) config);
            }
        }

        final HasJoinedContext context = new HasJoinedContext(username, serverId, ip);
        CompletionService<Boolean> completionService = new ExecutorCompletionService<>(AUTH_EXECUTOR);
        int taskCount = 0;
        for (BaseYggdrasilServiceConfig serviceConfig : serviceConfigs) {
            YggdrasilAuthenticationTask authenticationTask = new YggdrasilAuthenticationTask(core, username, serverId, ip, serviceConfig);
            completionService.submit(() -> authenticationTask.run(context));
            taskCount++;
        }

        boolean passed = false;
        for (int i = 0; i < taskCount; i++) {
            try {
                Future<Boolean> future = completionService.take();
                if (Boolean.TRUE.equals(future.get())) {
                    passed = true;
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                LoggerProvider.getLogger().debug("An authentication worker failed unexpectedly.", e.getCause());
            }
        }

        if (passed && context.getResponse().get() != null) {
            return YggdrasilAuthenticationResult.ofAllowed(
                    context.getResponse().get().getValue1(),
                    context.getResponse().get().getValue2()
            );
        }
        if (context.getServiceUnavailable().size() != 0) {
            for (Map.Entry<BaseYggdrasilServiceConfig, Throwable> entry : context.getServiceUnavailable().entrySet()) {
                LoggerProvider.getLogger().debug("An exception occurred during authentication of the yggdrasil service whose ID is " + entry.getKey().getId(), entry.getValue());
            }
            return YggdrasilAuthenticationResult.ofServerBreakdown();
        }
        return YggdrasilAuthenticationResult.ofValidationFailed();
    }
}
