package io.xcutiboo.mythicrod;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.LanguageManager;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.gui.GUIManager;
import io.xcutiboo.mythicrod.gui.menus.ConfigMenu;
import io.xcutiboo.mythicrod.gui.menus.DropsMenu;
import io.xcutiboo.mythicrod.gui.menus.LanguageSwitchMenu;
import io.xcutiboo.mythicrod.gui.menus.MainHubMenu;
import io.xcutiboo.mythicrod.gui.menus.StatsMenu;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import io.xcutiboo.mythicrod.paper.commands.BrigadierCommandManager;
import io.xcutiboo.mythicrod.paper.di.PaperModule;
import io.xcutiboo.mythicrod.paper.fishing.FishingListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bstats.bukkit.Metrics;

public final class MythicRod extends JavaPlugin implements MythicRodPlugin {
    private static final int BSTATS_PLUGIN_ID = 23847;
    
    private Injector injector;
    private ConfigManager configManager;
    private DropManager dropManager;
    private StatisticsManager statisticsManager;
    private GUIManager guiManager;
    private LanguageManager languageManager;
    private BrigadierCommandManager commandManager;
    private FishingListener fishingListener;
    private MythicRodAPI api;
    private Metrics metrics;

    @Override
    public void onLoad() {
        getLogger().info("MythicRod-Paper loading (Brigadier-enabled)...");
    }

    @Override
    public void onEnable() {
        long start = System.nanoTime();
        try {
            injector = Guice.createInjector(new PaperModule(this));
            
            this.configManager = injector.getInstance(ConfigManager.class);
            this.languageManager = injector.getInstance(LanguageManager.class);
            this.dropManager = injector.getInstance(DropManager.class);
            this.statisticsManager = injector.getInstance(StatisticsManager.class);
            this.commandManager = injector.getInstance(BrigadierCommandManager.class);
            this.fishingListener = injector.getInstance(FishingListener.class);
            this.guiManager = injector.getInstance(GUIManager.class);
            this.api = injector.getInstance(MythicRodAPI.class);
            this.metrics = injector.getInstance(Metrics.class);
            
            getServer().getPluginManager().registerEvents(fishingListener, this);
            
            guiManager.registerMenu("main", MainHubMenu::new);
            guiManager.registerMenu("config", ConfigMenu::new);
            guiManager.registerMenu("drops", DropsMenu::new);
            guiManager.registerMenu("stats", StatsMenu::new);
            guiManager.registerMenu("language", LanguageSwitchMenu::new);
            
            setupMetricsCharts();
            
            long cleanupIntervalTicks = configManager.getHookCleanupInterval() * 20L;
            getServer().getScheduler().runTaskTimer(
                this,
                fishingListener::cleanupHooks,
                cleanupIntervalTicks,
                cleanupIntervalTicks
            );

            long ms = (System.nanoTime() - start) / 1_000_000;
            Component banner = Component.text()
                .append(Component.text("MythicRod-Paper ", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("v" + getPluginMeta().getVersion(), NamedTextColor.GREEN))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Paper 1.21.4", NamedTextColor.YELLOW))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Brigadier ✓", NamedTextColor.GREEN))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(ms + "ms", NamedTextColor.YELLOW))
                .build();
            getServer().getConsoleSender().sendMessage(banner);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (statisticsManager != null) statisticsManager.cleanup();
        if (guiManager != null) guiManager.shutdown();
        getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public void reload() {
        configManager.reload();
        if (languageManager != null) {
            languageManager.setLanguage(configManager.getConfig().getString("language", "en"));
        }
        dropManager.reload();
        statisticsManager.reload();
    }

    @Override
    public void sendFormattedMessage(org.bukkit.entity.Player player, String message) {
        Component component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacyAmpersand().deserialize(message);
        player.sendMessage(component);
    }

    @Override
    public ConfigManager getConfigManager() { return configManager; }
    @Override
    public DropManager getDropManager() { return dropManager; }
    @Override
    public StatisticsManager getStatisticsManager() { return statisticsManager; }
    @Override
    public LanguageManager getLanguageManager() { return languageManager; }
    public GUIManager getGUIManager() { return guiManager; }
    public MythicRodAPI getAPI() { return api; }
    
    private void setupMetricsCharts() {
        metrics.addCustomChart(new Metrics.SimplePie("server_type", () -> "Paper"));
        
        metrics.addCustomChart(new Metrics.SimplePie("language", () -> 
            languageManager != null ? languageManager.getLanguage() : "en"));
        
        metrics.addCustomChart(new Metrics.SimplePie("statistics_enabled", () -> 
            configManager.trackStatistics() ? "Enabled" : "Disabled"));
        
        metrics.addCustomChart(new Metrics.SimplePie("biome_drops_enabled", () -> 
            configManager.enableBiomeSpecificDrops() ? "Enabled" : "Disabled"));
        
        metrics.addCustomChart(new Metrics.SingleLineChart("total_catches", () -> 
            statisticsManager != null ? statisticsManager.getTotalCatches() : 0));
    }
}
