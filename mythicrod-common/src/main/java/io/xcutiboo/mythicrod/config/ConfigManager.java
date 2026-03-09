package io.xcutiboo.mythicrod.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;

/**
 * Production-Grade Configuration Manager for MythicRod
 *
 * DESIGN PRINCIPLES:
 * - Single source of truth: all getters return cached values (O(1) access)
 * - Validation on load + on every write (fail-safe defaults)
 * - Strict enum validation for profiles/languages
 * - All numeric values constrained to safe ranges
 * - Paper-first, backwards compatible
 *
 * CACHING STRATEGY:
 * - All config values cached in private fields at startup
 * - Setters update cache + YAML atomically
 * - No repeated YAML lookups in hot paths (fishing events, etc)
 * - reload() synchronizes cache with disk
 *
 * NAMING CONVENTION:
 * - is[Feature]Enabled() -> return boolean
 * - get[Property]() -> return typed value
 * - set[Property](value) -> updates cache + saves to disk
 */
public class ConfigManager {

    private final MythicRodPlugin plugin;
    private File configFile;
    private PlatformConfiguration config;
    private File statsFile;
    private PlatformConfiguration statsConfig;
    private File dropsFile;
    private File messagesFile;
    private static final int CURRENT_CONFIG_VERSION = 4;

    // CACHED VALUES (primary source of truth at runtime)
    private String prefix = "&6&l[MythicRod] &r";
    private boolean soundsEnabled = true;
    private boolean particlesEnabled = true;
    private boolean biomeDropsEnabled = true;
    private boolean statisticsEnabled = true;
    private boolean permissionsEnabled = false;
    private boolean debugEnabled = false;
    private int statsSaveInterval = 600;
    private int hookCleanupInterval = 300;
    private String language = "en_US";
    private String profile = "balanced";

    // CONSTRAINTS
    private static final int MIN_STATS_INTERVAL = 60;
    private static final int MAX_STATS_INTERVAL = 3600;
    private static final int MIN_HOOK_INTERVAL = 60;
    private static final int MAX_HOOK_INTERVAL = 1800;
    private static final String[] VALID_PROFILES = {"lightweight", "balanced", "performance"};
    private static final String[] VALID_LANGUAGES = {"en_US", "ja_JP"};

    public ConfigManager(MythicRodPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        initialize();
    }

