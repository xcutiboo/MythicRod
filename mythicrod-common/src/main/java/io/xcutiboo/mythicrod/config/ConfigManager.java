package io.xcutiboo.mythicrod.config;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.internal.runtime.MythicRodRuntime;

public class ConfigManager {

    private final MythicRodRuntime runtime;
    private PlatformConfiguration config;
    private static final int CURRENT_CONFIG_VERSION = 8;

    private static final String DEFAULT_PREFIX = "<gold><bold>[MythicRod]</bold></gold> ";
    private static final String DEFAULT_MSG_LEGENDARY = "<gold><bold>✨ LEGENDARY CATCH! ✨</bold></gold>\n<yellow>You caught <gold><bold>{amount}x {item}</bold></gold>!";
    private static final String DEFAULT_MSG_RARE = "<aqua><bold>★ Rare Catch! ★</bold></aqua>\n<dark_aqua>You caught <aqua><bold>{amount}x {item}</bold></aqua>!";
    private static final String DEFAULT_MSG_UNCOMMON = "<green><bold>♦ Uncommon Catch ♦</bold></green>\n<dark_green>You caught <green><bold>{amount}x {item}</bold></green>!";
    private static final String DEFAULT_MSG_COMMON = "<gray>You caught <white><bold>{amount}x {item}</bold></white>!";

    private static final String DEFAULT_CATCH_PARTICLE = "SPLASH";
    private static final String DEFAULT_BUBBLE_PARTICLE = "BUBBLE_POP";
    private static final String DEFAULT_SUCCESS_PARTICLE = "HAPPY_VILLAGER";
    private static final String DEFAULT_XP_PARTICLE = "HAPPY_VILLAGER";
    private static final String DEFAULT_LANGUAGE = "en_US";
    private static final String DEFAULT_PROFILE = "balanced";

    private String prefix = DEFAULT_PREFIX;
    private boolean soundsEnabled = true;
    private boolean particlesEnabled = true;
    private String catchParticle = DEFAULT_CATCH_PARTICLE;
    private String bubbleParticle = DEFAULT_BUBBLE_PARTICLE;
    private String successParticle = DEFAULT_SUCCESS_PARTICLE;
    private String xpParticle = DEFAULT_XP_PARTICLE;

    private String msgLegendary = DEFAULT_MSG_LEGENDARY;
    private String msgRare = DEFAULT_MSG_RARE;
    private String msgUncommon = DEFAULT_MSG_UNCOMMON;
    private String msgCommon = DEFAULT_MSG_COMMON;

    private boolean biomeDropsEnabled = true;
    private boolean statisticsEnabled = true;
    private boolean permissionsEnabled = true;
    private boolean debugEnabled = false;
    private RewardDeliveryMode rewardDeliveryMode = RewardDeliveryMode.VANILLA_RETRIEVE;
    private double basicRodLuckMultiplier = 1.0D;
    private double advancedRodLuckMultiplier = 1.25D;
    private double legendaryRodLuckMultiplier = 1.5D;
    private int statsSaveInterval = 600;
    private String language = DEFAULT_LANGUAGE;
    private String profile = DEFAULT_PROFILE;

    private static final int MIN_STATS_INTERVAL = 60;
    private static final int MAX_STATS_INTERVAL = 3600;
    private static final double MIN_ROD_LUCK_MULTIPLIER = 0.01D;
    private static final double MAX_ROD_LUCK_MULTIPLIER = 10.0D;

    public ConfigManager(MythicRodRuntime runtime, PlatformConfiguration config) {
        this.runtime = runtime;
        this.config = config;
        validateAndCache();
    }

    public void reload(PlatformConfiguration newConfig) {
        this.config = newConfig;
        validateAndCache();
    }

    private Logger logger() {
        return runtime.getLogger();
    }

