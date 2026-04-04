package io.xcutiboo.mythicrod.config;

import java.io.File;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;

public class ConfigManager {

    private final MythicRodPlugin plugin;
    private PlatformConfiguration config;
    private static final int CURRENT_CONFIG_VERSION = 4;
    
    private static final String DEFAULT_PREFIX = "<gold><bold>[MythicRod]</bold></gold> ";

    private String prefix = DEFAULT_PREFIX;
    private boolean soundsEnabled = true;
    private boolean particlesEnabled = true;
    private String catchParticle = "SPLASH";
    private String bubbleParticle = "BUBBLE_POP";
    private String successParticle = "HAPPY_VILLAGER";
    private String xpParticle = "HAPPY_VILLAGER";
    
    // Message Templates (MiniMessage format) - User configurable
    private String msgLegendary = "<gold><bold>✨ LEGENDARY CATCH! ✨</bold></gold>\n<yellow>You caught <gold><bold>{amount}x {item}</bold></gold>!";
    private String msgRare = "<aqua><bold>★ Rare Catch! ★</bold></aqua>\n<dark_aqua>You caught <aqua><bold>{amount}x {item}</bold></aqua>!";
    private String msgUncommon = "<green><bold>♦ Uncommon Catch ♦</bold></green>\n<dark_green>You caught <green><bold>{amount}x {item}</bold></green>!";
    private String msgCommon = "<gray>You caught <white><bold>{amount}x {item}</bold></white>!";
    
    private boolean biomeDropsEnabled = true;
    private boolean statisticsEnabled = true;
    private boolean permissionsEnabled = false;
    private boolean debugEnabled = false;
    private boolean dropToInventory = false;
    private int statsSaveInterval = 600;
    private int hookCleanupInterval = 300;
    private String language = "en_US";
    private String profile = "balanced";

    private static final int MIN_STATS_INTERVAL = 60;
    private static final int MAX_STATS_INTERVAL = 3600;
    private static final int MIN_HOOK_INTERVAL = 60;
    private static final int MAX_HOOK_INTERVAL = 1800;

    public ConfigManager(MythicRodPlugin plugin, PlatformConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        validateAndCache();
    }

    public void reload(PlatformConfiguration newConfig) {
        this.config = newConfig;
        validateAndCache();
    }

    private void validateAndCache() {
        if (config == null) {
            plugin.getLogger().warning("Config is null, using all defaults");
            return;
        }

        int configVersion = config.getInt("config-version", 0);
        if (configVersion < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().info("Config version " + configVersion + " is outdated. Current version: " + CURRENT_CONFIG_VERSION);
        }
        // HIGH-006 FIX: soundsEnabled was field-initialised to true but NEVER read from config.
        soundsEnabled = config.getBoolean("features.sounds.enabled", true);
        particlesEnabled = config.getBoolean("features.particles.enabled", true);
        catchParticle = config.getString("features.particles.catch-particle", "SPLASH");
        bubbleParticle = config.getString("features.particles.bubble-particle", "BUBBLE_POP");
        successParticle = config.getString("features.particles.success-particle", "HAPPY_VILLAGER");
        xpParticle = config.getString("features.particles.xp-particle", "HAPPY_VILLAGER");
        
        // Load message templates
        msgLegendary = config.getString("messages.catch.legendary", msgLegendary);
        msgRare = config.getString("messages.catch.rare", msgRare);
        msgUncommon = config.getString("messages.catch.uncommon", msgUncommon);
        msgCommon = config.getString("messages.catch.common", msgCommon);
        
        biomeDropsEnabled = config.getBoolean("features.drops.biome-specific.enabled", true);
        statisticsEnabled = config.getBoolean("features.statistics.enabled", true);
        permissionsEnabled = config.getBoolean("features.permissions.enabled", false);
        debugEnabled = config.getBoolean("features.debug.enabled", false);
        dropToInventory = config.getBoolean("features.drops.drop-to-inventory", false);

        prefix = config.getString("ui.prefix", DEFAULT_PREFIX);
        if (prefix == null || prefix.isEmpty()) {
            prefix = DEFAULT_PREFIX;
        }

        String rawLang = config.getString("language.default", "en_US");
        language = isValidLanguage(rawLang) ? rawLang : "en_US";

        String rawProfile = config.getString("profile", "balanced");
        profile = isValidProfile(rawProfile) ? rawProfile : "balanced";

        int rawStats = config.getInt("timers.stats-save-interval-seconds", 600);
        statsSaveInterval = clampInterval(rawStats, MIN_STATS_INTERVAL, MAX_STATS_INTERVAL);

        int rawHook = config.getInt("timers.hook-cleanup-interval-seconds", 300);
        hookCleanupInterval = clampInterval(rawHook, MIN_HOOK_INTERVAL, MAX_HOOK_INTERVAL);
    }

