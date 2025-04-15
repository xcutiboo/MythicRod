package com.mythicrod.mythicrod;

import com.mythicrod.mythicrod.api.MythicRodAPI;
import com.mythicrod.mythicrod.commands.CommandManager;
import com.mythicrod.mythicrod.config.ConfigManager;
import com.mythicrod.mythicrod.fishing.FishingListener;
import com.mythicrod.mythicrod.drops.DropManager;
import com.mythicrod.mythicrod.metrics.StatisticsManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MythicRod extends JavaPlugin {

    private static MythicRod instance;
    private ConfigManager configManager;
    private DropManager dropManager;
    private StatisticsManager statisticsManager;
    private CommandManager commandManager;
    private MythicRodAPI api;
    private FishingListener fishingListener;

    @Override
    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.dropManager = new DropManager(this);
        this.statisticsManager = new StatisticsManager(this);
        this.commandManager = new CommandManager(this);
        this.fishingListener = new FishingListener(this);
        getServer().getPluginManager().registerEvents(fishingListener, this);
        this.api = new MythicRodAPI(this);
        getServer().getScheduler().runTaskTimer(this,
                fishingListener::cleanupHooks, 6000L, 6000L);
        getLogger().info("✨ MythicRod " + getDescription().getVersion() + " has been enabled! ✨");
    }

    @Override
    public void onDisable() {
        if (statisticsManager != null) {
            statisticsManager.cleanup();
        }
        getServer().getScheduler().cancelTasks(this);
        instance = null;
        getLogger().info("⚡ MythicRod has been disabled! ⚡");
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

    public void reload() {
        configManager.reload();
        dropManager.reload();
        statisticsManager.reload();
    }
}
