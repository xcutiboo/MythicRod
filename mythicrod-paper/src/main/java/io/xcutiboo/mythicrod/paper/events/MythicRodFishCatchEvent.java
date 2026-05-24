package io.xcutiboo.mythicrod.paper.events;

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import io.xcutiboo.mythicrod.api.platform.PlatformDrop;
import io.xcutiboo.mythicrod.drops.CustomDrop;

/// Fired immediately before MythicRod applies a selected custom reward.
///
/// Cancelling this event skips MythicRod's replacement and leaves the original
/// Minecraft catch untouched.
///
/// The exposed `CustomDrop` is always the reward descriptor MythicRod is about
/// to process. Built-in rewards point at the configured drop entry. External
/// provider rewards use a synthetic adapter that preserves the provider key,
/// display name, tier, and item metadata in a familiar shape.
///
/// If you only need stable read-only fields such as identifier, amount, weight,
/// or tier, prefer `getDropView()` and program against `PlatformDrop`.
///
/// ## Thread Context
///
/// This event is fired from the player-owned execution path that is about to
/// deliver the reward. On ordinary Paper that is the synchronous fishing event
/// thread. On Folia it is the player's owning region thread. Do not assume a
/// global main thread exists.
///
/// ## Example
///
/// ```java
/// @EventHandler(priority = EventPriority.NORMAL)
/// public void onMythicCatch(MythicRodFishCatchEvent event) {
///     if ("legendary".equalsIgnoreCase(event.getDrop().getTier())) {
///         grantQuestProgress(event.getPlayer());
///     }
/// }
/// ```
@ApiStatus.AvailableSince("2026.1.0")
public final class MythicRodFishCatchEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final @NotNull Player player;
    private final @NotNull CustomDrop drop;
    private @NotNull ItemStack rewardItem;
    private boolean cancelled = false;

    /// @param player player who caught the fish
    /// @param drop selected drop before item generation
    /// @param rewardItem item MythicRod is about to give
    public MythicRodFishCatchEvent(
            @NotNull Player player,
            @NotNull CustomDrop drop,
            @NotNull ItemStack rewardItem) {
        super(false);
        this.player = Objects.requireNonNull(player, "player");
        this.drop = Objects.requireNonNull(drop, "drop");
        this.rewardItem = copyRewardItem(rewardItem);
    }

    /// Returns the Bukkit player for this catch.
    ///
    /// The event is already running on MythicRod's player-owned execution path.
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /// Returns the selected reward descriptor as the internal `CustomDrop`
    /// type. Experimental: the internal representation may change. Use
    /// `getDropView()` when you only need stable read-only metadata.
    @NotNull
    @ApiStatus.Experimental
    public CustomDrop getDrop() {
        return drop;
    }

    /// Returns the selected reward descriptor through MythicRod's stable drop view.
    ///
    /// Prefer this accessor when your plugin only needs read-only metadata and
    /// should not depend on `CustomDrop`-specific methods.
    @NotNull
    @SuppressWarnings("java:S4144")
    public PlatformDrop getDropView() {
        return drop;
    }

    /// Returns the item that will be awarded.
    ///
    /// External plugins may replace this through `setRewardItem(ItemStack)`.
    /// MythicRod defensively copies the stack and clamps the delivered amount to
    /// the material's maximum stack size.
    ///
    /// @return defensive copy of the pending reward item
    @NotNull
    public ItemStack getRewardItem() {
        return rewardItem.clone();
    }

    /// Replaces the reward item before MythicRod applies it.
    ///
    /// The provided item is defensively copied before storage.
    ///
    /// @param rewardItem replacement item; must not be null or air
    /// @throws IllegalArgumentException if the provided item is null or air
    public void setRewardItem(@NotNull ItemStack rewardItem) {
        this.rewardItem = copyRewardItem(rewardItem);
    }

    private static ItemStack copyRewardItem(ItemStack rewardItem) {
        Objects.requireNonNull(rewardItem, "rewardItem");
        if (rewardItem.getType().isAir()) {
            throw new IllegalArgumentException("Reward item cannot be AIR");
        }
        return rewardItem.clone();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
