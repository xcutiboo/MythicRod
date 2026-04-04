package io.xcutiboo.mythicrod.paper.scheduler;

import io.xcutiboo.mythicrod.api.platform.PlatformLocation;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformTask;
import io.xcutiboo.mythicrod.paper.platform.PaperLocation;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.paper.platform.PaperTask;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class FoliaSchedulerService implements PlatformScheduler {
    private final Plugin plugin;
    private final boolean isFolia;

    public FoliaSchedulerService(Plugin plugin) {
        this.plugin = plugin;
        this.isFolia = detectFolia();
    }
    
    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }


    @Override
    public void runAtLocation(PlatformLocation location, Runnable task) {
        Location bukkitLoc = PaperLocation.toBukkit(location, Bukkit.getServer());
        
        if (isFolia) {
            Bukkit.getRegionScheduler().execute(plugin, bukkitLoc, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public PlatformTask runAtLocationDelayed(PlatformLocation location, Runnable task, long delayTicks) {
        Location bukkitLoc = PaperLocation.toBukkit(location, Bukkit.getServer());
        
        if (isFolia) {
            var foliaTask = Bukkit.getRegionScheduler().runDelayed(
                plugin, 
                bukkitLoc, 
                scheduledTask -> task.run(), 
                delayTicks
            );
            return new PaperTask(foliaTask);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return new PaperTask(bukkitTask);
        }
    }


    @Override
    public void runForPlayer(PlatformPlayer player, Runnable task) {
        Player bukkitPlayer = ((PaperPlayer) player).getBukkitPlayer();
        
        if (isFolia) {
            bukkitPlayer.getScheduler().execute(plugin, task, null, 1L);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public PlatformTask runForPlayerDelayed(PlatformPlayer player, Runnable task, long delayTicks) {
        Player bukkitPlayer = ((PaperPlayer) player).getBukkitPlayer();
        
        if (isFolia) {
            var foliaTask = bukkitPlayer.getScheduler().runDelayed(
                plugin, 
                scheduledTask -> task.run(), 
                null, 
                delayTicks
            );
            return new PaperTask(foliaTask);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return new PaperTask(bukkitTask);
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

    @Override
    public PlatformTask runGlobalDelayed(Runnable task, long delayTicks) {
        if (isFolia) {
            var foliaTask = Bukkit.getGlobalRegionScheduler().runDelayed(
                plugin, 
                scheduledTask -> task.run(), 
                delayTicks
            );
            return new PaperTask(foliaTask);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return new PaperTask(bukkitTask);
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
    public PlatformTask runAsyncDelayed(Runnable task, long delayTicks) {
        if (isFolia) {
            long delayMillis = delayTicks * 50; // Convert ticks to milliseconds
            var foliaTask = Bukkit.getAsyncScheduler().runDelayed(
                plugin, 
                scheduledTask -> task.run(), 
                delayMillis, 
                TimeUnit.MILLISECONDS
            );
            return new PaperTask(foliaTask);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
            return new PaperTask(bukkitTask);
        }
    }
    
    
    @Override
    public PlatformTask runGlobalRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        if (isFolia) {
            var foliaTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin, 
                scheduledTask -> task.run(), 
                initialDelayTicks, 
                periodTicks
            );
            return new PaperTask(foliaTask);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
            return new PaperTask(bukkitTask);
        }
    }
    
    @Override
    public PlatformTask runAsyncRepeating(Runnable task, long initialDelayMillis, long periodMillis) {
        if (isFolia) {
            var foliaTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin, 
                scheduledTask -> task.run(), 
                initialDelayMillis, 
                periodMillis, 
                TimeUnit.MILLISECONDS
            );
            return new PaperTask(foliaTask);
        } else {
            long delayTicks = initialDelayMillis / 50L;
            long periodTicks = periodMillis / 50L;
            var bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, 
                task, 
                delayTicks, 
                periodTicks
            );
            return new PaperTask(bukkitTask);
        }
    }
}
