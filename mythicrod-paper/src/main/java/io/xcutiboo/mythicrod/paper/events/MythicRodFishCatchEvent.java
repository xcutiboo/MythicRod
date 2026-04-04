package io.xcutiboo.mythicrod.paper.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.xcutiboo.mythicrod.drops.CustomDrop;

/**
 * Fired on the player's EntityScheduler region thread immediately before a custom
 * MythicRod drop replaces the vanilla caught item entity.
 *
 * <p>Cancelling this event prevents MythicRod from applying the custom drop,
 * allowing the vanilla item to persist unchanged.
 *
 * <p><strong>Thread context:</strong> This event is always fired on the region
 * thread that owns the player (Folia-safe). Do NOT assume this is the "main" thread.
 *
 * <p><strong>Usage for external plugins:</strong>
 * <pre>{@code
 * @EventHandler(priority = EventPriority.NORMAL)
 * public void onMythicCatch(MythicRodFishCatchEvent event) {
 *     if (event.getDrop().getTier().equals("legendary")) {
 *         // Grant quest progress
 *     }
 * }
 * }</pre>
 */
public final class MythicRodFishCatchEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final @NotNull Player player;
    private final @NotNull CustomDrop drop;
    private @NotNull ItemStack rewardItem;
    private boolean cancelled = false;

    /**
     * @param player     The player who caught the fish.
     * @param drop       The selected {@link CustomDrop} before item generation.
     * @param rewardItem The fully-built {@link ItemStack} about to be given.
     */
    public MythicRodFishCatchEvent(
            @NotNull Player player,
            @NotNull CustomDrop drop,
            @NotNull ItemStack rewardItem) {
        // Not async — fired on entity region thread in Folia
        super(false);
        this.player = player;
        this.drop = drop;
        this.rewardItem = rewardItem.clone(); // defensive copy
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** @return The player who triggered the catch. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** @return The {@link CustomDrop} entry that was selected from the drop table. */
    @NotNull
    public CustomDrop getDrop() {
        return drop;
    }

    /**
     * @return The {@link ItemStack} that will be awarded. External plugins may
     *         replace this via {@link #setRewardItem(ItemStack)}.
     */
    @NotNull
    public ItemStack getRewardItem() {
        return rewardItem.clone();
    }

    /**
     * Allows external plugins to swap the reward item before it is applied.
     * The provided item is defensively copied.
     *
     * @param rewardItem Replacement item; must not be null or AIR.
     * @throws IllegalArgumentException if the provided item is null or air.
     */
    public void setRewardItem(@NotNull ItemStack rewardItem) {
        if (rewardItem.getType().isAir()) {
            throw new IllegalArgumentException("Reward item cannot be AIR");
        }
        this.rewardItem = rewardItem.clone();
    }

    // -------------------------------------------------------------------------
    // Cancellable
    // -------------------------------------------------------------------------

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
