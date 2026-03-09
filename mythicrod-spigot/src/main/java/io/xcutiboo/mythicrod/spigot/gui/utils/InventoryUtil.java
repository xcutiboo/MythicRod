package io.xcutiboo.mythicrod.spigot.gui.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Spigot-specific utility to handle inventory creation with Adventure Components.
 * Converts Components to legacy strings for Spigot compatibility.
 */
public class InventoryUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    /**
     * Create an inventory with an Adventure Component title (converted to legacy format for Spigot).
     */
    public static Inventory createInventory(InventoryHolder holder, int size, Component title) {
        String legacyTitle = SERIALIZER.serialize(title);
        return Bukkit.createInventory(holder, size, legacyTitle);
    }
}
