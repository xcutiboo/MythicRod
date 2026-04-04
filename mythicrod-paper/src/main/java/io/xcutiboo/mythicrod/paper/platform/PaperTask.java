package io.xcutiboo.mythicrod.paper.platform;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.xcutiboo.mythicrod.api.platform.PlatformTask;
import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitTask;

@RequiredArgsConstructor
public class PaperTask implements PlatformTask {
    private final Object nativeTask; // Either ScheduledTask (Folia) or BukkitTask (Bukkit)
    
    @Override
    public void cancel() {
        if (nativeTask instanceof ScheduledTask foliaTask) {
            foliaTask.cancel();
        } else if (nativeTask instanceof BukkitTask bukkitTask) {
            bukkitTask.cancel();
        }
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
