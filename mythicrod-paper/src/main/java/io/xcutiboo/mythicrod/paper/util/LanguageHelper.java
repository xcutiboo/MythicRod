package io.xcutiboo.mythicrod.paper.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.config.LanguageManager;

import java.util.Map;

public class LanguageHelper {
    
    public static String tr(LanguageManager langManager, CommandSender sender, String key) {
        return langManager.tr(key);
    }
    
    public static String tr(LanguageManager langManager, CommandSender sender, String key, Map<String, String> placeholders) {
        return langManager.tr(key, placeholders);
    }
    
    public static String tr(LanguageManager langManager, Player player, String key) {
        return langManager.tr(key);
    }
    
    public static String tr(LanguageManager langManager, Player player, String key, Map<String, String> placeholders) {
        return langManager.tr(key, placeholders);
    }
}
