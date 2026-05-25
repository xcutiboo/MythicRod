package io.xcutiboo.mythicrod.paper.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/// Fires once MythicRod has finished an atomic configuration and drop-table
/// reload. Listeners receive the post-reload state, so it is safe to query
/// [io.xcutiboo.mythicrod.api.MythicRodAPI#getDropCatalog()] and any of the
/// live config flags from inside the handler.
///
/// ## Thread contract
///
/// Fired on the same thread that invoked
/// [io.xcutiboo.mythicrod.paper.MythicRod#reload()]. For console-issued
/// `/mythicrod reload` on Paper this is the main thread; on Folia the
/// command originates on the global region scheduler.
///
/// The event is not cancellable. Reloads always complete before the event
/// fires, so cancelling would have no meaningful effect.
@ApiStatus.AvailableSince("2026.1.0")
public final class MythicRodReloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean success;

    public MythicRodReloadEvent(boolean success) {
        this.success = success;
    }

    /// Returns whether the reload completed without exception.
    ///
    /// A failed reload leaves the previous drop table and config in place,
    /// but downstream caches may still want to refresh because language
    /// files or stats writers might have been reset before the failure.
    public boolean isSuccess() {
        return success;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
