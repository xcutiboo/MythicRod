package io.xcutiboo.mythicrod.paper;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.api.platform.PlatformTask;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.LanguageManager;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.internal.runtime.MythicRodRuntime;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import io.xcutiboo.mythicrod.paper.api.PaperMythicRodAPI;
import io.xcutiboo.mythicrod.paper.commands.BrigadierCommandManager;
import io.xcutiboo.mythicrod.paper.data.PlayerDataService;
import io.xcutiboo.mythicrod.paper.data.StatisticsPlayerListener;
import io.xcutiboo.mythicrod.paper.fishing.FishingListener;
import io.xcutiboo.mythicrod.paper.gui.GUIManager;
import io.xcutiboo.mythicrod.paper.gui.menus.ConfigMenu;
import io.xcutiboo.mythicrod.paper.gui.menus.DropsMenu;
import io.xcutiboo.mythicrod.paper.gui.menus.EditDropMenu;
import io.xcutiboo.mythicrod.paper.gui.menus.LanguageSwitchMenu;
import io.xcutiboo.mythicrod.paper.gui.menus.MainHubMenu;
import io.xcutiboo.mythicrod.paper.gui.menus.RodMenu;
import io.xcutiboo.mythicrod.paper.gui.menus.StatsMenu;
import io.xcutiboo.mythicrod.paper.metrics.MetricsReporter;
import io.xcutiboo.mythicrod.paper.internal.config.LanguageFileLoader;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.paper.platform.PaperServer;
import io.xcutiboo.mythicrod.paper.scheduler.FoliaSchedulerService;
import io.xcutiboo.mythicrod.paper.util.ParticleOptions;
import io.xcutiboo.mythicrod.paper.util.PrettyLogger;
import io.xcutiboo.mythicrod.text.ConfiguredText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class MythicRod extends JavaPlugin implements MythicRodRuntime {
    private static final String FILE_CONFIG = "config.yml";
    private static final String FILE_DROPS = "drops.yml";
    private static final String FILE_STATS = "statistics.yml";

    private final Logger logger = getSLF4JLogger();
    private PrettyLogger prettyLogger;

    private PlatformServer platformServer;
    private PlatformScheduler platformScheduler;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private DropManager dropManager;
    private StatisticsManager statisticsManager;

    private BrigadierCommandManager commandManager;
    private FishingListener fishingListener;
    private GUIManager guiManager;
    private PlayerDataService playerDataService;
    private StatisticsPlayerListener statisticsPlayerListener;
    private LanguageFileLoader languageFileLoader;

    private PaperMythicRodAPI api;
    private MetricsReporter metricsReporter;
    private PlatformTask statisticsSaveTask;
    private final AtomicBoolean reloadInProgress = new AtomicBoolean(false);

    @Override
    public void onLoad() {
        prettyLogger = new PrettyLogger(getLogger());
        prettyLogger.startup("MythicRod-Paper loading (Brigadier-enabled)...");
    }

    private record DropsBootstrap(PlatformConfiguration dropsConfig, File dropsFile) {}

    @Override
    public void onEnable() {
        long start = System.nanoTime();
        try {
            bootstrapPlatform();
            DropsBootstrap drops = bootstrapConfiguration();
            bootstrapLanguages();
            bootstrapDrops(drops);
            bootstrapStatistics();
            bootstrapCommandsAndListeners();
            bootstrapGuiAndApi();
            initializeMetrics();
            announceStartup(start);
        } catch (Exception e) {
            logger.error("Failed to enable MythicRod", e);
            super.getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void bootstrapPlatform() {
        this.platformServer = new PaperServer(super.getServer(), this);
        this.platformScheduler = platformServer.getScheduler();
    }

    private DropsBootstrap bootstrapConfiguration() {
        File configFile = new File(getDataFolder(), FILE_CONFIG);
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        PlatformConfiguration platformConfig = platformServer.loadConfiguration(configFile);

        File dropsFile = new File(getDataFolder(), FILE_DROPS);
        if (!dropsFile.exists()) {
            saveResource(FILE_DROPS, false);
        }
        PlatformConfiguration dropsConfig = platformServer.loadConfiguration(dropsFile);

        this.configManager = new ConfigManager(this, platformConfig);
        this.configManager.setConfigFile(configFile);
        validateConfiguredParticles();
        return new DropsBootstrap(dropsConfig, dropsFile);
    }

    private void bootstrapLanguages() {
        this.languageManager = new LanguageManager(this, configManager);
        this.languageFileLoader = new LanguageFileLoader(this, logger, prettyLogger, languageManager);
        languageFileLoader.loadLanguageFiles();
    }

    private void bootstrapDrops(DropsBootstrap drops) {
        this.dropManager = new DropManager(getLogger());
        applyDropRuntimeSettings();
        dropManager.loadDrops(drops.dropsConfig(), drops.dropsFile());
    }

    private void bootstrapStatistics() {
        this.statisticsManager = new StatisticsManager(this);
        File statsFile = new File(getDataFolder(), FILE_STATS);
        PlatformConfiguration statsConfig = platformServer.loadConfiguration(statsFile);
        configManager.setStatsConfig(statsFile, statsConfig);
        statisticsManager.initialize();
        scheduleStatisticsSaveTask();
        this.statisticsPlayerListener = new StatisticsPlayerListener(this);
        super.getServer().getPluginManager().registerEvents(statisticsPlayerListener, this);
        statisticsPlayerListener.preloadOnlinePlayers();
    }

    private void bootstrapCommandsAndListeners() {
        this.commandManager = new BrigadierCommandManager(this);
        commandManager.initialize();
        this.fishingListener = new FishingListener(this);
    }

    private void bootstrapGuiAndApi() {
        this.guiManager = new GUIManager(this, platformScheduler);
        guiManager.initialize();
        this.playerDataService = new PlayerDataService(this);
        super.getServer().getPluginManager().registerEvents(playerDataService, this);
        this.api = new PaperMythicRodAPI(
            getPluginMeta().getVersion(),
            getLogger(),
            dropManager,
            statisticsManager,
            platformScheduler,
            platformServer.getItemFactory()
        );
        // Bukkit ServicesManager registration lets third-party plugins resolve
        // the API without compile-time dependency on our internal classes.
        super.getServer().getServicesManager().register(
            MythicRodAPI.class, api, this, ServicePriority.Normal);

        super.getServer().getPluginManager().registerEvents(fishingListener, this);

        guiManager.registerMenu("main",     MainHubMenu::new);
        guiManager.registerMenu("config",   ConfigMenu::new);
        guiManager.registerMenu("drops",    DropsMenu::new);
        guiManager.registerMenu("editdrop", EditDropMenu::new);
        guiManager.registerMenu("stats",    StatsMenu::new);
        guiManager.registerMenu("language", LanguageSwitchMenu::new);
        guiManager.registerMenu("rod",      RodMenu::new);
    }

    private void announceStartup(long startNanos) {
        long ms = (System.nanoTime() - startNanos) / 1_000_000;
        String serverVersion = super.getServer().getName() + " " + super.getServer().getMinecraftVersion();
        Component banner = Component.text()
            .append(Component.text("MythicRod-Paper ", NamedTextColor.AQUA, TextDecoration.BOLD))
            .append(Component.text("v" + getPluginMeta().getVersion(), NamedTextColor.GREEN))
            .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
            .append(Component.text(serverVersion, NamedTextColor.YELLOW))
            .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
            .append(Component.text("Brigadier ✓", NamedTextColor.GREEN))
            .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
            .append(Component.text(ms + "ms", NamedTextColor.YELLOW))
            .build();
        super.getServer().getConsoleSender().sendMessage(banner);
    }

    @Override
    public void onDisable() {
        shutdownStep("services", () -> super.getServer().getServicesManager().unregisterAll(this));
        shutdownStep("event handlers", () -> HandlerList.unregisterAll(this));
        if (statisticsManager != null) {
            shutdownStep("statistics manager", () -> {
                cancelStatisticsSaveTask();
                statisticsManager.cleanup();
            });
        }
        if (languageManager != null) {
            shutdownStep("language manager", languageManager::shutdown);
        }
        if (guiManager != null) {
            shutdownStep("GUI manager", guiManager::shutdown);
        }
        if (playerDataService != null) {
            shutdownStep("player data cache", playerDataService::clearAllCache);
        }
        if (platformScheduler instanceof FoliaSchedulerService schedulerService) {
            shutdownStep("scheduled tasks", schedulerService::cancelPluginTasks);
        }
        logger.info("MythicRod disabled successfully");
    }

    private void shutdownStep(String label, Runnable step) {
        try {
            step.run();
            logger.info("Shutdown step completed: {}", label);
        } catch (Exception e) {
            logger.warn("Shutdown step failed: " + label, e);
        }
    }

    public boolean reload() {
        if (!reloadInProgress.compareAndSet(false, true)) {
            logger.info("MythicRod reload request ignored because a reload is already in progress");
            return false;
        }

        boolean restartStatisticsAutosave = statisticsSaveTask != null;
        try {
            if (guiManager != null) {
                guiManager.invalidateOpenMenusForReload();
            }

            File configFile = new File(getDataFolder(), FILE_CONFIG);
            PlatformConfiguration newConfig = platformServer.loadConfiguration(configFile);
            if (dropManager != null) {
                dropManager.awaitAsyncPersistenceOperations();
            }
            File dropsFile = new File(getDataFolder(), FILE_DROPS);
            PlatformConfiguration newDropsConfig = platformServer.loadConfiguration(dropsFile);
            configManager.reload(newConfig);
            validateConfiguredParticles();
            applyDropRuntimeSettings();

            cancelStatisticsSaveTask();

            if (languageManager != null) {
                languageManager.reloadPlayerPreferences();
                languageManager.refreshFormatting();
                languageManager.setLanguage(configManager.getLanguage());
                if (languageFileLoader != null) {
                    languageFileLoader.loadLanguageFiles();
                }
            }

            dropManager.reload(newDropsConfig, dropsFile);

            if (statisticsManager != null) {
                statisticsManager.reload();
            }

            if (statisticsPlayerListener != null) {
                statisticsPlayerListener.preloadOnlinePlayers();
            }

            scheduleStatisticsSaveTask();

            logger.info("MythicRod reload completed successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error during reload", e);
            return false;
        } finally {
            if (restartStatisticsAutosave && statisticsSaveTask == null) {
                scheduleStatisticsSaveTask();
            }
            reloadInProgress.set(false);
        }
    }

    public void applyDropRuntimeSettings() {
        if (dropManager == null || configManager == null) {
            return;
        }

        dropManager.setDebugMode(configManager.isDebugMode());
        dropManager.setUsePermissions(configManager.usePermissions());
        dropManager.setUseBiomeSpecificDrops(configManager.enableBiomeSpecificDrops());
    }

    public void sendFormattedMessage(PlatformPlayer player, String message) {
        if (!(player instanceof PaperPlayer paperPlayer)) {
            return;
        }

        Player bukkitPlayer = paperPlayer.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }

        if (message == null || message.isBlank()) {
            return;
        }

        if (message.length() > 10000) {
            message = message.substring(0, 10000) + "...";
        }

        try {
            String prefix = configManager.getPrefix();
            if (prefix == null || prefix.isBlank()) {
                prefix = "[MythicRod] ";
            }

            Component fullMessage = ConfiguredText.parse(prefix)
                .append(ConfiguredText.parse(message));
            bukkitPlayer.sendMessage(fullMessage);
        } catch (Exception e) {
            this.getLogger().log(Level.WARNING, e,
                () -> "Unexpected error while sending formatted message: " + e.getMessage());
        }
    }

    @Override
    public PlatformServer getPlatform() {
        return platformServer;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DropManager getDropManager() { return dropManager; }
    public StatisticsManager getStatisticsManager() { return statisticsManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public GUIManager getGUIManager() { return guiManager; }
    public boolean isReloadInProgress() { return reloadInProgress.get(); }
    public PlayerDataService getPlayerDataService() { return playerDataService; }
    public MythicRodAPI getAPI() { return api; }

    // Paper-internal access: same instance, narrower static type, so plugin
    // code can call PaperMythicRodAPI-only helpers without casting.
    @SuppressWarnings("java:S4144")
    public PaperMythicRodAPI getApiFacade() { return api; }

    public boolean isFoliaRuntime() {
        return platformScheduler instanceof FoliaSchedulerService schedulerService && schedulerService.isFoliaRuntime();
    }

    @Override
    public PlatformConfiguration loadConfig(File file) {
        return platformServer.loadConfiguration(file);
    }

    @Override
    public PlatformConfiguration createEmptyConfig() {
        return platformServer.createEmptyConfiguration();
    }

    // Paper-internal access: identical body to getPlatform() but kept under a
    // different name for symmetry with other manager getters on this class.
    @SuppressWarnings("java:S4144")
    public PlatformServer getPlatformServer() {
        return platformServer;
    }

    public PlatformScheduler getPlatformScheduler() {
        return platformScheduler;
    }

    public void refreshStatisticsAutosaveSchedule() {
        try {
            cancelStatisticsSaveTask();
            scheduleStatisticsSaveTask();
        } catch (Exception e) {
            logger.warn("Failed to refresh statistics autosave schedule", e);
        }
    }

    private void initializeMetrics() {
        this.metricsReporter = new MetricsReporter(this);
        metricsReporter.start();
    }

    private void validateConfiguredParticles() {
        if (configManager == null) {
            return;
        }

        validateParticleSetting(
            "features.particles.catch-particle",
            configManager.getCatchParticle(),
            "SPLASH",
            configManager::setCatchParticle
        );
        validateParticleSetting(
            "features.particles.bubble-particle",
            configManager.getBubbleParticle(),
            "BUBBLE_POP",
            configManager::setBubbleParticle
        );
        validateParticleSetting(
            "features.particles.success-particle",
            configManager.getSuccessParticle(),
            "HAPPY_VILLAGER",
            configManager::setSuccessParticle
        );
        validateParticleSetting(
            "features.particles.xp-particle",
            configManager.getXpParticle(),
            "HAPPY_VILLAGER",
            configManager::setXpParticle
        );
    }

    private void validateParticleSetting(String path, String configuredValue, String fallback, Consumer<String> setter) {
        String normalized = ParticleOptions.normalize(configuredValue);
        if (ParticleOptions.isConfigurableParticleName(normalized)) {
            setter.accept(normalized);
            return;
        }

        logger.warn("Invalid particle '{}' at {}. Falling back to {}.", configuredValue, path, fallback);
        setter.accept(fallback);
    }

    private void scheduleStatisticsSaveTask() {
        if (platformScheduler == null || statisticsManager == null || configManager == null) {
            return;
        }

        long intervalMillis = Math.max(1L, configManager.getStatsSaveInterval()) * 1000L;
        statisticsSaveTask = platformScheduler.runAsyncRepeating(() -> {
            try {
                statisticsManager.saveAll();
            } catch (Exception e) {
                logger.warn("Error during scheduled statistics save", e);
            }
        }, intervalMillis, intervalMillis);
        logger.info("Scheduled statistics autosave every {} seconds", configManager.getStatsSaveInterval());
    }

    private void cancelStatisticsSaveTask() {
        if (statisticsSaveTask == null) {
            return;
        }
        try {
            statisticsSaveTask.cancel();
        } catch (Exception e) {
            logger.warn("Failed to cancel statistics autosave task", e);
        } finally {
            statisticsSaveTask = null;
        }
    }

}
