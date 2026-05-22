package io.xcutiboo.mythicrod.paper.gui.menus;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.RewardDeliveryMode;
import io.xcutiboo.mythicrod.constants.PermissionNodes;
import io.xcutiboo.mythicrod.paper.gui.MenuItemFactory;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;
import io.xcutiboo.mythicrod.paper.util.ParticleOptions;
import io.xcutiboo.mythicrod.paper.util.StringFormatting;

public class ConfigMenu extends BaseMenu {
    private static final String CTX_STATUS = "status";
    private static final String CTX_ACTION = "action";
    private static final String CTX_PARTICLE = "particle";
    private static final String TR_ENABLED = "gui.config.enabled";
    private static final String TR_DISABLED = "gui.config.disabled";
    private static final String TR_ENABLE = "gui.config.enable";
    private static final String TR_DISABLE = "gui.config.disable";

    private static final String ADMIN_PERMISSION = PermissionNodes.ADMIN_CONFIG;

    private boolean draftSoundsEnabled;
    private boolean draftParticlesEnabled;
    private boolean draftStatisticsEnabled;
    private boolean draftBiomeDropsEnabled;
    private RewardDeliveryMode draftRewardDeliveryMode;
    private boolean draftPermissionsEnabled;
    private boolean draftDebugEnabled;
    private int draftStatsSaveInterval;
    private String draftCatchParticle;
    private String draftBubbleParticle;
    private String draftSuccessParticle;
    private String draftXpParticle;

    public ConfigMenu(MythicRod plugin, Player player) {
        super(plugin, player);
        ConfigManager config = plugin.getConfigManager();
        this.draftSoundsEnabled = config.useSounds();
        this.draftParticlesEnabled = config.useParticles();
        this.draftStatisticsEnabled = config.trackStatistics();
        this.draftBiomeDropsEnabled = config.enableBiomeSpecificDrops();
        this.draftRewardDeliveryMode = config.getRewardDeliveryMode();
        this.draftPermissionsEnabled = config.usePermissions();
        this.draftDebugEnabled = config.isDebugMode();
        this.draftStatsSaveInterval = config.getStatsSaveInterval();
        this.draftCatchParticle = config.getCatchParticle();
        this.draftBubbleParticle = config.getBubbleParticle();
        this.draftSuccessParticle = config.getSuccessParticle();
        this.draftXpParticle = config.getXpParticle();
    }

    private record ConfigSnapshot(
        boolean soundsEnabled,
        boolean particlesEnabled,
        boolean statisticsEnabled,
        boolean biomeDropsEnabled,
        RewardDeliveryMode rewardDeliveryMode,
        boolean permissionsEnabled,
        boolean debugEnabled,
        int statsSaveInterval,
        String catchParticle,
        String bubbleParticle,
        String successParticle,
        String xpParticle
    ) {}

