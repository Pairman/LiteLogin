package org.eu.pnxlr.git.litelogin.api.internal.util;

import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared asynchronous scheduler.
 */
@ApiStatus.Internal
public class AsyncScheduler {
    private final AtomicInteger asyncThreadId = new AtomicInteger(0);
    private final ScheduledExecutorService asyncExecutor = Executors.newScheduledThreadPool(5,
            runnable -> new Thread(runnable, "LiteLogin Async #" + asyncThreadId.incrementAndGet()));

    public void runTaskAsync(Runnable runnable) {
        asyncExecutor.execute(runnable);
    }

    public void runTaskAsync(Runnable runnable, long delay) {
        asyncExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

    public void runTaskAsyncTimer(Runnable runnable, long delay, long period) {
        asyncExecutor.scheduleAtFixedRate(runnable, delay, period, TimeUnit.MILLISECONDS);
    }

    public synchronized void shutdown() {
        if (asyncExecutor.isShutdown()) {
            return;
        }
        asyncExecutor.shutdown();
    }
}
