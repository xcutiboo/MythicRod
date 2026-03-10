package io.xcutiboo.mythicrod;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.LanguageManager;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import io.xcutiboo.mythicrod.spigot.commands.BrigadierStyleCommandManager;
import io.xcutiboo.mythicrod.spigot.di.SpigotModule;
import io.xcutiboo.mythicrod.spigot.fishing.FishingListener;
import io.xcutiboo.mythicrod.spigot.gui.GUIManager;
import io.xcutiboo.mythicrod.spigot.gui.menus.ConfigMenu;
import io.xcutiboo.mythicrod.spigot.gui.menus.DropsMenu;
import io.xcutiboo.mythicrod.spigot.gui.menus.LanguageSwitchMenu;
import io.xcutiboo.mythicrod.spigot.gui.menus.MainHubMenu;
import io.xcutiboo.mythicrod.spigot.gui.menus.StatsMenu;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bstats.bukkit.Metrics;

/**
 * MythicRod Spigot Implementation
 *
 * This is a FEATURE-COMPLETE MIRROR of the Paper implementation for Spigot servers.
 * Uses bundled Adventure Platform to provide IDENTICAL functionality to Paper.
 *
 * PARITY GUARANTEES:
 * - Command syntax and behavior matches Paper exactly (Brigadier-style adapter)
 * - GUI layout and interactions are pixel-perfect identical
 * - All features, permissions, and UX match Paper 100%
 * - Performance degradation only where technically impossible to avoid
 *
 * Paper is the source of truth. Spigot adapts.
 */
public final class MythicRod extends JavaPlugin implements MythicRodPlugin {
    private io.xcutiboo.mythicrod.api.platform.PlatformServer platformServer;
    @Override
    public io.xcutiboo.mythicrod.api.platform.PlatformServer getPlatform() { return platformServer; }

        
    private Injector injector;
    private ConfigManager configManager;
    private DropManager dropManager;
    private StatisticsManager statisticsManager;
        private GUIManager guiManager;
    private MythicRodAPI api;
    private FishingListener fishingListener;
    private LanguageManager languageManager;
    private BukkitAudiences audiences;
    private Metrics metrics;

    @Override
    public void onEnable() {
        long startTime = System.nanoTime();

        try {
            injector = Guice.createInjector(new SpigotModule(this));
            
            this.audiences = injector.getInstance(BukkitAudiences.class);
            this.configManager = injector.getInstance(ConfigManager.class);
            this.languageManager = injector.getInstance(LanguageManager.class);
            this.dropManager = injector.getInstance(DropManager.class);
            this.statisticsManager = injector.getInstance(StatisticsManager.class);
            injector.getInstance(BrigadierStyleCommandManager.class);
            this.fishingListener = injector.getInstance(FishingListener.class);
            this.guiManager = injector.getInstance(GUIManager.class);
            this.platformServer = new io.xcutiboo.mythicrod.spigot.platform.SpigotServer(this);
            this.api = injector.getInstance(MythicRodAPI.class);
            this.metrics = injector.getInstance(Metrics.class);
            
            getLogger().log(java.util.logging.Level.INFO, "[MythicRod-Spigot] Adventure Platform initialized and ready");
            
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

            long ms = (System.nanoTime() - startTime) / 1_000_000;
            Component banner = Component.text()
                .append(Component.text("MythicRod-Spigot ", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("v" + getDescription().getVersion(), NamedTextColor.GREEN))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Spigot 1.21.4", NamedTextColor.YELLOW))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Brigadier-Style ✓", NamedTextColor.GREEN))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(ms + "ms", NamedTextColor.YELLOW))
                .build();
            audiences.console().sendMessage(banner);

