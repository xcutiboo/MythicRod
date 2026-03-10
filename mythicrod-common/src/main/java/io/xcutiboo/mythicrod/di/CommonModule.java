package io.xcutiboo.mythicrod.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.LanguageManager;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.fishing.FishingService;
import io.xcutiboo.mythicrod.fishing.RewardService;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;

/**
 * Common DI module containing cross-platform providers.
 * Platform-specific modules should install this.
 */
public class CommonModule extends AbstractModule {
    private final MythicRodPlugin plugin;

    public CommonModule(MythicRodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(MythicRodPlugin.class).toInstance(plugin);
    }

    @Provides
    @Singleton
    public ConfigManager provideConfigManager(MythicRodPlugin plugin) {
        ConfigManager manager = new ConfigManager(plugin);
        manager.initialize();
        return manager;
    }

    @Provides
    @Singleton
    public LanguageManager provideLanguageManager(MythicRodPlugin plugin, ConfigManager configManager) {
        return new LanguageManager(plugin, configManager);
    }

    @Provides
    @Singleton
    public DropManager provideDropManager(MythicRodPlugin plugin, ConfigManager configManager) {
        DropManager manager = new DropManager(plugin);
        manager.loadDrops(configManager.getStatsConfig()); // Need a separate getter for drops, using platform config
        return manager;
    }

    @Provides
    @Singleton
    public StatisticsManager provideStatisticsManager(MythicRodPlugin plugin) {
        StatisticsManager manager = new StatisticsManager(plugin);
        manager.initialize();
        return manager;
    }

    @Provides
    @Singleton
    public FishingService provideFishingService(MythicRodPlugin plugin) {
        return new FishingService(plugin);
    }

    @Provides
    @Singleton
    public RewardService provideRewardService(MythicRodPlugin plugin) {
        return new RewardService(plugin);
    }
}