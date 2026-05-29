package io.xcutiboo.mythicrod.paper.scheduler;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.xcutiboo.mythicrod.api.platform.PlatformLocation;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformTask;
import io.xcutiboo.mythicrod.paper.platform.PaperLocation;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.paper.platform.PaperTask;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FoliaSchedulerService implements PlatformScheduler {
    private final Plugin plugin;
    private final boolean isFolia;
    private final Set<PaperTask> trackedTasks = ConcurrentHashMap.newKeySet();

    public FoliaSchedulerService(Plugin plugin) {
        this.plugin = plugin;
        this.isFolia = detectFolia();
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException _) {
            return false;
        }
    }

    public boolean isFoliaRuntime() {
        return isFolia;
    }

    public void cancelPluginTasks() {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
            Bukkit.getAsyncScheduler().cancelTasks(plugin);
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }

        for (PaperTask task : List.copyOf(trackedTasks)) {
            try {
                task.cancel();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING,
                    "Failed to cancel a scheduled MythicRod task", e);
            }
        }
        trackedTasks.clear();
    }


    @Override
    public void runAtLocation(PlatformLocation location, Runnable task) {
        Location bukkitLoc = PaperLocation.toBukkit(location, Bukkit.getServer());
        if (bukkitLoc == null) {
            plugin.getLogger().warning("Skipping location task because the target world is unavailable");
            return;
        }

        if (isFolia) {
            Bukkit.getRegionScheduler().execute(plugin, bukkitLoc, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public PlatformTask runAtLocationDelayed(PlatformLocation location, Runnable task, long delayTicks) {
        Location bukkitLoc = PaperLocation.toBukkit(location, Bukkit.getServer());
        if (bukkitLoc == null) {
            plugin.getLogger().warning("Skipping delayed location task because the target world is unavailable");
            return new PaperTask(null);
        }

        AtomicReference<PaperTask> taskRef = new AtomicReference<>();
        if (isFolia) {
            var foliaTask = Bukkit.getRegionScheduler().runDelayed(
                plugin,
                bukkitLoc,
                foliaOneShot(taskRef, task),
                delayTicks
            );
            return track(foliaTask, taskRef);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> runOneShot(taskRef, task),
                delayTicks
            );
            return track(bukkitTask, taskRef);
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

        AtomicReference<PaperTask> taskRef = new AtomicReference<>();
        if (isFolia) {
            var foliaTask = bukkitPlayer.getScheduler().runDelayed(
                plugin,
                foliaOneShot(taskRef, task),
                null,
                delayTicks
            );
            return track(foliaTask, taskRef);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> runOneShot(taskRef, task),
                delayTicks
            );
            return track(bukkitTask, taskRef);
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
        AtomicReference<PaperTask> taskRef = new AtomicReference<>();
        if (isFolia) {
            var foliaTask = Bukkit.getGlobalRegionScheduler().runDelayed(
                plugin,
                foliaOneShot(taskRef, task),
                delayTicks
            );
            return track(foliaTask, taskRef);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> runOneShot(taskRef, task),
                delayTicks
            );
            return track(bukkitTask, taskRef);
        }
    }


    @Override
    public void runAsync(Runnable task) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, foliaTask(task));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    @Override
    public PlatformTask runAsyncDelayed(Runnable task, long delayTicks) {
        AtomicReference<PaperTask> taskRef = new AtomicReference<>();
        if (isFolia) {
            long delayMillis = delayTicks * 50; // Convert ticks to milliseconds
            var foliaTask = Bukkit.getAsyncScheduler().runDelayed(
                plugin,
                foliaOneShot(taskRef, task),
                delayMillis,
                TimeUnit.MILLISECONDS
            );
            return track(foliaTask, taskRef);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(
                plugin,
                () -> runOneShot(taskRef, task),
                delayTicks
            );
            return track(bukkitTask, taskRef);
        }
    }


    @Override
    public PlatformTask runGlobalRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        if (isFolia) {
            var foliaTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                foliaTask(task),
                initialDelayTicks,
                periodTicks
            );
            return track(foliaTask);
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
            return track(bukkitTask);
        }
    }

    @Override
    public PlatformTask runAsyncRepeating(Runnable task, long initialDelayMillis, long periodMillis) {
        if (isFolia) {
            var foliaTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                foliaTask(task),
                initialDelayMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
            );
            return track(foliaTask);
        } else {
            long delayTicks = initialDelayMillis / 50L;
            long periodTicks = periodMillis / 50L;
            var bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                task,
                delayTicks,
                periodTicks
            );
            return track(bukkitTask);
        }
    }

    private PaperTask track(Object nativeTask) {
        AtomicReference<PaperTask> taskRef = new AtomicReference<>();
        return track(nativeTask, taskRef);
    }

    private PaperTask track(Object nativeTask, AtomicReference<PaperTask> taskRef) {
        if (nativeTask == null) {
            return new PaperTask(null);
        }

        PaperTask platformTask = new PaperTask(nativeTask, () -> {
            PaperTask task = taskRef.get();
            if (task != null) {
                trackedTasks.remove(task);
            }
        });
        taskRef.set(platformTask);
        trackedTasks.add(platformTask);
        return platformTask;
    }

    private Consumer<ScheduledTask> foliaTask(Runnable task) {
        return scheduledTask -> task.run();
    }

    private Consumer<ScheduledTask> foliaOneShot(AtomicReference<PaperTask> taskRef, Runnable task) {
        return scheduledTask -> runOneShot(taskRef, task);
    }

    private void runOneShot(AtomicReference<PaperTask> taskRef, Runnable task) {
        try {
            task.run();
        } finally {
            PaperTask platformTask = taskRef.get();
            if (platformTask != null) {
                trackedTasks.remove(platformTask);
            }
        }
    }
}