    private void validateAndCache() {
        if (config == null) {
            logger().severe("Config is null! Plugin may not function correctly. Using all defaults.");
            resetToDefaults();
            return;
        }

        try {
            int configVersion = config.getInt("config-version", 0);
            if (configVersion < CURRENT_CONFIG_VERSION) {
                logger().log(Level.INFO, () -> "Config version " + configVersion
                    + " is outdated. Current version: " + CURRENT_CONFIG_VERSION);
            }

            soundsEnabled = config.getBoolean("features.sounds.enabled", true);
            particlesEnabled = config.getBoolean("features.particles.enabled", true);

            catchParticle = validateParticle(config.getString("features.particles.catch-particle", DEFAULT_CATCH_PARTICLE), DEFAULT_CATCH_PARTICLE);
            bubbleParticle = validateParticle(config.getString("features.particles.bubble-particle", DEFAULT_BUBBLE_PARTICLE), DEFAULT_BUBBLE_PARTICLE);
            successParticle = validateParticle(config.getString("features.particles.success-particle", DEFAULT_SUCCESS_PARTICLE), DEFAULT_SUCCESS_PARTICLE);
            xpParticle = validateParticle(config.getString("features.particles.xp-particle", DEFAULT_XP_PARTICLE), DEFAULT_XP_PARTICLE);

            msgLegendary = defaultCatchTemplate(config.getString("messages.catch.legendary", DEFAULT_MSG_LEGENDARY), DEFAULT_MSG_LEGENDARY);
            msgRare = defaultCatchTemplate(config.getString("messages.catch.rare", DEFAULT_MSG_RARE), DEFAULT_MSG_RARE);
            msgUncommon = defaultCatchTemplate(config.getString("messages.catch.uncommon", DEFAULT_MSG_UNCOMMON), DEFAULT_MSG_UNCOMMON);
            msgCommon = defaultCatchTemplate(config.getString("messages.catch.common", DEFAULT_MSG_COMMON), DEFAULT_MSG_COMMON);

            biomeDropsEnabled = config.getBoolean("features.drops.biome-specific.enabled", true);
            statisticsEnabled = config.getBoolean("features.statistics.enabled", true);
            permissionsEnabled = config.getBoolean("features.permissions.enabled", true);
            debugEnabled = config.getBoolean("features.debug.enabled", false);
            rewardDeliveryMode = resolveRewardDeliveryMode();
            basicRodLuckMultiplier = resolveRodLuckMultiplier("basic", 1.0D);
            advancedRodLuckMultiplier = resolveRodLuckMultiplier("advanced", 1.25D);
            legendaryRodLuckMultiplier = resolveRodLuckMultiplier("legendary", 1.5D);

            prefix = config.getString("ui.prefix", DEFAULT_PREFIX);
            if (prefix == null || prefix.isEmpty()) {
                logger().warning("Prefix is empty, using default");
                prefix = DEFAULT_PREFIX;
            } else if (prefix.length() > 100) {
                logger().warning("Prefix is too long (max 100 chars), truncating");
                prefix = prefix.substring(0, 100);
            }

            String rawLang = config.getString("language.default", DEFAULT_LANGUAGE);
            language = isValidLanguage(rawLang) ? rawLang : DEFAULT_LANGUAGE;
            if (!language.equals(rawLang) && rawLang != null && !rawLang.isEmpty()) {
                logger().log(Level.WARNING, () -> "Invalid language format: " + rawLang + ", using en_US");
            }

            String rawProfile = config.getString("profile", DEFAULT_PROFILE);
            String normalizedProfile = normalizeProfile(rawProfile);
            profile = normalizedProfile != null ? normalizedProfile : DEFAULT_PROFILE;
            if (normalizedProfile == null && rawProfile != null && !rawProfile.isEmpty()) {
                logger().log(Level.WARNING, () -> "Invalid profile: " + rawProfile + ", using balanced");
            }

            int rawStats = config.getInt("timers.stats-save-interval-seconds", 600);
            statsSaveInterval = clampInterval(rawStats, MIN_STATS_INTERVAL, MAX_STATS_INTERVAL);
            if (rawStats != statsSaveInterval && rawStats > 0) {
                logger().log(Level.WARNING, () -> "Stats save interval clamped from " + rawStats
                    + " to " + statsSaveInterval + " seconds");
            }
        } catch (RuntimeException e) {
            logger().log(Level.SEVERE, "Error validating configuration", e);
            logger().warning("Using safe defaults - please check your configuration file");
            resetToDefaults();
        }
    }

    private String validateParticle(String particle, String fallback) {
        if (particle == null || particle.isEmpty()) {
            return fallback;
        }
        if (!particle.matches("[A-Z_]+")) {
            logger().log(Level.WARNING, () -> "Invalid particle name: " + particle + ", using " + fallback);
            return fallback;
        }
        return particle;
    }

    private String defaultCatchTemplate(String configuredTemplate, String fallback) {
        if (configuredTemplate == null || configuredTemplate.isBlank()) {
            return fallback;
        }
        return configuredTemplate;
    }

    private RewardDeliveryMode resolveRewardDeliveryMode() {
        String rawMode = config.getString("features.drops.delivery-mode", null);
        RewardDeliveryMode configuredMode = RewardDeliveryMode.fromConfigValue(rawMode);
        if (configuredMode != null) {
            return configuredMode;
        }

        if (rawMode != null && !rawMode.isBlank()) {
            logger().log(Level.WARNING, () -> "Invalid reward delivery mode: " + rawMode + ", using vanilla_retrieve");
        }

        return RewardDeliveryMode.VANILLA_RETRIEVE;
    }

    private void resetToDefaults() {
        prefix = DEFAULT_PREFIX;
        soundsEnabled = true;
        particlesEnabled = true;
        catchParticle = DEFAULT_CATCH_PARTICLE;
        bubbleParticle = DEFAULT_BUBBLE_PARTICLE;
        successParticle = DEFAULT_SUCCESS_PARTICLE;
        xpParticle = DEFAULT_XP_PARTICLE;
        msgLegendary = DEFAULT_MSG_LEGENDARY;
        msgRare = DEFAULT_MSG_RARE;
        msgUncommon = DEFAULT_MSG_UNCOMMON;
        msgCommon = DEFAULT_MSG_COMMON;
        biomeDropsEnabled = true;
        statisticsEnabled = true;
        permissionsEnabled = true;
        debugEnabled = false;
        rewardDeliveryMode = RewardDeliveryMode.VANILLA_RETRIEVE;
        basicRodLuckMultiplier = 1.0D;
        advancedRodLuckMultiplier = 1.25D;
        legendaryRodLuckMultiplier = 1.5D;
        language = DEFAULT_LANGUAGE;
        profile = DEFAULT_PROFILE;
        statsSaveInterval = 600;
    }

