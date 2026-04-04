package io.xcutiboo.mythicrod;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import io.xcutiboo.mythicrod.api.ExternalDropProvider;
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.api.PlayerStatSnapshot;
import io.xcutiboo.mythicrod.api.PlayerStatSnapshot.StatType;
import io.xcutiboo.mythicrod.drops.DropRegistry;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.cache.MythicRodCache;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.LanguageManager;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.gui.GUIManager;
import io.xcutiboo.mythicrod.gui.menus.ConfigMenu;
import io.xcutiboo.mythicrod.gui.menus.DropsMenu;
import io.xcutiboo.mythicrod.gui.menus.LanguageSwitchMenu;
import io.xcutiboo.mythicrod.gui.menus.MainHubMenu;
import io.xcutiboo.mythicrod.gui.menus.RodMenu;
import io.xcutiboo.mythicrod.gui.menus.StatsMenu;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import io.xcutiboo.mythicrod.paper.util.PrettyLogger;
import io.xcutiboo.mythicrod.paper.api.PaperMythicRodAPI;
import io.xcutiboo.mythicrod.paper.commands.BrigadierCommandManager;
import io.xcutiboo.mythicrod.paper.data.PlayerDataService;
import io.xcutiboo.mythicrod.paper.fishing.FishingListener;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.paper.platform.PaperServer;
import io.xcutiboo.mythicrod.paper.scheduler.FoliaSchedulerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

public final class MythicRod extends JavaPlugin implements MythicRodPlugin, MythicRodAPI {
    private final Logger logger = getSLF4JLogger();
    private PrettyLogger prettyLogger;
    
    private PlatformServer platformServer;
    private PlatformScheduler platformScheduler;
    
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private DropManager dropManager;
    private DropRegistry dropRegistry;
    private StatisticsManager statisticsManager;
    
    private BrigadierCommandManager commandManager;
    private FishingListener fishingListener;
    private GUIManager guiManager;
    private PlayerDataService playerDataService;
    
    private MythicRodCache cache;
    private MythicRodAPI api;
    private Metrics metrics;

    @Override
    public void onLoad() {
        prettyLogger = new PrettyLogger(getLogger(), "MythicRod");
        prettyLogger.startup("MythicRod-Paper loading (Brigadier-enabled)...");
    }

    @Override
    public void onEnable() {
        long start = System.nanoTime();
        try {
            this.platformServer = new PaperServer(super.getServer(), this);
            this.platformScheduler = new FoliaSchedulerService(this);
            
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                saveDefaultConfig();
            }
            PlatformConfiguration platformConfig = platformServer.loadConfiguration(configFile);
            
            this.configManager = new ConfigManager(this, platformConfig);
            this.configManager.setConfigFile(configFile);
            
            this.languageManager = new LanguageManager(this, configManager);
            loadLanguageFiles();
            
            this.dropManager = new DropManager(getLogger());
            dropManager.loadDrops(platformConfig);
            this.dropRegistry = buildDropRegistry();
            
            this.statisticsManager = new StatisticsManager(this);
            
            // Initialize stats config
            File statsFile = new File(getDataFolder(), "statistics.yml");
            PlatformConfiguration statsConfig = platformServer.loadConfiguration(statsFile);
            configManager.setStatsConfig(statsFile, statsConfig);
            statisticsManager.initialize();
            
            this.commandManager = new BrigadierCommandManager(this);
            commandManager.initialize();
            
            this.fishingListener = new FishingListener(this);
            
            this.guiManager = new GUIManager(this, platformScheduler);
            guiManager.initialize();
            this.playerDataService = new PlayerDataService(this);
            this.cache = new MythicRodCache();
            
            this.api = new PaperMythicRodAPI(
                getPluginMeta().getVersion(),
                dropManager,
                dropRegistry,
                statisticsManager
            );
            // Register in Bukkit ServicesManager so external plugins can retrieve
            // the API without depending on MythicRod's internal class hierarchy.
            super.getServer().getServicesManager().register(
                MythicRodAPI.class, this, this, ServicePriority.Normal);
            this.metrics = new Metrics(this, 23847);
            
            super.getServer().getPluginManager().registerEvents(fishingListener, this);
            
            guiManager.registerMenu("main", MainHubMenu::new);
            guiManager.registerMenu("config", ConfigMenu::new);
            guiManager.registerMenu("drops", DropsMenu::new);
            guiManager.registerMenu("stats", StatsMenu::new);
            guiManager.registerMenu("language", LanguageSwitchMenu::new);
            guiManager.registerMenu("rod", RodMenu::new);
            
            setupMetricsCharts();

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
            super.getServer().getConsoleSender().sendMessage(banner);
        } catch (Exception e) {
            logger.error("Failed to enable MythicRod", e);
            super.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        super.getServer().getServicesManager().unregisterAll(this);
        if (statisticsManager != null) statisticsManager.cleanup();
        if (guiManager != null) guiManager.shutdown();
        if (cache != null) cache.invalidateAll();
        if (playerDataService != null) playerDataService.clearAllCache();
        super.getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public void reload() {
        File configFile = new File(getDataFolder(), "config.yml");
        PlatformConfiguration newConfig = platformServer.loadConfiguration(configFile);
        configManager.reload(newConfig);
        
        if (languageManager != null) {
            languageManager.setLanguage(configManager.getLanguage());
        }
        dropManager.reload(configManager.getConfig());
        // Rebuild registry contents in-place so the existing PaperMythicRodAPI
        // reference stays valid (it holds the same DropRegistry object).
        dropRegistry.clear();
        dropManager.getDropCategories().forEach(dropRegistry::registerCategory);
        statisticsManager.reload();
    }

    @Override
    public void sendFormattedMessage(PlatformPlayer player, String message) {
        if (!(player instanceof PaperPlayer paperPlayer)) {
            return;
        }
        Player bukkitPlayer = paperPlayer.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }
        
        try {
            Component prefixComponent = MiniMessage.miniMessage().deserialize(configManager.getPrefix());
            Component messageComponent = MiniMessage.miniMessage().deserialize(message);
            Component fullMessage = prefixComponent.append(messageComponent);
            bukkitPlayer.sendMessage(fullMessage);
        } catch (Exception e) {
            this.getLogger().warning("Failed to parse MiniMessage: " + e.getMessage());
            bukkitPlayer.sendMessage(message);
        }
    }
    
