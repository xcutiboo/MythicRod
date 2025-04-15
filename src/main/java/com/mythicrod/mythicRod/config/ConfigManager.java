package com.mythicrod.mythicrod.config;

import com.mythicrod.mythicrod.MythicRod;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

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

    private String prefix = "&6&l<MythicRod> &r";
    private boolean useSounds = true;
    private boolean useParticles = true;
    private boolean enableBiomeSpecificDrops = true;
    private boolean trackStatistics = true;
    private boolean usePermissions = false;

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

        prefix = ChatColor.translateAlternateColorCodes('&',
                config.getString("prefix", prefix));
        useSounds = config.getBoolean("use-sounds", useSounds);
        useParticles = config.getBoolean("use-particles", useParticles);
        enableBiomeSpecificDrops = config.getBoolean("enable-biome-specific-drops", enableBiomeSpecificDrops);
        trackStatistics = config.getBoolean("track-statistics", trackStatistics);
        usePermissions = config.getBoolean("use-permissions", usePermissions);
    }

    private void loadDropsConfig() {
        dropsFile = new File(plugin.getDataFolder(), "drops.yml");

        if (!dropsFile.exists()) {
            try {
                dropsFile.createNewFile();
                YamlConfiguration dropsConfig = new YamlConfiguration();

                if (config.contains("drops")) {
                    dropsConfig.set("drops.global", config.getStringList("drops"));
                    dropsConfig.save(dropsFile);
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
}
