package io.xcutiboo.mythicrod.paper.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.xcutiboo.mythicrod.drops.CustomDrop;

/**
 * Fired on the player's EntityScheduler region thread when MythicRod calculates
 * which drop a player will receive from the weighted drop table.
 *
 * <p>External plugins can use this event to:
 * <ul>
 *   <li>Inject temporary luck multipliers that shift weights</li>
 *   <li>Force a specific drop by calling {@link #forceDrop(CustomDrop)}</li>
 *   <li>Observe the weight roll for analytics/statistics</li>
 * </ul>
 *
 * <p>Unlike {@link MythicRodFishCatchEvent}, this event fires <em>before</em>
 * the item is built, giving access to the raw weight table.
 *
 * <p><strong>Thread context:</strong> Always fired on the entity's region thread.
 */
public final class MythicRodRewardRollEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final @NotNull Player player;
    private final @NotNull String category;
    private final double baseWeight;
    private double luckMultiplier = 1.0;
    private @Nullable CustomDrop forcedDrop = null;

    /**
     * @param player     The fishing player.
     * @param category   The drop category being rolled (e.g. "legendary", "rare").
     * @param baseWeight The total cumulative weight across all eligible drops.
     */
    public MythicRodRewardRollEvent(
            @NotNull Player player,
            @NotNull String category,
            double baseWeight) {
        super(false); // Not async — entity region thread
        this.player = player;
        this.category = category;
        this.baseWeight = baseWeight;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** @return The player who triggered the roll. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** @return The drop table category being evaluated. */
    @NotNull
    public String getCategory() {
        return category;
    }

    /** @return The total cumulative weight of all eligible drops before any multiplier. */
    public double getBaseWeight() {
        return baseWeight;
    }

    /**
     * @return The luck multiplier applied to this player's roll.
     *         Defaults to {@code 1.0} (no effect). Values {@code > 1.0} increase
     *         the effective chance of rarer drops.
     */
    public double getLuckMultiplier() {
        return luckMultiplier;
    }

    /**
     * Sets a luck multiplier for this roll. The multiplier scales the random
     * roll target: a value of {@code 2.0} halves the effective roll, biasing
     * towards lower-weight (rarer) entries.
     *
     * @param multiplier Must be {@code > 0}. Clamped to a minimum of {@code 0.01}.
     */
    public void setLuckMultiplier(double multiplier) {
        this.luckMultiplier = Math.max(0.01, multiplier);
    }

    /**
     * Forces a specific drop, bypassing the weighted random selection entirely.
     * MythicRod will use this drop if it is non-null after the event completes.
     *
     * @param drop The drop to force, or {@code null} to clear any forced drop.
     */
    public void forceDrop(@Nullable CustomDrop drop) {
        this.forcedDrop = drop;
    }

    /**
     * @return The forced drop set by an external plugin, or {@code null} if
     *         no drop has been forced and normal selection should proceed.
     */
    @Nullable
    public CustomDrop getForcedDrop() {
        return forcedDrop;
    }

    /** @return {@code true} if an external plugin has forced a specific drop. */
    public boolean hasForcedDrop() {
        return forcedDrop != null;
    }

    // -------------------------------------------------------------------------
    // Event boilerplate
    // -------------------------------------------------------------------------

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
