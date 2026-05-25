package io.xcutiboo.mythicrod.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.xcutiboo.mythicrod.internal.runtime.MythicRodRuntime;
import io.xcutiboo.mythicrod.text.MessageFormatter;
import net.kyori.adventure.text.Component;

public class LanguageManager {
    private static final String DEFAULT_LOCALE = "en_US";

    private MessageFormatter messageFormatter;
    private final ConfigManager configManager;
    private final Map<String, Map<String, String>> translations = new HashMap<>();
    private String languageCode = DEFAULT_LOCALE;
    private final Map<String, String> fallbackTranslations = new HashMap<>();
    private final PlayerPreferences playerPreferences;

    public LanguageManager(MythicRodRuntime runtime, ConfigManager configManager) {
        this.configManager = configManager;
        this.playerPreferences = new PlayerPreferences(runtime);
        this.messageFormatter = new MessageFormatter(configManager.getPrefix());
        this.languageCode = normalizeLocale(configManager.getLanguage());
    }

    private String normalizeLocale(String code) {
        if (code == null || code.isEmpty()) return DEFAULT_LOCALE;
        String lc = code.trim().replace('-', '_');
        if (lc.equalsIgnoreCase("en")) return DEFAULT_LOCALE;
        return lc;
    }

    public void loadTranslations(String langCode, Map<String, String> translationMap) {
        String normalizedLocale = normalizeLocale(langCode);
        translations.put(normalizedLocale, new HashMap<>(translationMap));
        if (normalizedLocale.equals(DEFAULT_LOCALE)) {
            fallbackTranslations.clear();
            fallbackTranslations.putAll(translationMap);
        }
    }

    public void resetTranslations() {
        translations.clear();
        fallbackTranslations.clear();
    }

    public String tr(String key) {
        Map<String, String> currentLang = translations.get(languageCode);
        if (currentLang != null) {
            String translated = currentLang.get(key);
            if (translated != null) {
                return translated;
            }
        }
        String fallback = fallbackTranslations.get(key);
        if (fallback != null) {
            return fallback;
        }
        return key;
    }

    public String trInLanguage(String key, String locale) {
        String normalizedLocale = normalizeLocale(locale);
        Map<String, String> langTranslations = translations.get(normalizedLocale);
        if (langTranslations != null) {
            String translated = langTranslations.get(key);
            if (translated != null) {
                return translated;
            }
        }
        return tr(key);
    }

    public String trForPlayer(UUID playerId, String key) {
        String playerLanguage = getPlayerLanguage(playerId);
        if (playerLanguage == null || playerLanguage.isEmpty()) {
            return tr(key);
        }
        return trInLanguage(key, playerLanguage);
    }

    public String tr(String key, Map<String, String> placeholders) {
        String result = tr(key);
        return applyPlaceholders(result, placeholders);
    }

    public String trInLanguage(String key, String locale, Map<String, String> placeholders) {
        String result = trInLanguage(key, locale);
        return applyPlaceholders(result, placeholders);
    }

    private String applyPlaceholders(String result, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholderKey = entry.getKey();
            String replacement = entry.getValue();
            if (placeholderKey == null || replacement == null) {
                continue;
            }
            String normalizedKey = normalizePlaceholderKey(placeholderKey);
            result = result.replace("%" + normalizedKey + "%", replacement);
            result = result.replace("{" + normalizedKey + "}", replacement);

            if (!normalizedKey.equals(placeholderKey)) {
                result = result.replace(placeholderKey, replacement);
            }
        }
        return result;
    }

    private String normalizePlaceholderKey(String placeholderKey) {
        if ((placeholderKey.startsWith("%") && placeholderKey.endsWith("%"))
            || (placeholderKey.startsWith("{") && placeholderKey.endsWith("}"))) {
            return placeholderKey.substring(1, placeholderKey.length() - 1);
        }
        return placeholderKey;
    }

    public String trForPlayer(UUID playerId, String key, Map<String, String> placeholders) {
        String playerLanguage = getPlayerLanguage(playerId);
        if (playerLanguage == null || playerLanguage.isEmpty()) {
            return tr(key, placeholders);
        }
        return trInLanguage(key, playerLanguage, placeholders);
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

    public void refreshFormatting() {
        this.messageFormatter = new MessageFormatter(configManager.getPrefix());
    }

    public void reloadPlayerPreferences() {
        playerPreferences.reloadFromDisk();
    }

    public void setPlayerLanguage(UUID playerId, String language) {
        playerPreferences.setLanguage(playerId, normalizeLocale(language));
    }

    public String getPlayerLanguage(UUID playerId) {
        return playerPreferences.getLanguage(playerId);
    }

    public String getEffectivePlayerLanguage(UUID playerId) {
        String playerLanguage = getPlayerLanguage(playerId);
        if (playerLanguage == null || playerLanguage.isEmpty()) {
            return languageCode;
        }
        return normalizeLocale(playerLanguage);
    }

    public List<String> getAvailableLanguages() {
        return new ArrayList<>(translations.keySet());
    }

    public void shutdown() {
        playerPreferences.shutdown();
    }
}
