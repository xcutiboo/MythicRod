package io.xcutiboo.mythicrod.item;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import io.xcutiboo.mythicrod.constants.MythicRodKeys;

/**
 * Factory for creating and inspecting MythicRod custom fishing rods.
 *
 * <p><strong>API consistency:</strong> Display name and lore are set via the
 * {@link ItemBuilder} DataComponent API.  PersistentDataContainer writes use
 * {@link ItemStack#editMeta} (the only mutable PDC path in Paper 1.21+), while
 * PDC reads use the read-only {@link ItemStack#getPersistentDataContainer()} view.
 * {@code hasItemMeta()} is intentionally avoided — DataComponent items do not
 * always carry a traditional {@code ItemMeta}, so that guard produced false-negatives.
 */
public class RodFactory {
    private final NamespacedKey customRodKey;
    private final NamespacedKey rodTierKey;

    public RodFactory(Plugin plugin) {
        this.customRodKey = new NamespacedKey(plugin, MythicRodKeys.CUSTOM_ROD_KEY);
        this.rodTierKey   = new NamespacedKey(plugin, MythicRodKeys.ROD_TIER_KEY);
    }

    public ItemStack createBasicRod() {
        return createRod(
            MythicRodKeys.DEFAULT_ROD_TIER,
            "<aqua><bold>MythicRod</bold></aqua>",
            new String[]{
                "<gray>A magical fishing rod that catches",
                "<gray>rare and legendary items!",
                "",
                "<gold>Tier: <yellow>Basic</yellow>",
                "<dark_gray>✦ Base catch rate"
            }
        );
    }

    public ItemStack createAdvancedRod() {
        return createRod(
            "advanced",
            "<light_purple><bold>MythicRod</bold></light_purple>",
            new String[]{
                "<gray>An enhanced fishing rod with",
                "<gray>improved magical properties!",
                "",
                "<gold>Tier: <light_purple>Advanced</light_purple>",
                "<dark_gray>✦✦ +25% Rare catch rate",
                "<dark_gray>✦✦ Unlock exclusive drops"
            }
        );
    }

    public ItemStack createLegendaryRod() {
        return createRod(
            "legendary",
            "<gradient:#FFD700:#FFAA00><bold>MythicRod</bold></gradient>",
            new String[]{
                "<gray>The ultimate fishing rod, blessed",
                "<gray>by the gods of the sea!",
                "",
                "<gold>Tier: <gradient:#FFD700:#FFAA00>Legendary</gradient>",
                "<dark_gray>✦✦✦ +50% Rare catch rate",
                "<dark_gray>✦✦✦ Unlock legendary drops",
                "<dark_gray>✦✦✦ Faster bite time"
            }
        );
    }

/**
 * Creates a custom fishing rod with the given tier, display name, and lore.
 *
 * <p>Display name and lore are applied via the {@link ItemBuilder} DataComponent
 * API.  PersistentDataContainer data is then written through
 * {@link ItemStack#editMeta} — the only correct write path in Paper 1.21+
 * because {@link ItemStack#getPersistentDataContainer()} returns a read-only
 * {@code PersistentDataContainerView}.
 */
public ItemStack createRod(String tier, String name, String[] lore) {
    // Build display via DataComponent API
    ItemStack rod = ItemBuilder.of(Material.FISHING_ROD)
        .name(name)
        .lore(Arrays.asList(lore))
        .build();

    // Write PDC keys via editMeta — required because getPersistentDataContainer()
    // returns a READ-ONLY view in Paper 1.21+; set() is only available on the
    // mutable PersistentDataContainer exposed by ItemMeta.
    rod.editMeta(meta -> {
        meta.getPersistentDataContainer().set(customRodKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(rodTierKey,   PersistentDataType.STRING, tier);
    });

    return rod;
}

    /**
     * Returns {@code true} if the given item is a MythicRod custom rod.
     *
     * <p>Does NOT use {@code hasItemMeta()} — DataComponent items may not have a
     * traditional ItemMeta, so the guard was misleading and could produce false-negatives.
     * The PDC is read directly from the {@link ItemStack} instead.
     */
    public boolean isCustomRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD) {
            return false;
        }
        return item.getPersistentDataContainer().has(customRodKey, PersistentDataType.BYTE);
    }

    /**
     * Returns the tier string stored in the rod's PersistentDataContainer,
     * or {@code null} if the item is not a custom MythicRod.
     */
    public String getRodTier(ItemStack item) {
        if (!isCustomRod(item)) {
            return null;
        }
        return item.getPersistentDataContainer().get(rodTierKey, PersistentDataType.STRING);
    }
}
