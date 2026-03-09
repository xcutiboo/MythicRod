package io.xcutiboo.mythicrod;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.LanguageManager;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;

/**
 * Common plugin interface for platform-independent access.
 */
public interface MythicRodPlugin {

    ConfigManager getConfigManager();
    DropManager getDropManager();
    StatisticsManager getStatisticsManager();
    LanguageManager getLanguageManager();
    
    PlatformServer getPlatform();

    void reload();

    /**
     * Send a formatted message to a player using platform-appropriate method.
     * Paper: Uses Adventure Components
     * Spigot: Uses BukkitAudiences adapter
     *
     * @param player The player to send the message to
     * @param message The message string (supports & color codes)
     */
    void sendFormattedMessage(PlatformPlayer player, String message);

    Logger getLogger();

    File getDataFolder();

    InputStream getResource(String filename);

    void saveResource(String resourcePath, boolean replace);
}
