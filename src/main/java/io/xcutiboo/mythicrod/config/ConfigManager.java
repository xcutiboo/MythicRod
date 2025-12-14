package io.xcutiboo.mythicrod.config;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import io.xcutiboo.mythicrod.MythicRod;
public class ConfigManager {
    private final MythicRod plugin;
    private FileConfiguration config;
    private FileConfiguration dropsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration statsConfig;
    private File configFile;
    private File dropsFile;
    private File messagesFile;
    private File statsFile;
    private String prefix = "&6&l[MythicRod] &r";
    private boolean useSounds = true;
    private boolean useParticles = true;
    private boolean enableBiomeSpecificDrops = true;
    private boolean trackStatistics = true;
    private boolean usePermissions = false;
    private boolean debugMode = false;
    private int statsSaveInterval = 600;
    private int hookCleanupInterval = 300;
    public ConfigManager(MythicRod plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        loadMainConfig();
        loadDropsConfig();
        loadMessagesConfig();
        loadStatsConfig();
    }
    public void reload() {
        loadMainConfig();
        loadDropsConfig();
        loadMessagesConfig();
        if (trackStatistics) {
            loadStatsConfig();
        }
        plugin.getLogger().info("Configuration reloaded successfully!");
    }
    private void loadMainConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }
        prefix = config.getString("prefix", prefix);
        useSounds = config.getBoolean("use-sounds", useSounds);
        useParticles = config.getBoolean("use-particles", useParticles);
        enableBiomeSpecificDrops = config.getBoolean("enable-biome-specific-drops", enableBiomeSpecificDrops);
        trackStatistics = config.getBoolean("track-statistics", trackStatistics);
        usePermissions = config.getBoolean("use-permissions", usePermissions);
        debugMode = config.getBoolean("debug-mode", debugMode);
        statsSaveInterval = config.getInt("stats-save-interval", statsSaveInterval);
        hookCleanupInterval = config.getInt("hook-cleanup-interval", hookCleanupInterval);
    }
    private void loadDropsConfig() {
        dropsFile = new File(plugin.getDataFolder(), "drops.yml");
        if (!dropsFile.exists()) {
            try {
                dropsFile.createNewFile();
                YamlConfiguration defaultDropsConfig = new YamlConfiguration();
                // Migrate old drops section to new file if present
                if (config.contains("drops")) {
                    defaultDropsConfig.set("drops.global", config.getStringList("drops"));
                    defaultDropsConfig.save(dropsFile);
                } else {
                    plugin.saveResource("drops.yml", true);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create drops.yml", e);
                plugin.saveResource("drops.yml", true);
            }
        }
        dropsConfig = YamlConfiguration.loadConfiguration(dropsFile);
    }
    private void loadMessagesConfig() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    private void loadStatsConfig() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
                YamlConfiguration defaultStats = new YamlConfiguration();
                defaultStats.save(statsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create stats.yml", e);
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }
    public void saveStats() {
        if (statsFile == null || statsConfig == null) {
            return;
        }
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save stats.yml", e);
        }
    }
    public FileConfiguration getConfig() {
        return config;
    }
    public FileConfiguration getDropsConfig() {
        return dropsConfig;
    }
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    public FileConfiguration getStatsConfig() {
        return statsConfig;
    }
    public String getPrefix() {
        return prefix;
    }
    public boolean useSounds() {
        return useSounds;
    }
    public boolean useParticles() {
        return useParticles;
    }
    public boolean enableBiomeSpecificDrops() {
        return enableBiomeSpecificDrops;
    }
    public boolean trackStatistics() {
        return trackStatistics;
    }
    public boolean usePermissions() {
        return usePermissions;
    }
    public boolean isDebugMode() {
        return debugMode;
    }
    public int getStatsSaveInterval() {
        return statsSaveInterval;
    }
    public int getHookCleanupInterval() {
        return hookCleanupInterval;
    }
public void setUseSounds(boolean value) {
        this.useSounds = value;
        config.set("use-sounds", value);
    }
    public void setUseParticles(boolean value) {
        this.useParticles = value;
        config.set("use-particles", value);
    }
    public void setEnableBiomeSpecificDrops(boolean value) {
        this.enableBiomeSpecificDrops = value;
        config.set("enable-biome-specific-drops", value);
    }
    public void setTrackStatistics(boolean value) {
        this.trackStatistics = value;
        config.set("track-statistics", value);
    }
    public void setUsePermissions(boolean value) {
        this.usePermissions = value;
        config.set("use-permissions", value);
    }
    public void setDebugMode(boolean value) {
        this.debugMode = value;
        config.set("debug-mode", value);
    }
    public void setStatsSaveInterval(int value) {
        this.statsSaveInterval = Math.max(60, value);
        config.set("stats-save-interval", this.statsSaveInterval);
    }
    public void setHookCleanupInterval(int value) {
        this.hookCleanupInterval = Math.max(60, value);
        config.set("hook-cleanup-interval", this.hookCleanupInterval);
    }

    public void setLanguage(String langCode) {
        if (langCode == null || langCode.isEmpty()) {
            langCode = "en";
        }
        config.set("language", langCode.toLowerCase());
    }

    public void saveConfig() throws IOException {
        if (configFile != null && config != null) {
            config.save(configFile);
        }
    }
}
