package io.xcutiboo.mythicrod.spigot.scheduler;

import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.google.inject.Inject;

public class BukkitSchedulerService implements PlatformScheduler {

    private final Plugin plugin;

    @Inject
    public BukkitSchedulerService(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runAsyncLater(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    @Override
    public void runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    @Override
    public void runGlobal(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
