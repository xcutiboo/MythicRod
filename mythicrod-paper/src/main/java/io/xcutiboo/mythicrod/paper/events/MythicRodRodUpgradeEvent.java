package io.xcutiboo.mythicrod.paper.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fired on the player's EntityScheduler region thread when a player attempts to
 * upgrade a MythicRod (e.g., via Anvil combination or Crafter UI).
 *
 * <p>Cancelling this event prevents the upgrade from occurring. The base rod
 * and upgrade material will remain in the player's inventory unchanged.
 *
 * <p><strong>Thread context:</strong> Always fired on the entity's region thread.
 */
public final class MythicRodRodUpgradeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    /** The mechanism by which the upgrade was triggered. */
    public enum UpgradeType {
        /** Player used an Anvil to combine two rods or add an upgrade material. */
        ANVIL,
        /** Plugin-provided crafter or GUI upgrade path. */
        CRAFTER,
        /** Triggered programmatically via the {@code MythicRodAPI}. */
        API
    }

    private final @NotNull Player player;
    private final @NotNull ItemStack baseRod;
    private final @NotNull ItemStack upgradeMaterial;
    private @NotNull ItemStack resultRod;
    private final @NotNull UpgradeType upgradeType;
    private boolean cancelled = false;

    /**
     * @param player          The player performing the upgrade.
     * @param baseRod         The existing MythicRod being upgraded (defensive copy stored).
     * @param upgradeMaterial The item used as the upgrade catalyst (defensive copy stored).
     * @param resultRod       The projected result rod (defensive copy stored).
     * @param upgradeType     The mechanism that triggered this upgrade.
     */
    public MythicRodRodUpgradeEvent(
            @NotNull Player player,
            @NotNull ItemStack baseRod,
            @NotNull ItemStack upgradeMaterial,
            @NotNull ItemStack resultRod,
            @NotNull UpgradeType upgradeType) {
        super(false); // Entity region thread, not async
        this.player = player;
        this.baseRod = baseRod.clone();
        this.upgradeMaterial = upgradeMaterial.clone();
        this.resultRod = resultRod.clone();
        this.upgradeType = upgradeType;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** @return The player performing the upgrade. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** @return A defensive copy of the rod being upgraded. */
    @NotNull
    public ItemStack getBaseRod() {
        return baseRod.clone();
    }

    /** @return A defensive copy of the upgrade material. */
    @NotNull
    public ItemStack getUpgradeMaterial() {
        return upgradeMaterial.clone();
    }

    /**
     * @return A defensive copy of the projected result rod. External plugins may
     *         replace this via {@link #setResultRod(ItemStack)}.
     */
    @NotNull
    public ItemStack getResultRod() {
        return resultRod.clone();
    }

    /**
     * Replaces the upgrade result. The provided item must be a valid ItemStack
     * (not AIR). It is defensively copied.
     *
     * @param resultRod The new result item.
     * @throws IllegalArgumentException if the result is AIR.
     */
    public void setResultRod(@NotNull ItemStack resultRod) {
        if (resultRod.getType().isAir()) {
            throw new IllegalArgumentException("Result rod cannot be AIR");
        }
        this.resultRod = resultRod.clone();
    }

    /** @return The type of upgrade that triggered this event. */
    @NotNull
    public UpgradeType getUpgradeType() {
        return upgradeType;
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
