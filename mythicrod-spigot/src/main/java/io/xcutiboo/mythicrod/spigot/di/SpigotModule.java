package io.xcutiboo.mythicrod.spigot.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.di.CommonModule;
import io.xcutiboo.mythicrod.api.service.EffectsService;
import io.xcutiboo.mythicrod.fishing.FishingService;
import io.xcutiboo.mythicrod.fishing.RewardService;
import io.xcutiboo.mythicrod.spigot.commands.BrigadierStyleCommandManager;
import io.xcutiboo.mythicrod.spigot.fishing.FishingListener;
import io.xcutiboo.mythicrod.spigot.fishing.SpigotEffectsService;
import io.xcutiboo.mythicrod.spigot.gui.GUIManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;

public class SpigotModule extends AbstractModule {
    private final MythicRod plugin;

    public SpigotModule(MythicRod plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        install(new CommonModule(plugin));
        bind(MythicRod.class).toInstance(plugin);
    }

    @Provides
    @Singleton
    BukkitAudiences provideAudiences(MythicRod plugin) {
        return BukkitAudiences.create(plugin);
    }

    @Provides
    @Singleton
    GUIManager provideGUIManager(MythicRod plugin) {
        GUIManager manager = new GUIManager(plugin);
        manager.initialize();
        return manager;
    }

    @Provides
    @Singleton
    BrigadierStyleCommandManager provideCommandManager(MythicRod plugin) {
        BrigadierStyleCommandManager manager = new BrigadierStyleCommandManager(plugin);
        manager.initialize();
        return manager;
    }

    @Provides
    @Singleton
    FishingListener provideFishingListener(MythicRod plugin, FishingService fishingService, RewardService rewardService, EffectsService effectsService) {
        return new FishingListener(plugin, fishingService, rewardService, effectsService);
    }

    @Provides
    @Singleton
    Metrics provideMetrics(MythicRod plugin) {
        return new Metrics(plugin, 23847);
    }

    @Provides
    @Singleton
    EffectsService provideEffectsService(MythicRod plugin) {
        return new SpigotEffectsService(plugin);
    }
}