    @Override
    public PlatformServer getPlatform() {
        return platformServer;
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
    public PlayerDataService getPlayerDataService() { return playerDataService; }
    public MythicRodAPI getAPI() { return api; }
    
    // =========================================================================
    // MythicRodAPI delegation — all abstract methods delegated to PaperMythicRodAPI
    // =========================================================================

    @Override
    public @org.jetbrains.annotations.NotNull String getVersion() {
        return getPluginMeta().getVersion();
    }

    @Override
    public @org.jetbrains.annotations.NotNull DropRegistry getDropRegistry() {
        return dropRegistry;
    }

    @Override
    public void registerExternalDropProvider(@org.jetbrains.annotations.NotNull ExternalDropProvider provider) {
        api.registerExternalDropProvider(provider);
    }

    @Override
    public boolean unregisterExternalDropProvider(@org.jetbrains.annotations.NotNull String key) {
        return api.unregisterExternalDropProvider(key);
    }

    @Override
    public @org.jetbrains.annotations.NotNull Optional<ExternalDropProvider> getExternalDropProvider(@org.jetbrains.annotations.NotNull String key) {
        return api.getExternalDropProvider(key);
    }

    @Override
    public @org.jetbrains.annotations.NotNull List<ExternalDropProvider> getExternalDropProviders() {
        return api.getExternalDropProviders();
    }

    @Override
    public @org.jetbrains.annotations.NotNull CompletableFuture<@org.jetbrains.annotations.NotNull PlayerStatSnapshot> getPlayerStats(@org.jetbrains.annotations.NotNull UUID playerId) {
        return api.getPlayerStats(playerId);
    }

    @Override
    public @org.jetbrains.annotations.NotNull CompletableFuture<@org.jetbrains.annotations.NotNull List<@org.jetbrains.annotations.NotNull PlayerStatSnapshot>> getTopPlayers(
            @org.jetbrains.annotations.NotNull StatType statType,
            int limit) {
        return api.getTopPlayers(statType, limit);
    }

    @Override
    public @org.jetbrains.annotations.NotNull CompletableFuture<Void> flushAllStats() {
        return api.flushAllStats();
    }

    public MythicRodCache getCache() { return cache; }
    public int getActiveFishingCount() { return fishingListener != null ? fishingListener.getActiveFishingCount() : 0; }
    public boolean isFoliaSupported() { return platformScheduler instanceof FoliaSchedulerService; }
    
    @Override
    public void saveDefaultConfig() {
        super.saveDefaultConfig();
    }
    
    @Override
    public PlatformConfiguration loadConfig(File file) {
        return platformServer.loadConfiguration(file);
    }
    
    @Override
    public PlatformConfiguration createEmptyConfig() {
        return platformServer.createEmptyConfiguration();
    }
    
    public PlatformServer getPlatformServer() {
        return platformServer;
    }

    public PlatformScheduler getPlatformScheduler() {
        return platformServer.getScheduler();
    }
    
    private void setupMetricsCharts() {
        metrics.addCustomChart(new SimplePie("server_type", () -> "Paper"));
        metrics.addCustomChart(new SimplePie("language", () -> 
            languageManager != null ? languageManager.getLanguage() : "en"));
        metrics.addCustomChart(new SimplePie("statistics_enabled", () -> 
            configManager.trackStatistics() ? "Enabled" : "Disabled"));
        metrics.addCustomChart(new SimplePie("biome_drops_enabled", () -> 
            configManager.enableBiomeSpecificDrops() ? "Enabled" : "Disabled"));
        metrics.addCustomChart(new SingleLineChart("total_catches", () -> 
            statisticsManager != null ? (int) Math.min(statisticsManager.getTotalCatches(), Integer.MAX_VALUE) : 0));
    }

    private DropRegistry buildDropRegistry() {
        DropRegistry registry = new DropRegistry();
        dropManager.getDropCategories().forEach(registry::registerCategory);
        return registry;
    }

    private void loadLanguageFiles() {
        String[] bundledLangs = {"en_US", "ja_JP"};
        for (String lang : bundledLangs) {
            try (InputStream is = getClass().getResourceAsStream("/lang/" + lang + ".yml")) {
                if (is != null) {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(is));
                    Map<String, String> translations = new HashMap<>();
                    flattenYaml(yaml, "", translations);
                    languageManager.loadTranslations(lang, translations);
                    prettyLogger.info("Loaded language: " + lang + " (" + translations.size() + " entries)");
                }
            } catch (Exception e) {
                prettyLogger.warning("Failed to load language: " + lang);
            }
        }
    }

    private void flattenYaml(org.bukkit.configuration.ConfigurationSection section, String prefix, Map<String, String> result) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (section.isConfigurationSection(key)) {
                flattenYaml(section.getConfigurationSection(key), fullKey, result);
            } else if (section.isList(key)) {
                List<String> list = section.getStringList(key);
                result.put(fullKey, String.join("\n", list));
            } else {
                String value = section.getString(key);
                if (value != null) {
                    result.put(fullKey, value);
                }
            }
        }
    }
}
