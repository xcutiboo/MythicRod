package io.xcutiboo.mythicrod.di;

import java.io.File;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.fishing.FishingService;
import io.xcutiboo.mythicrod.fishing.RewardService;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import io.xcutiboo.mythicrod.config.LanguageManager;

public class CommonModule extends AbstractModule {
    private final MythicRodPlugin plugin;
    private final ConfigManager configManager;

    public CommonModule(MythicRodPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    protected void configure() {
        bind(MythicRodPlugin.class).toInstance(plugin);
        bind(ConfigManager.class).toInstance(configManager);
    }

    @Provides
    @Singleton
    LanguageManager provideLanguageManager() {
        return new LanguageManager(plugin, configManager);
    }

    @Provides
    @Singleton
    DropManager provideDropManager() {
        DropManager manager = new DropManager(plugin.getLogger());
        manager.loadDrops(plugin.getPlatform().loadConfiguration(
            new File(plugin.getDataFolder(), "config.yml")));
        return manager;
    }

    @Provides
    @Singleton
    FishingService provideFishingService(DropManager dropManager) {
        return new FishingService(dropManager, plugin.getLogger());
    }

    @Provides
    @Singleton
    RewardService provideRewardService() {
        return new RewardService(
            plugin.getPlatform(),
            new LanguageManager(plugin, configManager),
            plugin.getLogger()
        );
    }

    @Provides
    @Singleton
    StatisticsManager provideStatisticsManager() {
        return new StatisticsManager(plugin);
    }
}
