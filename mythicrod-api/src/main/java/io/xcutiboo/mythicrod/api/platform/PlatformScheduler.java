package io.xcutiboo.mythicrod.api.platform;

/// Scheduler facade for Paper/Folia-safe handoffs.
///
/// Tasks that touch players, inventories, worlds, entities, chunks, or other
/// platform state must use the owner-specific method:
///
/// - player work through `runForPlayer(PlatformPlayer, Runnable)`
/// - location work through `runAtLocation(PlatformLocation, Runnable)`
/// - server-global bookkeeping through `runGlobal(Runnable)`
/// - blocking I/O or pure computation through `runAsync(Runnable)`
///
/// Cancel retained delayed or repeating tasks during owner shutdown.
public interface PlatformScheduler {

    /// Runs work on the owner of a location or region.
    ///
    /// @param location location snapshot used to resolve the owning scheduler
    /// @param task work to run on that owner
    void runAtLocation(PlatformLocation location, Runnable task);

    /// Runs location-owned work after a delay.
    ///
    /// @param location location snapshot used to resolve the owning scheduler
    /// @param task work to run
    /// @param delayTicks delay in server ticks
    /// @return cancellable task handle for the scheduled work
    PlatformTask runAtLocationDelayed(PlatformLocation location, Runnable task, long delayTicks);

    /// Runs work on the scheduler that owns the supplied player.
    ///
    /// @param player player whose entity scheduler owns the work
    /// @param task work to run
    void runForPlayer(PlatformPlayer player, Runnable task);

    /// Runs player-owned work after a delay.
    ///
    /// @param player player whose entity scheduler owns the work
    /// @param task work to run
    /// @param delayTicks delay in server ticks
    /// @return cancellable task handle for the scheduled work
    PlatformTask runForPlayerDelayed(PlatformPlayer player, Runnable task, long delayTicks);

    /// Runs work on the global scheduler.
    ///
    /// Use this for server-global bookkeeping, not entity or world mutation.
    ///
    /// @param task work to run
    void runGlobal(Runnable task);

    /// Runs global-scheduler work after a delay.
    ///
    /// @param task work to run
    /// @param delayTicks delay in server ticks
    /// @return cancellable task handle for the scheduled work
    PlatformTask runGlobalDelayed(Runnable task, long delayTicks);

    /// Runs work away from server-owned tick/region threads.
    ///
    /// Schedule back to the correct owner before touching platform state from
    /// async callbacks.
    ///
    /// @param task work to run asynchronously
    void runAsync(Runnable task);

    /// Runs async work after a delay.
    ///
    /// @param task work to run asynchronously
    /// @param delayTicks delay in server ticks
    /// @return cancellable task handle for the scheduled work
    PlatformTask runAsyncDelayed(Runnable task, long delayTicks);

    /// Runs repeating global-scheduler work.
    ///
    /// @param task work to run
    /// @param initialDelayTicks initial delay in server ticks
    /// @param periodTicks repeat period in server ticks
    /// @return cancellable task handle for the repeating work
    PlatformTask runGlobalRepeating(Runnable task, long initialDelayTicks, long periodTicks);

    /// Runs repeating async work.
    ///
    /// Delay and period are expressed in milliseconds because Folia's async
    /// scheduler is time-based.
    ///
    /// @param task work to run asynchronously
    /// @param initialDelayMillis initial delay in milliseconds
    /// @param periodMillis repeat period in milliseconds
    /// @return cancellable task handle for the repeating work
    PlatformTask runAsyncRepeating(Runnable task, long initialDelayMillis, long periodMillis);
}