            getLogger().log(java.util.logging.Level.INFO, "[MythicRod-Spigot] Loaded " + dropManager.getTotalDropCount() + " custom drops from configuration");
            getLogger().log(java.util.logging.Level.INFO, "[MythicRod-Spigot] Running on Spigot 1.21.4+ with bundled Adventure Platform for Paper feature parity");
            getLogger().log(java.util.logging.Level.INFO, "[MythicRod-Spigot] All systems initialized successfully in " + ms + "ms");

        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Failed to enable MythicRod-Spigot", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (statisticsManager != null) {
                statisticsManager.cleanup();
                getLogger().log(java.util.logging.Level.INFO, "[MythicRod-Spigot] Player statistics persisted to storage");
            }

            if (guiManager != null) {
                guiManager.shutdown();
            }

            getServer().getScheduler().cancelTasks(this);

            // Close Adventure Platform
            if (audiences != null) {
                audiences.close();
                audiences = null;
            }

            getLogger().log(java.util.logging.Level.INFO, "[MythicRod-Spigot] Plugin disabled successfully");

        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "[MythicRod-Spigot] Critical error during plugin shutdown. Check for data loss.", e);
        }
    }

    /**
     * Get the BukkitAudiences instance for Adventure Component support.
     */
    public BukkitAudiences audiences() {
        return audiences;
    }

    public io.xcutiboo.mythicrod.api.platform.PlatformPlayer wrapPlayer(org.bukkit.entity.Player player) {
        return player == null ? null : new io.xcutiboo.mythicrod.spigot.platform.SpigotPlayer(player);
    }

    public io.xcutiboo.mythicrod.api.platform.PlatformCommandSender wrapSender(org.bukkit.command.CommandSender sender) {
        if (sender instanceof org.bukkit.entity.Player) {
            return wrapPlayer((org.bukkit.entity.Player) sender);
        }
        return sender == null ? null : new io.xcutiboo.mythicrod.spigot.platform.SpigotCommandSender(sender);
    }


    @Override
    public ConfigManager getConfigManager() { return configManager; }

    @Override
    public DropManager getDropManager() { return dropManager; }

    @Override
    public StatisticsManager getStatisticsManager() { return statisticsManager; }

    @Override
    public LanguageManager getLanguageManager() { return languageManager; }

    public MythicRodAPI getAPI() { return api; }

    public GUIManager getGUIManager() { return guiManager; }

    @Override
    public void reload() {
        try {
            getLogger().log(java.util.logging.Level.INFO, "[MythicRod-Spigot] Configuration reload initiated by admin or plugin");
            configManager.reload();
            if (languageManager != null) {
                languageManager.setLanguage(configManager.getConfig().getString("language", "en"));
            }
            dropManager.loadDrops(configManager.getStatsConfig());
            statisticsManager.reload();
            getLogger().log(java.util.logging.Level.INFO, "[MythicRod-Spigot] Configuration and all managers reloaded successfully");
        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "[MythicRod-Spigot] Critical error reloading configuration. Check YAML syntax and file permissions.", e);
            throw new RuntimeException("Failed to reload configuration", e);
        }
    }

    @Override
    public void sendFormattedMessage(io.xcutiboo.mythicrod.api.platform.PlatformPlayer player, String message) {
        Component component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacyAmpersand().deserialize(message);
        audiences.player(java.util.UUID.fromString(player.getUniqueId().toString())).sendMessage(component);
    }
    
    private void setupMetricsCharts() {
        metrics.addCustomChart(new org.bstats.charts.SimplePie("server_type", () -> "Spigot"));
        
        metrics.addCustomChart(new org.bstats.charts.SimplePie("language", () -> 
            languageManager != null ? languageManager.getLanguage() : "en"));
        
        metrics.addCustomChart(new org.bstats.charts.SimplePie("statistics_enabled", () -> 
            configManager.trackStatistics() ? "Enabled" : "Disabled"));
        
        metrics.addCustomChart(new org.bstats.charts.SimplePie("biome_drops_enabled", () -> 
            configManager.enableBiomeSpecificDrops() ? "Enabled" : "Disabled"));
        
        metrics.addCustomChart(new org.bstats.charts.SingleLineChart("total_catches", () -> 
            statisticsManager != null ? statisticsManager.getTotalCatches() : 0));
    }
}
