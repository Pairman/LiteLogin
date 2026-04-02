package org.eu.pnxlr.git.litelogin.api.internal.plugin;

import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared server thread scheduler.
 */
@ApiStatus.Internal
public class BaseScheduler {
    private final AtomicInteger asyncThreadId = new AtomicInteger(0);
    private final ScheduledExecutorService asyncExecutor = Executors.newScheduledThreadPool(5,
            r -> new Thread(r, "LiteLogin Async #" + asyncThreadId.incrementAndGet()));

    /**
     * Executes an asynchronous task.
     *
     * @param runnable task instance
     */
    public void runTaskAsync(Runnable runnable) {
        asyncExecutor.execute(runnable);
    }

    /**
     * Executes an asynchronous task.
     *
     * @param runnable task instance
     * @param delay    delay
     */
    public void runTaskAsync(Runnable runnable, long delay) {
        asyncExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Executes a repeating asynchronous task.
     *
     * @param run    task
     * @param delay  delay
     * @param period period
     */
    public void runTaskAsyncTimer(Runnable run, long delay, long period) {
        asyncExecutor.scheduleAtFixedRate(run, delay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Shuts down the thread pool.
     */
    public synchronized void shutdown() {
        if (asyncExecutor.isShutdown()) return;
        asyncExecutor.shutdown();
    }
}
