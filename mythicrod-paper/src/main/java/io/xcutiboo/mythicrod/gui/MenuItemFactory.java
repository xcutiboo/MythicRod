package io.xcutiboo.mythicrod.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.item.ItemBuilder;

/**
 * Factory for creating common menu item patterns.
 * Eliminates DRY violations across GUI menus by centralizing item creation logic.
 */
public final class MenuItemFactory {

    private MenuItemFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a toggle item that shows enabled/disabled state.
     * 
     * @param enabled Current state
     * @param name Item name (without status indicator)
     * @param description What this toggle controls
     * @param enabledMaterial Material when enabled
     * @param disabledMaterial Material when disabled
     * @return Configured ItemStack
     */
    public static ItemStack createToggleItem(
            boolean enabled,
            String name,
            String description,
            Material enabledMaterial,
            Material disabledMaterial) {
        
        Material material = enabled ? enabledMaterial : disabledMaterial;
        String statusColor = enabled ? "<green>" : "<red>";
        String statusText = enabled ? "✓ ENABLED" : "✗ DISABLED";
        String actionText = enabled ? "<red>disable" : "<green>enable";

        return new ItemBuilder(material)
                .name("<yellow><bold>" + name + " " + statusColor + statusText + "</bold></yellow>")
                .lore(
                        "<gray>" + description + "</gray>",
                        "",
                        "<gray>Status: " + statusColor + statusText + "</gray>",
                        "",
                        "<yellow>▶ Click to " + actionText + "</yellow>"
                )
                .glow(enabled)
                .build();
    }

    /**
     * Creates a toggle item with default materials (NOTE_BLOCK/GRAY_STAINED_GLASS).
     */
    public static ItemStack createToggleItem(boolean enabled, String name, String description) {
        return createToggleItem(enabled, name, description, Material.NOTE_BLOCK, Material.GRAY_STAINED_GLASS);
    }

    /**
     * Creates a navigation back button.
     */
    public static ItemStack createBackButton(String destination) {
        return new ItemBuilder(Material.ARROW)
                .name("<yellow>← Back to " + destination + "</yellow>")
                .lore("<gray>Click to return</gray>")
                .build();
    }

    /**
     * Creates a close button.
     */
    public static ItemStack createCloseButton() {
        return new ItemBuilder(Material.BARRIER)
                .name("<red><bold>Close</bold></red>")
                .lore("<gray>Click to close menu</gray>")
                .build();
    }

    /**
     * Creates a save/confirm button.
     */
    public static ItemStack createSaveButton() {
        return new ItemBuilder(Material.EMERALD)
                .name("<green><bold>✓ Save Changes</bold></green>")
                .lore(
                        "<gray>Save all changes</gray>",
                        "",
                        "<yellow>▶ Click to save</yellow>"
                )
                .glow(true)
                .build();
    }

    /**
     * Creates an info/help button.
     */
    public static ItemStack createInfoButton(String title, String... loreLines) {
        ItemBuilder builder = new ItemBuilder(Material.BOOK)
                .name("<gold><bold>" + title + "</bold></gold>");
        
        for (String line : loreLines) {
            builder.lore(line);
        }
        
        return builder.build();
    }

    /**
     * Creates a border item with empty name.
     */
    public static ItemStack createBorderItem(Material material) {
        return new ItemBuilder(material)
                .name(" ")
                .build();
    }

    /**
     * Creates a numeric adjustment item (for values like intervals, amounts).
     */
    public static ItemStack createNumericItem(
            Material material,
            String title,
            int currentValue,
            String unit,
            String description) {
        
        return new ItemBuilder(material)
                .name("<aqua><bold>" + title + "</bold></aqua>")
                .lore(
                        "<gray>" + description + "</gray>",
                        "",
                        "<yellow>Current: <white>" + formatTime(currentValue) + "</white> (<white>" + currentValue + unit + "</white>)</yellow>",
                        "",
                        "<yellow>▶ Controls:</yellow>",
                        "<gray>  Left-click: <green>+" + unit + "</green></gray>",
                        "<gray>  Right-click: <red>-" + unit + "</red></gray>",
                        "<gray>  Shift-left: <green>+5" + unit + "</green></gray>",
                        "<gray>  Shift-right: <red>-5" + unit + "</red></gray>"
                )
                .build();
    }

    private static String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m" + (remainingSeconds > 0 ? " " + remainingSeconds + "s" : "");
        }
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return hours + "h" + (remainingMinutes > 0 ? " " + remainingMinutes + "m" : "");
    }
}
