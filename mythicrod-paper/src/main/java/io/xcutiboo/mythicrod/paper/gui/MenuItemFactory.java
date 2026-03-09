package io.xcutiboo.mythicrod.paper.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.paper.item.ItemBuilder;

/// Factory for shared menu controls used across MythicRod inventory screens.
public final class MenuItemFactory {

    private MenuItemFactory() {
    }

    /// Creates a toggle item that shows enabled/disabled state.
    ///
    /// @param enabled current state
    /// @param name localized item name
    /// @param description localized description of what this setting controls
    /// @param statusLine localized status lore line
    /// @param actionLine localized click-action lore line
    /// @param enabledMaterial material shown when enabled
    /// @param disabledMaterial material shown when disabled
    /// @return configured item stack
    public static ItemStack createToggleItem(
            boolean enabled,
            String name,
            String description,
            String statusLine,
            String actionLine,
            Material enabledMaterial,
            Material disabledMaterial) {

        Material material = enabled ? enabledMaterial : disabledMaterial;

        return new ItemBuilder(material)
                .name(name)
                .lore(
                        description,
                        "",
                        statusLine,
                        "",
                        actionLine
                )
                .glow(enabled)
                .build();
    }

}
