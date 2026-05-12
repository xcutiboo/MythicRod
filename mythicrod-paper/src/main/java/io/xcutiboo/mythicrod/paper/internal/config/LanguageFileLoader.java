package io.xcutiboo.mythicrod.paper.internal.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import io.xcutiboo.mythicrod.config.LanguageManager;
import io.xcutiboo.mythicrod.paper.util.PrettyLogger;

public final class LanguageFileLoader {
    private static final String[] BUNDLED_LANGS = {"en_US", "ja_JP"};

    private final JavaPlugin plugin;
    private final Logger logger;
    private final PrettyLogger prettyLogger;
    private final LanguageManager languageManager;

    public LanguageFileLoader(
        JavaPlugin plugin,
        Logger logger,
        PrettyLogger prettyLogger,
        LanguageManager languageManager
    ) {
        this.plugin = plugin;
        this.logger = logger;
        this.prettyLogger = prettyLogger;
        this.languageManager = languageManager;
    }

    public void loadLanguageFiles() {
        File langDirectory = new File(plugin.getDataFolder(), "lang");
        if (!langDirectory.exists() && !langDirectory.mkdirs()) {
            logger.warn("Failed to create language directory at {}", langDirectory.getAbsolutePath());
        }

        languageManager.resetTranslations();

        for (String lang : BUNDLED_LANGS) {
            File langFile = ensureLanguageFile(langDirectory, lang);
            Map<String, String> translations = loadMergedLanguageTranslations(langFile, lang);
            if (translations.isEmpty()) {
                continue;
            }

            languageManager.loadTranslations(lang, translations);
            prettyLogger.info("Loaded language: " + lang + " (" + translations.size() + " entries) from "
                + describeLanguageSource(langFile));
        }
    }

    private Map<String, String> loadMergedLanguageTranslations(File langFile, String lang) {
        YamlConfiguration bundledDefaults = loadBundledLanguageConfiguration(lang);
        YamlConfiguration diskOverrides = loadLanguageOverrideConfiguration(langFile, lang);
        MergeResult mergeResult = mergeTranslations(bundledDefaults, diskOverrides);

        if (diskOverrides != null && mergeResult.refreshedDiskValues() > 0) {
            saveRefreshedLanguageOverrides(langFile, lang, diskOverrides, mergeResult.refreshedDiskValues());
        }

        return mergeResult.translations();
    }

    static MergeResult mergeTranslations(YamlConfiguration bundledDefaults, YamlConfiguration diskOverrides) {
        if (bundledDefaults == null && diskOverrides == null) {
            return new MergeResult(Map.of(), 0);
        }

        Map<String, String> translations = new HashMap<>();
        if (bundledDefaults != null) {
            flattenYaml(bundledDefaults, "", translations);
        }

        int refreshedDiskValues = 0;
        if (diskOverrides != null) {
            Map<String, String> diskTranslations = new HashMap<>();
            flattenYaml(diskOverrides, "", diskTranslations);

            for (Map.Entry<String, String> entry : diskTranslations.entrySet()) {
                String bundledValue = translations.get(entry.getKey());
                Optional<String> replacement = LanguageOverridePolicy.replacementForDiskValue(
                    entry.getKey(), entry.getValue(), bundledValue);
                if (replacement.isPresent()) {
                    translations.put(entry.getKey(), replacement.get());
                    diskOverrides.set(entry.getKey(), replacement.get());
                    refreshedDiskValues++;
                } else if (LanguageOverridePolicy.shouldUseDiskOverride(
                    entry.getKey(), entry.getValue(), bundledValue)) {
                    translations.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return new MergeResult(translations, refreshedDiskValues);
    }

    private void saveRefreshedLanguageOverrides(
        File langFile,
        String lang,
        YamlConfiguration diskOverrides,
        int refreshedDiskValues
    ) {
        try {
            diskOverrides.save(langFile);
            prettyLogger.info("Refreshed " + refreshedDiskValues + " old language value"
                + (refreshedDiskValues == 1 ? "" : "s") + " in " + lang + ".yml");
        } catch (IOException e) {
            logger.warn("Failed to refresh old language override values in {}", langFile.getPath(), e);
        }
    }

    private String describeLanguageSource(File langFile) {
        if (langFile.isFile()) {
            return "bundled defaults + " + langFile.getPath();
        }
        return "bundled resources";
    }

    private File ensureLanguageFile(File langDirectory, String lang) {
        File langFile = new File(langDirectory, lang + ".yml");
        if (langFile.exists()) {
            return langFile;
        }

        String resourcePath = "lang/" + lang + ".yml";
        try {
            plugin.saveResource(resourcePath, false);
        } catch (IllegalArgumentException e) {
            logger.warn("Missing bundled language resource {}", resourcePath);
        } catch (Exception e) {
            logger.warn("Failed to save bundled language file {}", resourcePath, e);
        }
        return langFile;
    }

    private YamlConfiguration loadBundledLanguageConfiguration(String lang) {
        String resourcePath = "lang/" + lang + ".yml";
        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream == null) {
                logger.warn("No language file found for {}", lang);
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.warn("Failed to load language {}", lang, e);
            return null;
        }
    }

    private YamlConfiguration loadLanguageOverrideConfiguration(File langFile, String lang) {
        if (!langFile.isFile()) {
            return null;
        }

        try {
            return YamlConfiguration.loadConfiguration(langFile);
        } catch (Exception e) {
            logger.warn("Failed to load language override {} from {}", lang, langFile.getPath(), e);
            return null;
        }
    }

    static void flattenYaml(ConfigurationSection section, String prefix, Map<String, String> result) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (section.isConfigurationSection(key)) {
                flattenYaml(section.getConfigurationSection(key), fullKey, result);
            } else if (section.isList(key)) {
                result.put(fullKey, String.join("\n", section.getStringList(key)));
            } else {
                String value = section.getString(key);
                if (value != null) {
                    result.put(fullKey, value);
                }
            }
        }
    }

    record MergeResult(Map<String, String> translations, int refreshedDiskValues) {
    }
}