    private boolean isValidLanguage(String lang) {
        return lang != null && !lang.isEmpty() && lang.matches("[a-z]{2}_[A-Z]{2}");
    }

    private boolean isValidProfile(String profile) {
        return profile != null && (profile.equals("balanced") || profile.equals("generous") || profile.equals("scarce"));
    }

    private int clampInterval(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    // HIGH-007 FIX: Expose message templates so FishingListener (and other callers)
    // use the user-configurable, locale-neutral templates instead of hardcoded English strings.
    public String getMsgLegendary() { return msgLegendary; }
    public String getMsgRare()      { return msgRare; }
    public String getMsgUncommon()  { return msgUncommon; }
    public String getMsgCommon()    { return msgCommon; }

    public String getPrefix() { return prefix; }
    public boolean useSounds() { return soundsEnabled; }
    public boolean useParticles() { return particlesEnabled; }
    public String getCatchParticle() { return catchParticle; }
    public String getBubbleParticle() { return bubbleParticle; }
    public String getSuccessParticle() { return successParticle; }
    public String getXpParticle() { return xpParticle; }
    
    public void setCatchParticle(String particle) { this.catchParticle = particle; }
    public void setBubbleParticle(String particle) { this.bubbleParticle = particle; }
    public void setSuccessParticle(String particle) { this.successParticle = particle; }
    public void setXpParticle(String particle) { this.xpParticle = particle; }
    public boolean enableBiomeSpecificDrops() { return biomeDropsEnabled; }
    public boolean trackStatistics() { return statisticsEnabled; }
    public boolean usePermissions() { return permissionsEnabled; }
    public boolean isDebugMode() { return debugEnabled; }
    public boolean dropToInventory() { return dropToInventory; }
    public int getStatsSaveInterval() { return statsSaveInterval; }
    public int getHookCleanupInterval() { return hookCleanupInterval; }
    public String getLanguage() { return language; }
    public String getProfile() { return profile; }
    
    public void setSoundsEnabled(boolean enabled) { 
        this.soundsEnabled = enabled; 
        if (config != null) config.set("features.sounds.enabled", enabled);
    }
    public void setParticlesEnabled(boolean enabled) { 
        this.particlesEnabled = enabled; 
        if (config != null) config.set("features.particles.enabled", enabled);
    }
    public void setBiomeDropsEnabled(boolean enabled) { 
        this.biomeDropsEnabled = enabled; 
        if (config != null) config.set("features.drops.biome-specific.enabled", enabled);
    }
    public void setStatisticsEnabled(boolean enabled) { 
        this.statisticsEnabled = enabled; 
        if (config != null) config.set("features.statistics.enabled", enabled);
    }
    public void setPermissionsEnabled(boolean enabled) { 
        this.permissionsEnabled = enabled; 
        if (config != null) config.set("features.permissions.enabled", enabled);
    }
    public void setDebugEnabled(boolean enabled) { 
        this.debugEnabled = enabled; 
        if (config != null) config.set("features.debug.enabled", enabled);
    }
    public void setDropToInventory(boolean enabled) { 
        this.dropToInventory = enabled; 
        if (config != null) config.set("features.drops.drop-to-inventory", enabled);
    }
    public void setStatsSaveInterval(int seconds) { 
        this.statsSaveInterval = clampInterval(seconds, MIN_STATS_INTERVAL, MAX_STATS_INTERVAL); 
        if (config != null) config.set("timers.stats-save-interval-seconds", statsSaveInterval);
    }
    public void setHookCleanupInterval(int seconds) { 
        this.hookCleanupInterval = clampInterval(seconds, MIN_HOOK_INTERVAL, MAX_HOOK_INTERVAL); 
        if (config != null) config.set("timers.hook-cleanup-interval-seconds", hookCleanupInterval);
    }
    
    private File configFile;
    
    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }
    
    public void saveConfig() throws Exception {
        if (config != null && configFile != null) {
            config.save(configFile);
        } else {
            plugin.getLogger().warning("Cannot save config: config=" + (config != null) + ", configFile=" + (configFile != null));
        }
    }
    
    public PlatformConfiguration getConfig() { return config; }
    
    private PlatformConfiguration statsConfig;
    private File statsFile;
    
    public void setStatsConfig(File statsFile, PlatformConfiguration statsConfig) {
        this.statsFile = statsFile;
        this.statsConfig = statsConfig;
    }
    
    public PlatformConfiguration getStatsConfig() {
        return statsConfig;
    }
    
    public void saveStats() {
        if (statsConfig != null && statsFile != null) {
            try {
                statsConfig.save(statsFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save stats config: " + e.getMessage());
            }
        }
    }
}
