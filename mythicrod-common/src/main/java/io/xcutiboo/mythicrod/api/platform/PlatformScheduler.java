package io.xcutiboo.mythicrod.api.platform;

/**
 * Platform-agnostic scheduler abstraction.
 * Used to avoid BukkitScheduler or FoliaScheduler dependencies in common.
 */
public interface PlatformScheduler {
    
    /**
     * Run a task asynchronously.
     */
    void runAsync(Runnable task);
    
    /**
     * Run a task asynchronously after a delay.
     * @param delayTicks Delay in ticks (20 ticks = 1 second)
     */
    void runAsyncLater(Runnable task, long delayTicks);
    
    /**
     * Run a repeating task asynchronously.
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     */
    void runAsyncTimer(Runnable task, long delayTicks, long periodTicks);
    
    /**
     * Schedule a task on the global region (Folia) or main thread (Spigot).
     * Must be used for any operations that modify global world state.
     */
    void runGlobal(Runnable task);
}