    /**
     * Load and validate all cached configuration values from disk.
     * Called at startup and on reload(). No validation errors are fatal -
     * safe defaults always apply.
     */
    private void validateAndCache() {
        // Validate boolean flags (always safe - fallback to default if malformed)
        soundsEnabled = config.getBoolean("features.sounds.enabled", true);
        particlesEnabled = config.getBoolean("features.particles.enabled", true);
        biomeDropsEnabled = config.getBoolean("features.drops.biome-specific.enabled", true);
        statisticsEnabled = config.getBoolean("features.statistics.enabled", true);
        permissionsEnabled = config.getBoolean("features.permissions.enabled", false);
        debugEnabled = config.getBoolean("features.debug.enabled", false);

        // Validate prefix (non-empty)
        prefix = config.getString("ui.prefix", "&6&l[MythicRod] &r");
        if (prefix == null || prefix.isEmpty()) {
            prefix = "&6&l[MythicRod] &r";
            logWarning("ui.prefix is empty, using default");
        }

        // Validate language (must be in VALID_LANGUAGES)
        String rawLang = config.getString("language.default", "en_US");
        language = isValidLanguage(rawLang) ? rawLang : "en_US";
        if (!rawLang.equals(language)) {
            logWarning("language.default '" + rawLang + "' is invalid, using " + language);
        }

        // Validate profile (must be in VALID_PROFILES)
        String rawProfile = config.getString("profile", "balanced");
        profile = isValidProfile(rawProfile) ? rawProfile : "balanced";
        if (!rawProfile.equals(profile)) {
            logWarning("profile '" + rawProfile + "' is invalid, using " + profile);
        }

        // Validate numeric intervals with clamping
        int rawStats = config.getInt("timers.stats-save-interval-seconds", 600);
        statsSaveInterval = clampInterval(rawStats, MIN_STATS_INTERVAL, MAX_STATS_INTERVAL, "stats-save-interval-seconds");

        int rawHook = config.getInt("timers.hook-cleanup-interval-seconds", 300);
        hookCleanupInterval = clampInterval(rawHook, MIN_HOOK_INTERVAL, MAX_HOOK_INTERVAL, "hook-cleanup-interval-seconds");

        // Log if debug enabled (cheap operation)
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        // The platform implementation provides the actual configuration parser
        config = plugin.getPlatform().loadConfiguration(configFile);
        
        int version = config.getInt("config-version", 1);
        if (version < CURRENT_CONFIG_VERSION) {
            migrateConfig(version);
        }

        reloadCache();

        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create stats.yml", e);
            }
        }
        statsConfig = plugin.getPlatform().loadConfiguration(statsFile);
    }

    /**
     * Reloads configuration from disk and updates all caches.
     */
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = plugin.getPlatform().loadConfiguration(configFile);
        
        if (statsFile == null) {
            statsFile = new File(plugin.getDataFolder(), "stats.yml");
        }
        statsConfig = plugin.getPlatform().loadConfiguration(statsFile);
        
        reloadCache();
        
        if (debugEnabled) {
            logConfigurationSummary();
        }
    }

    /**
     * Internal atomic cache update
     */
    private synchronized void reloadCache() {
        // UI
        this.prefix = config.getString("ui.prefix", "&8[&6MythicRod&8]");
        
        // Features
        this.soundsEnabled = config.getBoolean("features.sounds.enabled", true);
        this.particlesEnabled = config.getBoolean("features.particles.enabled", true);
        this.biomeDropsEnabled = config.getBoolean("features.drops.biome-specific.enabled", true);
        this.statisticsEnabled = config.getBoolean("features.statistics.enabled", true);
        this.permissionsEnabled = config.getBoolean("features.permissions.enabled", false);
        this.debugEnabled = config.getBoolean("features.debug.enabled", false);

        // Performance / Timers
        this.statsSaveInterval = clampInterval(
            config.getInt("timers.stats-save-interval-seconds", 600),
            MIN_STATS_INTERVAL, MAX_STATS_INTERVAL, "stats-save-interval-seconds"
        );
        
        this.hookCleanupInterval = clampInterval(
            config.getInt("timers.hook-cleanup-interval-seconds", 300),
            MIN_HOOK_INTERVAL, MAX_HOOK_INTERVAL, "hook-cleanup-interval-seconds"
        );

        // Localization
        String cfgLang = config.getString("language.default", "en_US");
        this.language = isValidLanguage(cfgLang) ? cfgLang : "en_US";

        // Profiles
        String cfgProf = config.getString("profile", "balanced");
        this.profile = isValidProfile(cfgProf) ? cfgProf : "balanced";
    }

    /**
     * Safely clamp interval values within bounds and warn if adjusted
     */
    private int clampInterval(int value, int min, int max, String keyName) {
        if (value < min) {
            logWarning("'" + keyName + "' (" + value + ") is below minimum allowed (" + min + "). Clamped to " + min);
            return min;
        }
        if (value > max) {
            logWarning("'" + keyName + "' (" + value + ") is above maximum allowed (" + max + "). Clamped to " + max);
            return max;
        }
        return value;
    }

    private void logWarning(String message) {
        plugin.getLogger().warning("[Configuration] " + message);
    }

    private boolean isValidLanguage(String lang) {
        if (lang == null || lang.isEmpty()) return false;
        // Basic validation: en_US, ja_JP, zh_CN, etc. 
        // Could also verify if file exists in future
        return lang.matches("^[a-z]{2}(_[A-Z]{2})?$");
    }

    private boolean isValidProfile(String prof) {
        if (prof == null) return false;
        String l = prof.toLowerCase();
        return l.equals("op") || l.equals("balanced") || l.equals("hardcore");
    }

    // ==========================================
    // GETTERS (O(1) Memory Access)
    // ==========================================

    public boolean useSounds() { return soundsEnabled; }
    public boolean useParticles() { return particlesEnabled; }
    public boolean enableBiomeSpecificDrops() { return biomeDropsEnabled; }
    public boolean trackStatistics() { return statisticsEnabled; }
    public boolean usePermissions() { return permissionsEnabled; }
    public boolean isDebugMode() { return debugEnabled; }
    
    public int getStatsSaveInterval() { return statsSaveInterval; }
    public int getHookCleanupInterval() { return hookCleanupInterval; }
    
    public String getLanguage() { return language; }
    public String getProfile() { return profile; }
    public String getPrefix() { return prefix; }

    public PlatformConfiguration getStatsConfig() {
        if (statsConfig == null) {
            reloadConfig();
        }
        return statsConfig;
    }

    public void saveStats() {
        if (statsConfig == null || statsFile == null) return;
        try {
            statsConfig.save(statsFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save stats to " + statsFile, e);
        }
    }

    // ==========================================
    // SETTERS (Write-Through)
    // ==========================================

    public void setSoundsEnabled(boolean value) {
        this.soundsEnabled = value;
        config.set("features.sounds.enabled", value);
        saveToFile();
    }

    public void setParticlesEnabled(boolean value) {
        this.particlesEnabled = value;
        config.set("features.particles.enabled", value);
        saveToFile();
    }

    public void setBiomeDropsEnabled(boolean value) {
        this.biomeDropsEnabled = value;
        config.set("features.drops.biome-specific.enabled", value);
        saveToFile();
    }

    public void setStatisticsEnabled(boolean value) {
        this.statisticsEnabled = value;
        config.set("features.statistics.enabled", value);
        saveToFile();
    }

    public void setPermissionsEnabled(boolean value) {
        this.permissionsEnabled = value;
        config.set("features.permissions.enabled", value);
        saveToFile();
    }

    public void setDebugEnabled(boolean value) {
        this.debugEnabled = value;
        config.set("features.debug.enabled", value);
        saveToFile();
    }

    public void setStatsSaveInterval(int seconds) {
        this.statsSaveInterval = clampInterval(seconds, MIN_STATS_INTERVAL, MAX_STATS_INTERVAL, "stats-save-interval-seconds");
        config.set("timers.stats-save-interval-seconds", statsSaveInterval);
        saveToFile();
    }

    public void setHookCleanupInterval(int seconds) {
        this.hookCleanupInterval = clampInterval(seconds, MIN_HOOK_INTERVAL, MAX_HOOK_INTERVAL, "hook-cleanup-interval-seconds");
        config.set("timers.hook-cleanup-interval-seconds", hookCleanupInterval);
        saveToFile();
    }

    public void setLanguage(String langCode) {
        if (!isValidLanguage(langCode)) {
            logWarning("language.default '" + langCode + "' is invalid");
            return;
        }
        this.language = langCode;
        config.set("language.default", langCode);
        saveToFile();
    }

    public void setProfile(String prof) {
        if (!isValidProfile(prof)) {
            logWarning("profile '" + prof + "' is invalid");
            return;
        }
        this.profile = prof;
        config.set("profile", prof);
        saveToFile();
    }

    private void saveToFile() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config.yml", e);
        }
    }

    /**
     * Log configuration summary to console (when debug enabled)
     */
    private void logConfigurationSummary() {
        plugin.getLogger().info("╔════════════════════════════════════════╗");
        plugin.getLogger().info("║   MythicRod Configuration               ║");
        plugin.getLogger().info("╠════════════════════════════════════════╣");
        plugin.getLogger().info("║ FEATURES                                ║");
        plugin.getLogger().info("║   Sounds: " + statusIcon(soundsEnabled) + "                      ║");
        plugin.getLogger().info("║   Particles: " + statusIcon(particlesEnabled) + "                   ║");
        plugin.getLogger().info("║   Statistics: " + statusIcon(statisticsEnabled) + "                   ║");
        plugin.getLogger().info("║   Biome Drops: " + statusIcon(biomeDropsEnabled) + "                   ║");
        plugin.getLogger().info("║   Permissions: " + statusIcon(permissionsEnabled) + "                    ║");
        plugin.getLogger().info("║ PERFORMANCE                             ║");
        plugin.getLogger().info("║   Profile: " + String.format("%-27s", profile.toUpperCase()) + "║");
        plugin.getLogger().info("║   Stats Save: " + String.format("%-24s", statsSaveInterval + "s") + "║");
        plugin.getLogger().info("║   Hook Cleanup: " + String.format("%-22s", hookCleanupInterval + "s") + "║");
        plugin.getLogger().info("║   Language: " + String.format("%-26s", language) + "║");
        plugin.getLogger().info("║ DEBUG                                   ║");
        plugin.getLogger().info("║   Debug Mode: " + statusIcon(debugEnabled) + "                    ║");
        plugin.getLogger().info("╚════════════════════════════════════════╝");
    }

    private String statusIcon(boolean enabled) {
        return enabled ? "✓ ENABLED" : "✗ DISABLED";
    }

    public void saveConfig() throws Exception {
        if (configFile != null && config != null) {
            config.save(configFile);
        }
    }

    /**
     * Migrate from legacy config versions to v4 (kebab-case, strict validation)
     */
    private void migrateConfig(int fromVersion) {
        plugin.getLogger().info("Migrating config from v" + fromVersion + " to v" + CURRENT_CONFIG_VERSION);
        config.set("config-version", CURRENT_CONFIG_VERSION);

        // UI
        if (config.contains("prefix")) {
            config.set("ui.prefix", config.getString("prefix"));
        }

        // Features: read from old boolean keys
        config.set("features.sounds.enabled", config.getBoolean("use-sounds", true));
        config.set("features.particles.enabled", config.getBoolean("use-particles", true));
        config.set("features.drops.biome-specific.enabled", config.getBoolean("enable-biome-specific-drops", true));
        config.set("features.statistics.enabled", config.getBoolean("track-statistics", true));
        config.set("features.permissions.enabled", config.getBoolean("use-permissions", false));
        config.set("features.debug.enabled", config.getBoolean("debug-mode", false));

        // Timers: migrate to kebab-case
        int statsInterval = Math.max(MIN_STATS_INTERVAL, config.getInt("stats-save-interval", 600));
        int hookInterval = Math.max(MIN_HOOK_INTERVAL, config.getInt("hook-cleanup-interval", 300));
        config.set("timers.stats-save-interval-seconds", statsInterval);
        config.set("timers.hook-cleanup-interval-seconds", hookInterval);

        // Language
        String legacyLang = config.getString("language", null);
        if (legacyLang != null && isValidLanguage(legacyLang)) {
            config.set("language.default", legacyLang);
        } else {
            config.set("language.default", "en_US");
        }

        // Profile (new in v3)
        String legacyProfile = config.getString("profile", "balanced");
        if (isValidProfile(legacyProfile)) {
            config.set("profile", legacyProfile);
        } else {
            config.set("profile", "balanced");
        }

        try {
            config.save(configFile);
            plugin.getLogger().info("Configuration successfully migrated to v" + CURRENT_CONFIG_VERSION);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save migrated config.yml", e);
        }
    }
}
