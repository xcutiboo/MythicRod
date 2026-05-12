package io.xcutiboo.mythicrod.paper.util;

import java.util.Locale;

/// Formatting helpers for names shown in MythicRod menus and commands.
public final class StringFormatting {
    private StringFormatting() {}

    /// Turns item ids such as `minecraft:diamond_pickaxe` into `Diamond Pickaxe`.
    public static String formatMaterialName(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "Unknown";
        }
        String cleanId = identifier;
        if (identifier.contains(":")) {
            cleanId = identifier.substring(identifier.indexOf(":") + 1);
        }
        String[] parts = cleanId.split("[_\\s]");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                  .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }

    /// Turns category ids such as `biome_mushroom_fields` into menu labels.
    public static String formatCategoryName(String category) {
        if (category == null || category.isEmpty()) {
            return "Unknown";
        }
        if (category.startsWith("biome_")) {
            return formatMaterialName(category.substring(6)) + " Biome";
        }
        return formatMaterialName(category);
    }

    /// Turns enchantment keys such as `minecraft:sweeping_edge` into `Sweeping Edge`.
    public static String formatEnchantName(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String cleanKey = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        String[] parts = cleanKey.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }

    /// Formats a number as an ordinal such as `1st` or `22nd`.
    public static String getOrdinal(int number) {
        if (number >= 11 && number <= 13) {
            return number + "th";
        }
        return switch (number % 10) {
            case 1 -> number + "st";
            case 2 -> number + "nd";
            case 3 -> number + "rd";
            default -> number + "th";
        };
    }

    /// Formats a duration in seconds as compact text for menus.
    public static String formatTime(int seconds) {
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
