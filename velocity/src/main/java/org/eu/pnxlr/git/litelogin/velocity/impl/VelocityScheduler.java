package org.eu.pnxlr.git.litelogin.velocity.impl;

import org.eu.pnxlr.git.litelogin.api.internal.plugin.BaseScheduler;

/**
 * Velocity 调度器对象
 */
public class VelocityScheduler extends BaseScheduler {
    @Override
    public void runTask(Runnable run, long delay) {
        runTaskAsync(run, delay);
    }
}
