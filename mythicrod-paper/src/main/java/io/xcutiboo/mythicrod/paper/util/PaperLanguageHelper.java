package io.xcutiboo.mythicrod.paper.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.config.LanguageManager;

import java.util.Map;

/**
 * Helper class to bridge Paper's Bukkit types with LanguageManager's platform abstractions
 */
public class PaperLanguageHelper {
    
    /**
     * Get translated message for a Bukkit CommandSender
     * Bypasses platform abstraction by using default language
     */
    public static String tr(LanguageManager langManager, CommandSender sender, String key) {
        // Paper module doesn't use platform abstraction - just use default language
        return langManager.tr(key);
    }
    
    /**
     * Get translated message with placeholders for a Bukkit CommandSender
     */
    public static String tr(LanguageManager langManager, CommandSender sender, String key, Map<String, String> placeholders) {
        return langManager.tr(key, placeholders);
    }
    
    /**
     * Get translated message for a Bukkit Player
     */
    public static String tr(LanguageManager langManager, Player player, String key) {
        return langManager.tr(key);
    }
    
    /**
     * Get translated message with placeholders for a Bukkit Player
     */
    public static String tr(LanguageManager langManager, Player player, String key, Map<String, String> placeholders) {
        return langManager.tr(key, placeholders);
    }
}
