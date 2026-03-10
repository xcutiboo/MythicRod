package io.xcutiboo.mythicrod.config;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

/**
 * Crowdin-ready language manager with:
 * - lang/en_US.yml authoritative base (bundled under resources/lang)
 * - Fallback to en_US with ONCE-only warnings for missing keys
 * - Named placeholder interpolation with safety
 * - Global default language + per-player override support hooks
 */
public class LanguageManager {
    private final MythicRodPlugin plugin;

    // Locale codes like en_US, ja_JP
    private String languageCode = "en_US";

    private PlatformConfiguration baseEnUS;
    private PlatformConfiguration selectedLang;

    // Warn once per missing key
    private final Set<String> warnedMissingKeys = new HashSet<>();
    // Warn once per placeholder mismatch
    private final Set<String> warnedPlaceholderMismatches = new HashSet<>();
    
    // Simple per-player preference store (players.yml in data folder)
    private final PlayerPreferences playerPreferences;

    public LanguageManager(MythicRodPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.playerPreferences = new PlayerPreferences(plugin);
        
        this.languageCode = normalizeLocale(configManager.getLanguage());
        ensureBundledBaseExists();
        loadBundles();
    }

    private String normalizeLocale(String code) {
        if (code == null || code.isEmpty()) return "en_US";
        // Accept short codes like "en" and map to en_US
        String lc = code.trim().replace('-', '_');
        if (lc.equalsIgnoreCase("en")) return "en_US";
        return lc;
    }

    /** Ensure the bundled base file exists in the JAR (best-effort). */
    private void ensureBundledBaseExists() {
        // No-op at runtime; resource is packaged under resources/lang/en_US.yml
        // We still create data folder copy on first run for server owners to inspect.
        File outDir = plugin.getDataFolder();
        File out = new File(outDir, "lang/en_US.yml");
        if (!out.exists()) {
            out.getParentFile().mkdirs();
            plugin.saveResource("lang/en_US.yml", false);
        }
    }

    private PlatformConfiguration loadYamlPreferData(String relativePathInData, String jarResourcePath) {
        File file = new File(plugin.getDataFolder(), relativePathInData);
        if (file.exists()) {
            return plugin.getPlatform().loadConfiguration(file);
        }
        InputStream is = plugin.getResource(jarResourcePath);
        if (is != null) {
            return plugin.getPlatform().loadConfiguration(is);
        }
        return plugin.getPlatform().createEmptyConfiguration();
    }

    private void loadBundles() {
        // Always load authoritative base
        baseEnUS = loadYamlPreferData("lang/en_US.yml", "lang/en_US.yml");

        // Selected locale
        String selectedJar = "lang/" + languageCode + ".yml";
        selectedLang = loadYamlPreferData("lang/" + languageCode + ".yml", selectedJar);
    }

    public void setLanguage(String code) {
        this.languageCode = normalizeLocale(code);
        loadBundles();
    }

    public String getLanguage() {
        return languageCode;
    }

    /** Set per-player language override and persist it. */
    public void setPlayerLanguage(java.util.UUID playerId, String code) {
        String normalized = normalizeLocale(code);
        playerPreferences.setLanguage(playerId, normalized);
    }

    /** Global default translation (server-level). */
    public String tr(String key) {
        String val = selectedLang.getString(key);
        if (val == null) {
            val = baseEnUS.getString(key);
            warnMissingOnce(key, selectedLang);
        }
        return Objects.requireNonNullElse(val, key);
    }

    /** Global translation with placeholders. */
    public String tr(String key, Map<String, String> placeholders) {
        return applyPlaceholders(tr(key), key, placeholders);
    }

    /** Per-sender translation honoring per-player override -> server default -> en_US. */
    public String trForSender(PlatformCommandSender sender, String key) {
        if (sender instanceof PlatformPlayer p) {
            String playerLang = playerPreferences.getLanguage(p.getUniqueId());
            if (playerLang != null) {
                PlatformConfiguration playerCfg = loadYamlPreferData("lang/" + playerLang + ".yml", "lang/" + playerLang + ".yml");
                String val = playerCfg.getString(key);
                if (val != null) return val;
            }
        }
        return tr(key);
    }

    public String trForSender(PlatformCommandSender sender, String key, Map<String, String> placeholders) {
        return applyPlaceholders(trForSender(sender, key), key, placeholders);
    }

    private String applyPlaceholders(String msg, String key, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) return msg;
        String result = msg;
        // Replace provided placeholders
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            String ph = "{" + e.getKey() + "}";
            if (!result.contains(ph)) {
                warnPlaceholderMismatchOnce(key, e.getKey());
            }
            result = result.replace(ph, Objects.toString(e.getValue(), ""));
        }
        return result;
    }

    private void warnMissingOnce(String key, PlatformConfiguration attemptedCfg) {
        if (attemptedCfg == null) return;
        if (warnedMissingKeys.add(key)) {
            plugin.getLogger().warning("Missing localization key '" + key + "' in locale '" + languageCode + "'. Falling back to en_US.");
        }
    }

    private void warnPlaceholderMismatchOnce(String key, String placeholder) {
        String compositeKey = key + ":" + placeholder;
        if (warnedPlaceholderMismatches.add(compositeKey)) {
            plugin.getLogger().fine("Placeholder '{" + placeholder + "}' not present in message key '" + key + "'. This is normal if the message doesn't need this placeholder.");
        }
    }
}
