package io.xcutiboo.mythicrod;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.commands.CommandManager;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.LanguageManager;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.fishing.FishingListener;
import io.xcutiboo.mythicrod.gui.GUIManager;
import io.xcutiboo.mythicrod.gui.menus.ConfigMenu;
import io.xcutiboo.mythicrod.gui.menus.DropsMenu;
import io.xcutiboo.mythicrod.gui.menus.StatsMenu;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
public final class MythicRod extends JavaPlugin {
    private static MythicRod instance;
    private ConfigManager configManager;
    private DropManager dropManager;
    private StatisticsManager statisticsManager;
    private CommandManager commandManager;
    private GUIManager guiManager;
    private MythicRodAPI api;
    private FishingListener fishingListener;
    private LanguageManager languageManager;
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;
        try {
            this.configManager = new ConfigManager(this);
            this.dropManager = new DropManager(this);
            this.statisticsManager = new StatisticsManager(this);
            this.commandManager = new CommandManager(this);
            this.fishingListener = new FishingListener(this);
            this.languageManager = new LanguageManager(this, this.configManager);
            this.dropManager.initialize();
            this.statisticsManager.initialize();
            this.commandManager.initialize();
            this.guiManager = new GUIManager(this);
            this.guiManager.initialize();
            registerGUIMenus();
            getServer().getPluginManager().registerEvents(fishingListener, this);
            this.api = new MythicRodAPI(this);
            // Cleanup old fishing hooks to prevent memory leaks
            long cleanupIntervalTicks = configManager.getHookCleanupInterval() * 20L;
            getServer().getScheduler().runTaskTimer(
                this,
                fishingListener::cleanupHooks,
                cleanupIntervalTicks,
                cleanupIntervalTicks
            );
            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("✨ MythicRod " + getPluginMeta().getVersion() +
                " enabled in " + loadTime + "ms ✨");
            getLogger().info("Loaded " + dropManager.getTotalDropCount() + " custom drops");

        } catch (RuntimeException e) {
            getLogger().log(Level.SEVERE, "Failed to enable MythicRod", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (statisticsManager != null) {
                statisticsManager.cleanup();
                getLogger().info("Statistics saved successfully");
            }

            if (guiManager != null) {
                guiManager.shutdown();
            }

            getServer().getScheduler().cancelTasks(this);

            instance = null;
            getLogger().info("⚡ MythicRod has been disabled! ⚡");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown", e);
        }
    }

    private void registerGUIMenus() {
        guiManager.registerMenu("config", ConfigMenu::new);
        guiManager.registerMenu("drops", DropsMenu::new);
        guiManager.registerMenu("stats", StatsMenu::new);
        getLogger().info("Registered 3 GUI menus");
    }

    public static MythicRod getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DropManager getDropManager() {
        return dropManager;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public MythicRodAPI getAPI() {
        return api;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public void reload() {
        try {
            getLogger().info("Reloading configuration...");
            configManager.reload();
            if (languageManager != null) {
                languageManager.setLanguage(configManager.getConfig().getString("language", "en"));
            }
            dropManager.reload();
            statisticsManager.reload();
            getLogger().info("Configuration reloaded successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error reloading configuration", e);
            throw new RuntimeException("Failed to reload configuration", e);
        }
    }
}
