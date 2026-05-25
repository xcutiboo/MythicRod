package io.xcutiboo.mythicrod.paper.platform;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.xcutiboo.mythicrod.api.platform.PlatformTask;
import org.bukkit.scheduler.BukkitTask;

public class PaperTask implements PlatformTask {
    private final Object nativeTask;
    private final Runnable onCancel;

    public PaperTask(Object nativeTask) {
        this(nativeTask, () -> {
        });
    }

    public PaperTask(Object nativeTask, Runnable onCancel) {
        this.nativeTask = nativeTask;
        this.onCancel = onCancel;
    }

    @Override
    public void cancel() {
        if (nativeTask instanceof ScheduledTask foliaTask) {
            foliaTask.cancel();
        } else if (nativeTask instanceof BukkitTask bukkitTask) {
            bukkitTask.cancel();
        }
        onCancel.run();
    }

    @Override
    public boolean isCancelled() {
        if (nativeTask instanceof ScheduledTask foliaTask) {
            return foliaTask.isCancelled();
        } else if (nativeTask instanceof BukkitTask bukkitTask) {
            return bukkitTask.isCancelled();
        }
        return false;
    }
}
