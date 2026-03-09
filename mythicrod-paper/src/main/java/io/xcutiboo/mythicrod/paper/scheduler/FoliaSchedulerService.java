package io.xcutiboo.mythicrod.paper.scheduler;

import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.google.inject.Inject;

import java.util.concurrent.TimeUnit;

public class FoliaSchedulerService implements PlatformScheduler {

    private final Plugin plugin;
    private final boolean isFolia;

    @Inject
    public FoliaSchedulerService(Plugin plugin) {
        this.plugin = plugin;
        this.isFolia = isFoliaEnabled();
    }
    
    private boolean isFoliaEnabled() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void runAsync(Runnable task) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    @Override
    public void runAsyncLater(Runnable task, long delayTicks) {
        if (isFolia) {
            long delayMillis = delayTicks * 50;
            Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayMillis, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    @Override
    public void runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isFolia) {
            long delayMillis = delayTicks * 50;
            long periodMillis = periodTicks * 50;
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delayMillis, periodMillis, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    @Override
    public void runGlobal(Runnable task) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
