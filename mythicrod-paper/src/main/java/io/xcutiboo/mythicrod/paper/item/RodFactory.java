package io.xcutiboo.mythicrod.paper.item;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.constants.MythicRodKeys;

/// Creates MythicRod fishing rods and reads their persistent tier data.
public class RodFactory {
    private static final String LORE_RARE_LUCK_SUFFIX = "x rare luck";

    private final MythicRod plugin;
    private final NamespacedKey customRodKey;
    private final NamespacedKey rodTierKey;

    public RodFactory(MythicRod plugin) {
        this.plugin = plugin;
        this.customRodKey = new NamespacedKey(plugin, MythicRodKeys.CUSTOM_ROD_KEY);
        this.rodTierKey   = new NamespacedKey(plugin, MythicRodKeys.ROD_TIER_KEY);
    }

    public ItemStack createBasicRod() {
        return createRod(
            MythicRodKeys.DEFAULT_ROD_TIER,
            "<aqua><bold>MythicRod</bold></aqua>",
            new String[]{
                "<gray>A reliable rod tuned for",
                "<gray>custom MythicRod catches",
                "",
                "<gold>Tier: <yellow>Basic</yellow>",
                "<dark_gray>✦ " + formatMultiplier("basic") + LORE_RARE_LUCK_SUFFIX
            }
        );
    }

    public ItemStack createAdvancedRod() {
        return createRod(
            "advanced",
            "<light_purple><bold>MythicRod</bold></light_purple>",
            new String[]{
                "<gray>A refined rod for players",
                "<gray>with higher-tier access",
                "",
                "<gold>Tier: <light_purple>Advanced</light_purple>",
                "<dark_gray>✦✦ " + formatMultiplier("advanced") + LORE_RARE_LUCK_SUFFIX,
                "<dark_gray>✦✦ Requires advanced rod access"
            }
        );
    }

    public ItemStack createLegendaryRod() {
        return createRod(
            "legendary",
            "<gradient:#FFD700:#FFAA00><bold>MythicRod</bold></gradient>",
            new String[]{
                "<gray>A showcase rod for servers",
                "<gray>that gate top-tier rewards",
                "",
                "<gold>Tier: <gradient:#FFD700:#FFAA00>Legendary</gradient>",
                "<dark_gray>✦✦✦ " + formatMultiplier("legendary") + LORE_RARE_LUCK_SUFFIX,
                "<dark_gray>✦✦✦ Requires legendary rod access",
                "<dark_gray>✦✦✦ Tuned for rare rewards"
            }
        );
    }

    /// Creates a rod item and stores the MythicRod marker plus tier in its PDC.
    public ItemStack createRod(String tier, String name, String[] lore) {
        ItemStack rod = ItemBuilder.of(Material.FISHING_ROD)
            .name(name)
            .lore(Arrays.asList(lore))
            .build();

        // ItemStack exposes a read-only PDC view; writes still go through ItemMeta.
        rod.editMeta(meta -> {
            meta.getPersistentDataContainer().set(customRodKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(rodTierKey, PersistentDataType.STRING, tier);
        });

        return rod;
    }

    /// Returns `true` when the item carries MythicRod's custom-rod marker.
    public boolean isCustomRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD) {
            return false;
        }
        return item.getPersistentDataContainer().has(customRodKey, PersistentDataType.BYTE);
    }

    /// Returns the stored rod tier, or `null` when the item is not a MythicRod.
    public String getRodTier(ItemStack item) {
        if (!isCustomRod(item)) {
            return null;
        }
        return item.getPersistentDataContainer().get(rodTierKey, PersistentDataType.STRING);
    }

    private String formatMultiplier(String tier) {
        double multiplier = plugin.getConfigManager() != null
            ? plugin.getConfigManager().getRodLuckMultiplier(tier)
            : 1.0D;
        return String.format(java.util.Locale.ROOT, "%.2f", multiplier);
    }
}
