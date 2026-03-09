package io.xcutiboo.mythicrod.config;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;

/**
 * Stores per-player preferences like language overrides.
 */
public class PlayerPreferences {
    private final MythicRodPlugin plugin;
    private final File file;
    private PlatformConfiguration cfg;

    public PlayerPreferences(MythicRodPlugin plugin) {
        this.plugin = plugin;
        File dir = plugin.getDataFolder();
        this.file = new File(dir, "players.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create players.yml: " + e.getMessage());
            }
        }
        this.cfg = plugin.getPlatform().loadConfiguration(file);
    }

    public String getLanguage(UUID playerId) {
        return cfg.getString("players." + playerId + ".language", null);
    }

    public void setLanguage(UUID playerId, String locale) {
        cfg.set("players." + playerId + ".language", locale);
        try {
            cfg.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save players.yml: " + e.getMessage());
        }
    }
}