    private double resolveRodLuckMultiplier(String tier, double fallback) {
        String path = "features.rods.luck-multipliers." + tier;
        double configuredValue = config.getDouble(path, fallback);
        if (!Double.isFinite(configuredValue)) {
            logger().log(Level.WARNING, () -> "Invalid rod luck multiplier at " + path + ": "
                + configuredValue + ", using " + fallback);
            return fallback;
        }

        double clamped = Math.clamp(configuredValue, MIN_ROD_LUCK_MULTIPLIER, MAX_ROD_LUCK_MULTIPLIER);
        if (configuredValue != clamped) {
            logger().log(Level.WARNING, () -> "Rod luck multiplier at " + path + " clamped from "
                + configuredValue + " to " + clamped);
        }
        return clamped;
    }

    private String updateParticleSetting(String path, String particle, String fallback) {
        String validatedParticle = validateParticle(particle, fallback);
        if (config != null) {
            config.set(path, validatedParticle);
        }
        return validatedParticle;
    }

    private boolean isValidLanguage(String lang) {
        return lang != null && !lang.isEmpty() && lang.matches("[a-z]{2}_[A-Z]{2}");
    }

    private String normalizeProfile(String rawProfile) {
        if (rawProfile == null || rawProfile.isBlank()) {
            return null;
        }

        return switch (rawProfile.trim().toLowerCase(Locale.ROOT)) {
            case "lightweight", DEFAULT_PROFILE, "performance" -> rawProfile.trim().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private int clampInterval(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

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

    public void setCatchParticle(String particle) {
        this.catchParticle = updateParticleSetting("features.particles.catch-particle", particle, DEFAULT_CATCH_PARTICLE);
    }
    public void setBubbleParticle(String particle) {
        this.bubbleParticle = updateParticleSetting("features.particles.bubble-particle", particle, DEFAULT_BUBBLE_PARTICLE);
    }
    public void setSuccessParticle(String particle) {
        this.successParticle = updateParticleSetting("features.particles.success-particle", particle, DEFAULT_SUCCESS_PARTICLE);
    }
    public void setXpParticle(String particle) {
        this.xpParticle = updateParticleSetting("features.particles.xp-particle", particle, DEFAULT_XP_PARTICLE);
    }
    public boolean enableBiomeSpecificDrops() { return biomeDropsEnabled; }
    public boolean trackStatistics() { return statisticsEnabled; }
    public boolean usePermissions() { return permissionsEnabled; }
    public boolean isDebugMode() { return debugEnabled; }
    public RewardDeliveryMode getRewardDeliveryMode() { return rewardDeliveryMode; }
    public double getRodLuckMultiplier(String tier) {
        if (tier == null || tier.isBlank()) {
            return basicRodLuckMultiplier;
        }

        return switch (tier.trim().toLowerCase(Locale.ROOT)) {
            case "advanced" -> advancedRodLuckMultiplier;
            case "legendary" -> legendaryRodLuckMultiplier;
            default -> basicRodLuckMultiplier;
        };
    }
    public int getStatsSaveInterval() { return statsSaveInterval; }
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
    public void setRewardDeliveryMode(RewardDeliveryMode mode) {
        this.rewardDeliveryMode = mode != null ? mode : RewardDeliveryMode.VANILLA_RETRIEVE;
        if (config != null) {
            config.set("features.drops.delivery-mode", rewardDeliveryMode.getConfigValue());
        }
    }
    public void setStatsSaveInterval(int seconds) {
        this.statsSaveInterval = clampInterval(seconds, MIN_STATS_INTERVAL, MAX_STATS_INTERVAL);
        if (config != null) config.set("timers.stats-save-interval-seconds", statsSaveInterval);
    }

    private File configFile;

    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    public void saveConfig() throws IOException {
        if (config != null && configFile != null) {
            writeMissingCurrentDefaults();
            config.set("config-version", CURRENT_CONFIG_VERSION);
            config.save(configFile);
        } else {
            logger().log(Level.WARNING, () -> "Cannot save config: config=" + (config != null)
                + ", configFile=" + (configFile != null));
        }
    }

    private void writeMissingCurrentDefaults() {
        setIfMissing("features.rods.luck-multipliers.basic", basicRodLuckMultiplier);
        setIfMissing("features.rods.luck-multipliers.advanced", advancedRodLuckMultiplier);
        setIfMissing("features.rods.luck-multipliers.legendary", legendaryRodLuckMultiplier);
    }

    private void setIfMissing(String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
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
            } catch (IOException e) {
                logger().log(Level.WARNING, "Failed to save stats config", e);
            }
        }
    }
}
