package io.xcutiboo.mythicrod.paper.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.di.CommonModule;
import io.xcutiboo.mythicrod.fishing.EffectsService;
import io.xcutiboo.mythicrod.fishing.FishingService;
import io.xcutiboo.mythicrod.fishing.RewardService;
import io.xcutiboo.mythicrod.gui.GUIManager;
import io.xcutiboo.mythicrod.paper.commands.BrigadierCommandManager;
import io.xcutiboo.mythicrod.paper.fishing.FishingListener;
import io.xcutiboo.mythicrod.paper.fishing.PaperEffectsService;
import org.bstats.bukkit.Metrics;

public class PaperModule extends AbstractModule {
    private final MythicRod plugin;

    public PaperModule(MythicRod plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        install(new CommonModule(plugin));
        bind(MythicRod.class).toInstance(plugin);
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
    BrigadierCommandManager provideCommandManager(MythicRod plugin) {
        BrigadierCommandManager manager = new BrigadierCommandManager(plugin);
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
        return new PaperEffectsService(plugin);
    }
}
