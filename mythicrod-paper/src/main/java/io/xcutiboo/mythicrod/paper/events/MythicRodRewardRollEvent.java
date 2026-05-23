package io.xcutiboo.mythicrod.paper.events;

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.xcutiboo.mythicrod.api.platform.PlatformDrop;
import io.xcutiboo.mythicrod.drops.CustomDrop;

/// Fired when MythicRod calculates which reward a player will receive.
///
/// External plugins can use this event to:
///
/// - inject temporary luck multipliers that shift weights
/// - force a specific configured drop with `forceDrop(CustomDrop)`
/// - observe the weight roll for analytics or statistics
///
/// Unlike `MythicRodFishCatchEvent`, this event fires before the item is built,
/// giving access to the raw weight table.
///
/// `forceDrop(CustomDrop)` is an advanced Paper-specific override because it
/// requires a concrete MythicRod drop descriptor. For most integrations,
/// adjusting `setLuckMultiplier(double)` or replacing the final item in
/// `MythicRodFishCatchEvent` is the simpler option.
///
/// The `category` field describes the logical roll context supplied by the
/// fishing pipeline. In the current Paper implementation this is usually the
/// biome key used for drop filtering.
///
/// `baseWeight` is the pre-luck cumulative weight across all eligible built-in
/// drops and registered external drop providers.
///
/// ## Thread Context
///
/// Fired from the same player-owned execution path as the fishing event. On
/// ordinary Paper this is the synchronous event thread. On Folia this is the
/// owning region thread.
public final class MythicRodRewardRollEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final @NotNull Player player;
    private final @NotNull String category;
    private final double baseWeight;
    private double luckMultiplier = 1.0;
    private @Nullable CustomDrop forcedDrop = null;

    /// @param player fishing player
    /// @param category logical roll context, typically the biome key being evaluated
    /// @param baseWeight total pre-luck cumulative weight across all eligible rewards
    public MythicRodRewardRollEvent(
            @NotNull Player player,
            @NotNull String category,
            double baseWeight) {
        super(false);
        this.player = Objects.requireNonNull(player, "player");
        this.category = Objects.requireNonNull(category, "category");
        this.baseWeight = baseWeight;
    }

    /// Returns the Bukkit player whose catch is being rolled.
    ///
    /// The event is already running on MythicRod's player-owned execution path.
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /// Returns the logical roll context being evaluated.
    ///
    /// Current Paper builds use the biome category key for this value.
    @NotNull
    public String getCategory() {
        return category;
    }

    /// Returns the total cumulative weight before any luck multiplier.
    public double getBaseWeight() {
        return baseWeight;
    }

    /// Returns the luck multiplier applied to this player's roll.
    ///
    /// Defaults to `1.0` (no effect). Values above `1.0` increase the effective
    /// weight of rarer drops.
    ///
    /// @return current luck multiplier
    public double getLuckMultiplier() {
        return luckMultiplier;
    }

    /// Sets a luck multiplier for this roll.
    ///
    /// MythicRod applies the multiplier to rare and legendary reward weights
    /// before the final roll, so values above `1.0` make those rewards relatively
    /// more likely without changing common reward weights.
    ///
    /// @param multiplier must be `> 0`; clamped to a minimum of `0.01`
    public void setLuckMultiplier(double multiplier) {
        this.luckMultiplier = Math.max(0.01, multiplier);
    }

    /// Forces a specific drop, bypassing weighted random selection.
    ///
    /// MythicRod will use this drop if it is non-null after the event completes.
    ///
    /// @param drop drop to force, or `null` to clear any forced drop
    public void forceDrop(@Nullable CustomDrop drop) {
        this.forcedDrop = drop;
    }

    /// @return forced drop set by an external plugin, or `null` when normal selection should proceed
    @Nullable
    public CustomDrop getForcedDrop() {
        return forcedDrop;
    }

    /// Returns the forced drop through MythicRod's stable drop view.
    ///
    /// @return forced drop as `PlatformDrop`, or `null` when no override is set
    @Nullable
    @SuppressWarnings("java:S4144")
    public PlatformDrop getForcedDropView() {
        return forcedDrop;
    }

    /// @return `true` if an external plugin has forced a specific drop
    public boolean hasForcedDrop() {
        return forcedDrop != null;
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
