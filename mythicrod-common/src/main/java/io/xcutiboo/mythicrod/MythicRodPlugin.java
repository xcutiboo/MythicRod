package io.xcutiboo.mythicrod;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.LanguageManager;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;

public interface MythicRodPlugin {

    ConfigManager getConfigManager();
    DropManager getDropManager();
    StatisticsManager getStatisticsManager();
    LanguageManager getLanguageManager();
    
    PlatformServer getPlatform();

    void reload();

    void sendFormattedMessage(PlatformPlayer player, String message);

    Logger getLogger();

    File getDataFolder();

    InputStream getResource(String filename);

    void saveResource(String resourcePath, boolean replace);
    
    void saveDefaultConfig();
    
    PlatformConfiguration loadConfig(File file);
    
    PlatformConfiguration createEmptyConfig();
}
