package io.xcutiboo.mythicrod.paper.events;

import java.util.Objects;
import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import io.xcutiboo.mythicrod.api.PlayerStatSnapshot;

/**
 * Fired after MythicRod records a catch against a player's stats. The
 * accompanying snapshot reflects the post-increment state of the player's
 * stored stats.
 *
 * <p>The event is read-only. External plugins may use it to mirror catches to
 * their own scoreboards, quest progress, or analytics pipelines without poking
 * MythicRod's mutable storage.
 *
 * <p>The event fires on MythicRod's player-owned execution path: the regular
 * event thread on ordinary Paper and the player's region thread on Folia.
 * Asynchronous work scheduled from a handler must be re-dispatched to the
 * correct owner before touching world or inventory state.
 */
public final class MythicRodStatsUpdateEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final @NotNull UUID playerId;
    private final @NotNull String tier;
    private final @NotNull PlayerStatSnapshot snapshot;

    public MythicRodStatsUpdateEvent(
            @NotNull UUID playerId,
            @NotNull String tier,
            @NotNull PlayerStatSnapshot snapshot) {
        super(false);
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.tier = Objects.requireNonNull(tier, "tier");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    /** Player whose stats were updated. */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /** Rarity tier that was incremented: one of {@code common}, {@code uncommon}, {@code rare}, {@code legendary}. */
    @NotNull
    public String getTier() {
        return tier;
    }

    /** Immutable snapshot of the player's stats after the increment. */
    @NotNull
    public PlayerStatSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    @NotNull
    @SuppressWarnings("java:S4144")
    // Bukkit requires both the instance and static accessor to expose the same HandlerList.
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    @SuppressWarnings("java:S4144")
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