    @Override
    public String getRequiredPermission() {
        return ADMIN_PERMISSION;
    }
    @Override
    protected int getSize() {
        return 54;
    }
    @Override
    protected String getTitle() {
        return tr("gui.config.title");
    }
    @Override
    protected void build() {
        ConfigManager config = plugin.getConfigManager();
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        boolean useSounds = draftSoundsEnabled;
        ItemStack soundsItem = createToggleItem(
            useSounds,
            tr("gui.config.sounds"),
            tr("gui.config.sounds_lore"),
            Material.NOTE_BLOCK,
            Material.GRAY_STAINED_GLASS
        );
        setConfigurableToggle(10, soundsItem, () -> draftSoundsEnabled = !draftSoundsEnabled);

        boolean useParticles = draftParticlesEnabled;
        ItemStack particlesItem = createToggleItem(
            useParticles,
            tr("gui.config.particles"),
            tr("gui.config.particles_lore"),
            Material.FIREWORK_STAR,
            Material.GRAY_DYE
        );
        setConfigurableToggle(11, particlesItem, () -> draftParticlesEnabled = !draftParticlesEnabled);

        ItemStack particleSettingsItem = new ItemBuilder(Material.FIREWORK_ROCKET)
                .name(tr("gui.config.particles_settings.name"))
                .lore(
                        tr("gui.config.particles_settings.lore1"),
                        tr("gui.config.particles_settings.lore2"),
                        "",
                        tr("gui.config.particles_settings.current_catch", Map.of(CTX_PARTICLE, draftCatchParticle)),
                        tr("gui.config.particles_settings.current_bubble", Map.of(CTX_PARTICLE, draftBubbleParticle)),
                        tr("gui.config.particles_settings.current_success", Map.of(CTX_PARTICLE, draftSuccessParticle)),
                        tr("gui.config.particles_settings.current_xp", Map.of(CTX_PARTICLE, draftXpParticle)),
                        "",
                        tr("gui.config.particles_settings.left_click"),
                        tr("gui.config.particles_settings.right_click"),
                        tr("gui.config.particles_settings.shift_left"),
                        tr("gui.config.particles_settings.shift_right"),
                        "",
                        tr("gui.config.particles_settings.available")
                )
                .glow(draftParticlesEnabled)
                .build();
        setItem(12, particleSettingsItem, event -> {
            if (!requirePermission()) return;
            playClickSound();
            if (event.isShiftClick() && event.isLeftClick()) {
                draftSuccessParticle = cycleParticle(draftSuccessParticle);
            } else if (event.isShiftClick() && event.isRightClick()) {
                draftXpParticle = cycleParticle(draftXpParticle);
            } else if (event.isRightClick()) {
                draftBubbleParticle = cycleParticle(draftBubbleParticle);
            } else {
                draftCatchParticle = cycleParticle(draftCatchParticle);
            }
            refresh();
        });
        boolean trackStats = draftStatisticsEnabled;
        ItemStack statsItem = createToggleItem(
            trackStats,
            tr("gui.config.stats"),
            tr("gui.config.stats_lore"),
            Material.FILLED_MAP,
            Material.MAP
        );
        setConfigurableToggle(14, statsItem, () -> draftStatisticsEnabled = !draftStatisticsEnabled);

        boolean biomeDrops = draftBiomeDropsEnabled;
        ItemStack biomeItem = createToggleItem(
            biomeDrops,
            tr("gui.config.biome_drops"),
            tr("gui.config.biome_drops_lore"),
            Material.GRASS_BLOCK,
            Material.COARSE_DIRT
        );
        setConfigurableToggle(15, biomeItem, () -> draftBiomeDropsEnabled = !draftBiomeDropsEnabled);

        RewardDeliveryMode rewardDeliveryMode = draftRewardDeliveryMode;
        ItemStack deliveryModeItem = new ItemBuilder(getRewardDeliveryModeMaterial(rewardDeliveryMode))
                .name(tr("gui.config.delivery_mode.name"))
                .lore(
                        tr("gui.config.delivery_mode.lore1"),
                        tr("gui.config.delivery_mode.lore2"),
                        "",
                        tr("gui.config.delivery_mode.current", Map.of("mode", tr(getRewardDeliveryModeKey(rewardDeliveryMode)))),
                        tr(getRewardDeliveryModeDescriptionKey(rewardDeliveryMode)),
                        "",
                        tr("gui.config.delivery_mode.left_click"),
                        tr("gui.config.delivery_mode.right_click")
                )
                .glow(rewardDeliveryMode != RewardDeliveryMode.VANILLA_RETRIEVE)
                .build();
        setItem(16, deliveryModeItem, event -> {
            if (!requirePermission()) return;
            draftRewardDeliveryMode = event.isRightClick()
                ? draftRewardDeliveryMode.previous()
                : draftRewardDeliveryMode.next();
            playClickSound();
            refresh();
        });

        boolean usePerms = draftPermissionsEnabled;
        ItemStack permsItem = new ItemBuilder(usePerms ? Material.DIAMOND : Material.COAL)
                .name(tr("gui.config.perms.name", Map.of(CTX_STATUS, usePerms ? "<green>✓" : "<red>✗")))
                .lore(
                        tr("gui.config.perms.lore1"),
                        tr("gui.config.perms.lore2"),
                        "",
                        tr("gui.config.perms.status", Map.of("color", getStatusColor(usePerms), CTX_STATUS, usePerms ? tr(TR_ENABLED) : tr(TR_DISABLED))),
                        "",
                        usePerms ? tr("gui.config.perms.active") : tr("gui.config.perms.inactive"),
                        "",
                        tr("gui.config.perms.warning"),
                        "",
                        tr("gui.config.perms.click", Map.of(CTX_ACTION, usePerms ? tr(TR_DISABLE) : tr(TR_ENABLE)))
                )
                .glow(usePerms)
                .build();
        setItem(20, permsItem, () -> {
            if (!requirePermission()) return;
            draftPermissionsEnabled = !draftPermissionsEnabled;
            playClickSound();
            refresh();
        });

        int statsSaveInterval = draftStatsSaveInterval;
        ItemStack saveIntervalItem = new ItemBuilder(Material.CLOCK)
                .name(tr("gui.config.save_interval.name"))
                .lore(
                        tr("gui.config.save_interval.lore1"),
                        tr("gui.config.save_interval.lore2"),
                        "",
                        tr("gui.config.save_interval.current", Map.of("time", StringFormatting.formatTime(statsSaveInterval), "seconds", String.valueOf(statsSaveInterval))),
                        "",
                        statsSaveInterval <= 300 ? tr("gui.config.save_interval.frequent") : statsSaveInterval <= 600 ? tr("gui.config.save_interval.balanced") : tr("gui.config.save_interval.infrequent"),
                        "",
                        tr("gui.config.save_interval.controls"),
                        tr("gui.config.save_interval.left_click"),
                        tr("gui.config.save_interval.right_click"),
                        tr("gui.config.save_interval.shift_left"),
                        tr("gui.config.save_interval.shift_right"),
                        "",
                        tr("gui.config.save_interval.minimum")
                )
                .build();
        setItem(22, saveIntervalItem, event -> {
            if (!requirePermission()) return;
            int change = 60;
            if (event.isShiftClick()) {
                change = 300;
            }
            int newValue = statsSaveInterval;
            if (event.isLeftClick()) {
                newValue = Math.min(3600, newValue + change);
            } else if (event.isRightClick()) {
                newValue = Math.max(60, newValue - change);
            }
            draftStatsSaveInterval = newValue;
            playClickSound();
            refresh();
        });

        boolean debugMode = draftDebugEnabled;
        ItemStack debugItem = new ItemBuilder(debugMode ? Material.REDSTONE_TORCH : Material.TORCH)
                .name(tr("gui.config.debug.name", Map.of(CTX_STATUS, debugMode ? "<green>✓" : "<red>✗")))
                .lore(
                        tr("gui.config.debug.lore1"),
                        tr("gui.config.debug.lore2"),
                        "",
                        tr("gui.config.debug.status", Map.of("color", getStatusColor(debugMode), CTX_STATUS, debugMode ? tr(TR_ENABLED) : tr(TR_DISABLED))),
                        "",
                        debugMode ? tr("gui.config.debug.active") : tr("gui.config.debug.inactive"),
                        "",
                        tr("gui.config.debug.warning"),
                        "",
                        tr("gui.config.debug.click", Map.of(CTX_ACTION, debugMode ? tr(TR_DISABLE) : tr(TR_ENABLE)))
                )
                .glow(debugMode)
                .build();
        setItem(24, debugItem, () -> {
            if (!requirePermission()) return;
            draftDebugEnabled = !draftDebugEnabled;
            playClickSound();
            refresh();
        });
        ItemStack languageItem = new ItemBuilder(Material.REPEATER)
                .name(tr("gui.config.language.name"))
                .lore(
                        tr("gui.config.language.lore1"),
                        tr("gui.config.language.lore2"),
                        "",
                        tr("gui.config.language.current", Map.of("lang", formatLanguageName(plugin.getLanguageManager().getEffectivePlayerLanguage(playerUuid)))),
                        "",
                        tr("gui.config.language.available"),
                        tr("gui.config.language.option_english"),
                        tr("gui.config.language.option_japanese"),
                        "",
                        tr("gui.config.language.click")
                )
                .glow(true)
                .build();
        setItem(38, languageItem, () -> {
            playClickSound();
            plugin.getGUIManager().openMenu(getPlayer(), "language");
        });
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name(tr("gui.config.info.name"))
                .lore(
                        tr("gui.config.info.lore1"),
                        tr("gui.config.info.lore2"),
                        "",
                        tr("gui.config.info.lore3"),
                        tr("gui.config.info.lore4"),
                        "",
                        tr("gui.config.info.lore5")
                )
                .build();
        setItem(40, infoItem);
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name(tr("gui.config.back.name"))
                .lore(tr("gui.config.back.lore"))
                .build();
        setItem(45, backItem, () -> {
            playClickSound();
            plugin.getGUIManager().openMainHub(getPlayer());
        });
        ItemStack saveItem = new ItemBuilder(Material.EMERALD)
                .name(tr("gui.config.save.name"))
                .lore(
                        tr("gui.config.save.lore1"),
                        "",
                        tr("gui.config.save.lore2"),
                        tr("gui.config.save.lore3"),
                        tr("gui.config.save.lore4"),
                        "",
                        tr("gui.config.save.lore5")
                )
                .glow(true)
                .build();
        setItem(49, saveItem, () -> {
            if (!requirePermission()) return;
            ConfigSnapshot snapshot = snapshotConfig(config);
            try {
                applyDraft(config);
                config.saveConfig();
                plugin.refreshStatisticsAutosaveSchedule();
                sendMessage(tr("gui.config.save.success"));
                sendMessage(tr("gui.config.save.success_info"));
                playSuccessSound();
            } catch (IOException | RuntimeException e) {
                restoreSnapshot(config, snapshot);
                this.plugin.getLogger().log(Level.WARNING, "Failed to save configuration", e);
                sendMessage(tr("gui.config.save.failed"));
                playErrorSound();
            }
        });
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.config.close.name"))
                .lore(tr("gui.config.close.lore"))
                .build();
        setItem(53, closeItem, () -> {
            playClickSound();
            getPlayer().closeInventory();
        });
    }

