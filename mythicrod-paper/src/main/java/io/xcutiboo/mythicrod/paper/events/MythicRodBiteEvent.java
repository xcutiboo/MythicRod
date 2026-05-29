package io.xcutiboo.mythicrod.paper.events;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/// Fires when a fish bites a MythicRod hook, before the catch resolves.
///
/// Mirrors the vanilla `PlayerFishEvent` with state `BITE`, but only for
/// rods MythicRod recognises. External plugins use this to trigger a
/// skill-check minigame, play a custom bite cue, or short-circuit the
/// catch when the player isn't holding the right item.
///
/// Cancelling this event cancels the underlying `PlayerFishEvent`, so the
/// fish drops the hook and the catch never resolves. The vanilla bobber
/// animation already played by the time the event fires; cancelling does
/// not roll that back.
///
/// ## Thread contract
///
/// Fired on the same thread that delivered the underlying
/// `PlayerFishEvent` — main on Paper, the player's region thread on
/// Folia. Treat it as a normal Bukkit event handler thread.
@ApiStatus.AvailableSince("2026.1.0")
public final class MythicRodBiteEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final FishHook hook;
    private final PlayerFishEvent source;
    private boolean cancelled;

    public MythicRodBiteEvent(@NotNull Player player,
                              @NotNull FishHook hook,
                              @NotNull PlayerFishEvent source) {
        this.player = player;
        this.hook = hook;
        this.source = source;
    }

    /// The player whose rod the fish bit.
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /// The fishing hook entity. Use this to inspect the hook location, the
    /// hooked entity (if any), or to apply visual cues at the bobber.
    @NotNull
    public FishHook getHook() {
        return hook;
    }

    /// The underlying Bukkit event. Use this only when you need fields the
    /// MythicRod event does not expose. Calling `setCancelled(true)` on the
    /// source has the same effect as cancelling this event.
    @NotNull
    public PlayerFishEvent getSourceEvent() {
        return source;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
        source.setCancelled(cancel);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
