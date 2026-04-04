package io.xcutiboo.mythicrod.config;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.text.MessageFormatter;
import net.kyori.adventure.text.Component;

import java.util.*;

public class LanguageManager {
    private final MessageFormatter messageFormatter;
    private final Map<String, Map<String, String>> translations = new HashMap<>();
    private String languageCode = "en_US";
    private final Map<String, String> fallbackTranslations = new HashMap<>();
    private final PlayerPreferences playerPreferences;

    public LanguageManager(MythicRodPlugin plugin, ConfigManager configManager) {
        this.playerPreferences = new PlayerPreferences(plugin);
        this.messageFormatter = new MessageFormatter(configManager.getPrefix());
        this.languageCode = normalizeLocale(configManager.getLanguage());
    }

    private String normalizeLocale(String code) {
        if (code == null || code.isEmpty()) return "en_US";
        String lc = code.trim().replace('-', '_');
        if (lc.equalsIgnoreCase("en")) return "en_US";
        return lc;
    }

    public void loadTranslations(String langCode, Map<String, String> translationMap) {
        translations.put(langCode, new HashMap<>(translationMap));
        if (langCode.equals("en_US")) {
            fallbackTranslations.putAll(translationMap);
        }
    }

    public String tr(String key) {
        Map<String, String> currentLang = translations.get(languageCode);
        if (currentLang != null && currentLang.containsKey(key)) {
            return currentLang.get(key);
        }
        if (fallbackTranslations.containsKey(key)) {
            return fallbackTranslations.get(key);
        }
        return key;
    }

    public String tr(String key, Map<String, String> placeholders) {
        String result = tr(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    public Component trComponent(String key) {
        String raw = tr(key);
        return messageFormatter.formatChatMessage(raw);
    }

    public Component trComponent(String key, Map<String, String> placeholders) {
        String raw = tr(key, placeholders);
        return messageFormatter.formatChatMessage(raw);
    }

    public List<Component> trLore(String key) {
        String raw = tr(key);
        if (raw.contains("\n")) {
            String[] lines = raw.split("\\n");
            List<Component> result = new ArrayList<>();
            for (String line : lines) {
                result.add(messageFormatter.formatLore(line));
            }
            return result;
        }
        return Collections.singletonList(messageFormatter.formatLore(raw));
    }

    public Component trTitle(String key) {
        String raw = tr(key);
        return messageFormatter.formatTitle(raw);
    }

    public Component trItemName(String key) {
        String raw = tr(key);
        return messageFormatter.formatItemName(raw);
    }

    public String getLanguage() {
        return languageCode;
    }

    public void setLanguage(String language) {
        this.languageCode = normalizeLocale(language);
    }

    public void setPlayerLanguage(UUID playerId, String language) {
        playerPreferences.setLanguage(playerId, normalizeLocale(language));
    }

    public String getPlayerLanguage(UUID playerId) {
        return playerPreferences.getLanguage(playerId);
    }

    public List<String> getAvailableLanguages() {
        return new ArrayList<>(translations.keySet());
    }
}