    private String getStatusColor(boolean enabled) {
        return enabled ? "<green>" : "<red>";
    }

    private ItemStack createToggleItem(
            boolean enabled,
            String name,
            String description,
            Material enabledMaterial,
            Material disabledMaterial) {
        String status = enabled ? tr(TR_ENABLED) : tr(TR_DISABLED);
        String action = enabled ? tr(TR_DISABLE) : tr(TR_ENABLE);

        return MenuItemFactory.createToggleItem(
            enabled,
            name,
            description,
            tr("gui.config.toggle.status", Map.of(CTX_STATUS, status)),
            tr("gui.config.toggle.click", Map.of(CTX_ACTION, action)),
            enabledMaterial,
            disabledMaterial
        );
    }

    private Material getRewardDeliveryModeMaterial(RewardDeliveryMode mode) {
        return switch (mode) {
            case VANILLA_RETRIEVE -> Material.FISHING_ROD;
            case INVENTORY -> Material.CHEST;
            case DROP_AT_PLAYER -> Material.DROPPER;
        };
    }

    private String getRewardDeliveryModeKey(RewardDeliveryMode mode) {
        return switch (mode) {
            case VANILLA_RETRIEVE -> "gui.config.delivery_mode.vanilla";
            case INVENTORY -> "gui.config.delivery_mode.inventory";
            case DROP_AT_PLAYER -> "gui.config.delivery_mode.player_drop";
        };
    }

    private String getRewardDeliveryModeDescriptionKey(RewardDeliveryMode mode) {
        return switch (mode) {
            case VANILLA_RETRIEVE -> "gui.config.delivery_mode.vanilla_desc";
            case INVENTORY -> "gui.config.delivery_mode.inventory_desc";
            case DROP_AT_PLAYER -> "gui.config.delivery_mode.player_drop_desc";
        };
    }

    private ConfigSnapshot snapshotConfig(ConfigManager config) {
        return new ConfigSnapshot(
            config.useSounds(),
            config.useParticles(),
            config.trackStatistics(),
            config.enableBiomeSpecificDrops(),
            config.getRewardDeliveryMode(),
            config.usePermissions(),
            config.isDebugMode(),
            config.getStatsSaveInterval(),
            config.getCatchParticle(),
            config.getBubbleParticle(),
            config.getSuccessParticle(),
            config.getXpParticle()
        );
    }

    private void applyDraft(ConfigManager config) {
        config.setSoundsEnabled(draftSoundsEnabled);
        config.setParticlesEnabled(draftParticlesEnabled);
        config.setStatisticsEnabled(draftStatisticsEnabled);
        config.setBiomeDropsEnabled(draftBiomeDropsEnabled);
        config.setRewardDeliveryMode(draftRewardDeliveryMode);
        config.setPermissionsEnabled(draftPermissionsEnabled);
        config.setDebugEnabled(draftDebugEnabled);
        config.setStatsSaveInterval(draftStatsSaveInterval);
        config.setCatchParticle(draftCatchParticle);
        config.setBubbleParticle(draftBubbleParticle);
        config.setSuccessParticle(draftSuccessParticle);
        config.setXpParticle(draftXpParticle);
        plugin.applyDropRuntimeSettings();
    }

    private void restoreSnapshot(ConfigManager config, ConfigSnapshot snapshot) {
        config.setSoundsEnabled(snapshot.soundsEnabled());
        config.setParticlesEnabled(snapshot.particlesEnabled());
        config.setStatisticsEnabled(snapshot.statisticsEnabled());
        config.setBiomeDropsEnabled(snapshot.biomeDropsEnabled());
        config.setRewardDeliveryMode(snapshot.rewardDeliveryMode());
        config.setPermissionsEnabled(snapshot.permissionsEnabled());
        config.setDebugEnabled(snapshot.debugEnabled());
        config.setStatsSaveInterval(snapshot.statsSaveInterval());
        config.setCatchParticle(snapshot.catchParticle());
        config.setBubbleParticle(snapshot.bubbleParticle());
        config.setSuccessParticle(snapshot.successParticle());
        config.setXpParticle(snapshot.xpParticle());
        plugin.applyDropRuntimeSettings();
    }

    private String cycleParticle(String currentParticle) {
        return ParticleOptions.nextSuggested(currentParticle);
    }

    private String formatLanguageName(String code) {
        if (code == null || code.isEmpty()) {
            return "<white>Unknown</white>";
        }
        String normalized = code.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "en", "en_us" -> "<white>English</white>";
            case "ja", "ja_jp", "jp" -> "<white>日本語</white>";
            default -> "<white>" + code.toUpperCase(Locale.ROOT) + "</white>";
        };
    }
}
