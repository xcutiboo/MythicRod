package io.xcutiboo.mythicrod.paper.commands;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.config.RewardDeliveryMode;
import io.xcutiboo.mythicrod.constants.MythicRodKeys;
import io.xcutiboo.mythicrod.constants.PermissionNodes;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.paper.item.RodFactory;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.paper.util.ParticleOptions;
import io.xcutiboo.mythicrod.paper.util.StringFormatting;
import io.xcutiboo.mythicrod.stats.PlayerStats;
import io.xcutiboo.mythicrod.text.ConfiguredText;
import net.kyori.adventure.text.Component;

public class BrigadierCommandManager {
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_SECONDS = "seconds";
    private static final String KEY_COUNT = "count";
    private static final String KEY_PLAYER = "player";
    private static final String KEY_LIMIT = "limit";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_DROPS = "drops";
    private static final String TR_GENERAL_ERROR = "general.error";
    private static final String TR_PLAYER_ONLY = "general.player_only";
    private static final String KEY_ERROR = "error";
    private static final String TIER_LEGENDARY = "legendary";
    private static final String TR_CONFIG_SAVE_FAILED = "command.config.save-failed";
    private static final String BIOME_PREFIX = "biome_";
    private static final String CLOSE_WHITE = "</white>";
    private static final String ERR_PREFIX = "<red>✗ <white>";
    private static final String PATH_SEP_WHITE = "</white>/<white>";
    private static final String WARN_PREFIX = "<yellow>⚠ <white>";
    private static final String GRAY_PREFIX = "<gray>";

    private final MythicRod plugin;
    private final RodFactory rodFactory;

    public BrigadierCommandManager(MythicRod plugin) {
        this.plugin = plugin;
        this.rodFactory = new RodFactory(plugin);
    }

    private String tr(CommandSender sender, String key) {
        if (sender instanceof Player player) {
            return plugin.getLanguageManager().trForPlayer(player.getUniqueId(), key);
        }
        return plugin.getLanguageManager().tr(key);
    }

    private String tr(CommandSender sender, String key, Map<String, String> args) {
        if (sender instanceof Player player) {
            return plugin.getLanguageManager().trForPlayer(player.getUniqueId(), key, args);
        }
        return plugin.getLanguageManager().tr(key, args);
    }

    public void initialize() {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            LiteralCommandNode<CommandSourceStack> mythicrodCommand = buildMythicRodCommand();
            commands.register(mythicrodCommand, "Main MythicRod command", List.of("mr", "mrod"));
        });
    }

    private LiteralCommandNode<CommandSourceStack> buildMythicRodCommand() {
        return Commands.literal("mythicrod")
            .requires(source -> source.getSender().hasPermission(PermissionNodes.COMMAND))
            .executes(this::executeDefault)
            .then(Commands.literal("gui")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.GUI))
                .executes(this::executeGui))
            .then(Commands.literal("menu")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.GUI))
                .executes(this::executeGui))
            .then(Commands.literal("rod")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.GUI))
                .executes(context -> executeMenu(context, "rod", "command.rod.opened"))
                .then(Commands.literal("inspect")
                    .requires(source -> source.getSender().hasPermission(PermissionNodes.ADMIN_DEBUG))
                    .executes(this::executeRodInspect)))
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.ADMIN_RELOAD))
                .executes(this::executeReload))
            .then(Commands.literal("config")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.ADMIN_CONFIG))
                .executes(this::executeConfigOverview)
                .then(Commands.literal("sounds")
                    .then(Commands.argument(KEY_ENABLED, BoolArgumentType.bool())
                        .executes(context -> executeBooleanConfig(
                            context,
                            "command.config.settings.sounds",
                            plugin.getConfigManager()::useSounds,
                            plugin.getConfigManager()::setSoundsEnabled,
                            null
                        ))))
                .then(Commands.literal("particles")
                    .then(Commands.argument(KEY_ENABLED, BoolArgumentType.bool())
                        .executes(context -> executeBooleanConfig(
                            context,
                            "command.config.settings.particles",
                            plugin.getConfigManager()::useParticles,
                            plugin.getConfigManager()::setParticlesEnabled,
                            null
                        ))))
                .then(Commands.literal("statistics")
                    .then(Commands.argument(KEY_ENABLED, BoolArgumentType.bool())
                        .executes(context -> executeBooleanConfig(
                            context,
                            "command.config.settings.statistics",
                            plugin.getConfigManager()::trackStatistics,
                            plugin.getConfigManager()::setStatisticsEnabled,
                            null
                        ))))
                .then(Commands.literal("biome-drops")
                    .then(Commands.argument(KEY_ENABLED, BoolArgumentType.bool())
                        .executes(context -> executeBooleanConfig(
                            context,
                            "command.config.settings.biome-drops",
                            plugin.getConfigManager()::enableBiomeSpecificDrops,
                            plugin.getConfigManager()::setBiomeDropsEnabled,
                            plugin::applyDropRuntimeSettings
                        ))))
                .then(Commands.literal("permissions")
                    .then(Commands.argument(KEY_ENABLED, BoolArgumentType.bool())
                        .executes(context -> executeBooleanConfig(
                            context,
                            "command.config.settings.permissions",
                            plugin.getConfigManager()::usePermissions,
                            plugin.getConfigManager()::setPermissionsEnabled,
                            plugin::applyDropRuntimeSettings
                        ))))
                .then(Commands.literal("debug")
                    .then(Commands.argument(KEY_ENABLED, BoolArgumentType.bool())
                        .executes(context -> executeBooleanConfig(
                            context,
                            "command.config.settings.debug",
                            plugin.getConfigManager()::isDebugMode,
                            plugin.getConfigManager()::setDebugEnabled,
                            plugin::applyDropRuntimeSettings
                        ))))
                .then(Commands.literal("delivery-mode")
                    .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests(this::suggestRewardDeliveryModes)
                        .executes(this::executeDeliveryModeConfig)))
                .then(Commands.literal("stats-save-interval")
                    .then(Commands.argument(KEY_SECONDS, IntegerArgumentType.integer(60, 3600))
                        .executes(this::executeStatsSaveIntervalConfig))))
            .then(Commands.literal("stats")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.STATS_VIEW))
                .executes(this::executeStatsOwnPlayer)
                .then(Commands.literal("reset")
                    .requires(source -> source.getSender().hasPermission(PermissionNodes.ADMIN_CONFIG))
                    .then(Commands.argument(KEY_PLAYER, StringArgumentType.word())
                        .suggests(this::suggestKnownStatsPlayers)
                        .executes(this::executeStatsReset)))
                .then(Commands.argument(KEY_PLAYER, StringArgumentType.word())
                    .requires(source -> source.getSender().hasPermission(PermissionNodes.STATS_VIEW_OTHERS))
                    .suggests(this::suggestKnownStatsPlayers)
                    .executes(this::executeStatsSpecificPlayer)))
            .then(Commands.literal("top")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.STATS_LEADERBOARD))
                .executes(context -> executeTop(context.getSource(), 10))
                .then(Commands.argument(KEY_LIMIT, IntegerArgumentType.integer(1, 50))
                    .executes(context -> executeTop(context.getSource(),
                        IntegerArgumentType.getInteger(context, KEY_LIMIT)))))
            .then(Commands.literal("give")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.ADMIN_GIVE))
                .then(Commands.argument(KEY_PLAYER, StringArgumentType.word())
                    .suggests(this::suggestOnlinePlayers)
                    .then(Commands.argument("tier", StringArgumentType.word())
                        .suggests(this::suggestRodTiers)
                        .executes(this::executeGive))))
            .then(Commands.literal(KEY_DROPS)
                .requires(source -> source.getSender().hasPermission(PermissionNodes.DROPS_VIEW))
                .executes(this::executeDrops)
                .then(Commands.argument(KEY_CATEGORY, StringArgumentType.word())
                    .suggests(this::suggestDropCategories)
                    .executes(this::executeDropsCategory)))
            .then(Commands.literal("debug")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.ADMIN_DEBUG))
                .executes(this::executeDebug))
            .then(Commands.literal("validate")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.ADMIN_CONFIG))
                .executes(this::executeValidate))
            .then(Commands.literal("testroll")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.ADMIN_DEBUG))
                .executes(this::executeTestRoll)
                .then(Commands.argument("biome", StringArgumentType.string())
                    .suggests(this::suggestBiomes)
                    .executes(this::executeTestRoll)
                    .then(Commands.argument(KEY_COUNT, IntegerArgumentType.integer(1, 10000))
                        .executes(this::executeTestRoll))))
            .then(Commands.literal("particle")
                .requires(source -> source.getSender().hasPermission(PermissionNodes.ADMIN_CONFIG))
                .executes(this::executeParticleInfo)
                .then(Commands.literal("catch")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(this::suggestParticles)
                        .executes(this::executeParticleCatch)))
                .then(Commands.literal("bubble")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(this::suggestParticles)
                        .executes(this::executeParticleBubble)))
                .then(Commands.literal("success")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(this::suggestParticles)
                        .executes(this::executeParticleSuccess)))
                .then(Commands.literal("xp")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(this::suggestParticles)
                        .executes(this::executeParticleXp))))
            .then(Commands.literal("help")
                .executes(this::executeHelp))
            .build();
    }

    private int executeDefault(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().getSender() instanceof Player player
                && player.hasPermission(PermissionNodes.GUI)) {
                return plugin.getGUIManager().openMenu(player, "main") ? Command.SINGLE_SUCCESS : 0;
            }
            return executeHelp(context);
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_GENERAL_ERROR));
            playErrorSound(context.getSource().getSender());
            plugin.getLogger().log(Level.SEVERE, "Error executing default command", e);
            return 0;
        }
    }

    private int executeGui(CommandContext<CommandSourceStack> context) {
        return executeMenu(context, "main", "command.gui.opened");
    }

    private int executeMenu(CommandContext<CommandSourceStack> context, String menuId, String successKey) {
        try {
            if (context.getSource().getSender() instanceof Player player) {
                if (!plugin.getGUIManager().openMenu(player, menuId)) {
                    playErrorSound(player);
                    return 0;
                }
                sendMessage(player, tr(player, successKey));
                playSuccessSound(player);
                return Command.SINGLE_SUCCESS;
            }
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_PLAYER_ONLY));
            playErrorSound(context.getSource().getSender());
            return 0;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_GENERAL_ERROR));
            playErrorSound(context.getSource().getSender());
            plugin.getLogger().log(Level.SEVERE, e, () -> "Error executing menu command for " + menuId);
            return 0;
        }
    }

    private int executeGive(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            String playerName = StringArgumentType.getString(context, KEY_PLAYER);
            String tier = StringArgumentType.getString(context, "tier");
            if (tier == null || tier.isEmpty()) {
                sendMessage(sender, tr(sender, "command.give.tier-missing"));
                playErrorSound(sender);
                return 0;
            }

            Player target = Bukkit.getPlayer(playerName);
            if (target == null || !target.isOnline()) {
                sendMessage(sender, tr(sender, "command.player_not_found",
                    Map.of(KEY_PLAYER, playerName)));
                playErrorSound(sender);
                return 0;
            }

            ItemStack rod = buildRodForTier(tier);
            if (rod == null) {
                sendMessage(sender, tr(sender, "command.give.invalid-tier",
                    Map.of("tier", tier)));
                playErrorSound(sender);
                return 0;
            }

            if (rod.getType().isAir()) {
                sendMessage(sender, tr(sender, "command.give.rod-creation-failed"));
                playErrorSound(sender);
                return 0;
            }

            deliverRodOnTargetThread(sender, target, rod.clone(), tier);
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException e) {
            sendMessage(sender, tr(sender, "command.give.give-failed",
                Map.of(KEY_ERROR, e.getMessage())));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, "Error executing give command", e);
            return 0;
        }
    }

    private ItemStack buildRodForTier(String tier) {
        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "basic" -> rodFactory.createBasicRod();
            case "advanced" -> rodFactory.createAdvancedRod();
            case TIER_LEGENDARY -> rodFactory.createLegendaryRod();
            default -> null;
        };
    }

    private void deliverRodOnTargetThread(CommandSender sender, Player target, ItemStack rod, String tier) {
        UUID targetId = target.getUniqueId();
        String targetName = target.getName();
        Runnable delivery = () -> {
            Player onlineTarget = Bukkit.getPlayer(targetId);
            if (onlineTarget == null || !onlineTarget.isOnline()) {
                sendMessage(sender, tr(sender, "command.give.target-offline",
                    Map.of(KEY_PLAYER, targetName)));
                playErrorSound(sender);
                return;
            }

            try {
                Map<Integer, ItemStack> leftovers = onlineTarget.getInventory().addItem(rod);
                if (!leftovers.isEmpty()) {
                    sendMessage(sender, tr(sender, "command.give.inventory-full",
                        Map.of(KEY_PLAYER, onlineTarget.getName())));
                    sendMessage(onlineTarget, tr(onlineTarget, "command.give.inventory-full-self"));
                    playErrorSound(sender);
                    playErrorSound(onlineTarget);
                    return;
                }
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to add rod to inventory", e);
                sendMessage(sender, tr(sender, "command.give.give-failed",
                    Map.of(KEY_ERROR, e.getMessage())));
                playErrorSound(sender);
                return;
            }

            sendMessage(sender, tr(sender, "command.give.sender-success",
                Map.of("tier", tier, KEY_PLAYER, onlineTarget.getName())));
            sendMessage(onlineTarget, tr(onlineTarget, "command.give.target-success",
                Map.of("tier", tier)));
            playSuccessSound(sender);
            playSuccessSound(onlineTarget);
        };

        plugin.getPlatformScheduler().runForPlayer(new PaperPlayer(target), delivery);
    }

    private int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CommandSender sender = source.getSender();
        try {
            sendMessage(sender, tr(sender, "command.reload.start"));

            if (!plugin.reload()) {
                if (plugin.isReloadInProgress()) {
                    sendMessage(sender, tr(sender, "command.reload.already_running"));
                    playErrorSound(sender);
                    return 0;
                }

                sendMessage(sender, tr(sender, "command.reload.failed",
                    Map.of(KEY_ERROR, "See server log for details")));
                playErrorSound(sender);
                plugin.getLogger().log(Level.SEVERE, "Reload command reported failure");
                return 0;
            }

            sendMessage(sender, tr(sender, "command.reload.success"));
            playSuccessSound(sender);
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException e) {
            sendMessage(sender, tr(sender, "command.reload.failed",
                Map.of(KEY_ERROR, e.getMessage())));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, "Unexpected error while executing reload command", e);
            return 0;
        }
    }

    private int executeConfigOverview(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        ConfigManager config = plugin.getConfigManager();
        try {
            sendMessage(sender, tr(sender, "command.config.header"));
            sendConfigLine(sender, "command.config.settings.sounds", configStatus(sender, config.useSounds()));
            sendConfigLine(sender, "command.config.settings.particles", configStatus(sender, config.useParticles()));
            sendConfigLine(sender, "command.config.settings.statistics", configStatus(sender, config.trackStatistics()));
            sendConfigLine(sender, "command.config.settings.biome-drops", configStatus(sender, config.enableBiomeSpecificDrops()));
            sendConfigLine(sender, "command.config.settings.permissions", configStatus(sender, config.usePermissions()));
            sendConfigLine(sender, "command.config.settings.debug", configStatus(sender, config.isDebugMode()));
            sendConfigLine(sender, "command.config.settings.delivery-mode", config.getRewardDeliveryMode().getConfigValue());
            sendConfigLine(sender, "command.config.settings.stats-save-interval",
                tr(sender, "command.config.seconds", Map.of(KEY_SECONDS, String.valueOf(config.getStatsSaveInterval()))));
            sendMessage(sender, tr(sender, "command.config.usage"));
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException e) {
            sendMessage(sender, tr(sender, TR_GENERAL_ERROR));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, "Error executing config overview command", e);
            return 0;
        }
    }

    private void sendConfigLine(CommandSender sender, String settingKey, String value) {
        sendMessage(sender, tr(sender, "command.config.line",
            Map.of(
                "setting", tr(sender, settingKey),
                "value", value
            )));
    }

    private String configStatus(CommandSender sender, boolean enabled) {
        return tr(sender, enabled ? "general.enabled" : "general.disabled");
    }

    private int executeBooleanConfig(
        CommandContext<CommandSourceStack> context,
        String settingKey,
        BooleanSupplier currentValue,
        Consumer<Boolean> setter,
        Runnable afterChange
    ) {
        CommandSender sender = context.getSource().getSender();
        boolean previousValue = currentValue.getAsBoolean();
        boolean newValue = BoolArgumentType.getBool(context, KEY_ENABLED);
        try {
            setter.accept(newValue);
            if (afterChange != null) {
                afterChange.run();
            }
            plugin.getConfigManager().saveConfig();
            sendMessage(sender, tr(sender, "command.config.boolean-set",
                Map.of(
                    "setting", tr(sender, settingKey),
                    "value", configStatus(sender, newValue)
                )));
            playSuccessSound(sender);
            return Command.SINGLE_SUCCESS;
        } catch (IOException | RuntimeException e) {
            setter.accept(previousValue);
            if (afterChange != null) {
                afterChange.run();
            }
            sendMessage(sender, tr(sender, TR_CONFIG_SAVE_FAILED,
                Map.of(KEY_ERROR, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, e, () -> "Failed to update config setting " + settingKey);
            return 0;
        }
    }

    private int executeDeliveryModeConfig(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        ConfigManager config = plugin.getConfigManager();
        RewardDeliveryMode previousMode = config.getRewardDeliveryMode();
        String rawMode = StringArgumentType.getString(context, "mode");
        RewardDeliveryMode newMode = parseRewardDeliveryMode(rawMode);
        if (newMode == null) {
            sendMessage(sender, tr(sender, "command.config.invalid-delivery-mode",
                Map.of(
                    "mode", rawMode,
                    "modes", rewardDeliveryModeList()
                )));
            playErrorSound(sender);
            return 0;
        }

        try {
            config.setRewardDeliveryMode(newMode);
            config.saveConfig();
            sendMessage(sender, tr(sender, "command.config.delivery-set",
                Map.of("mode", newMode.getConfigValue())));
            playSuccessSound(sender);
            return Command.SINGLE_SUCCESS;
        } catch (IOException | RuntimeException e) {
            config.setRewardDeliveryMode(previousMode);
            sendMessage(sender, tr(sender, TR_CONFIG_SAVE_FAILED,
                Map.of(KEY_ERROR, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, "Failed to update reward delivery mode", e);
            return 0;
        }
    }

    private int executeStatsSaveIntervalConfig(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        ConfigManager config = plugin.getConfigManager();
        int previousInterval = config.getStatsSaveInterval();
        int newInterval = IntegerArgumentType.getInteger(context, KEY_SECONDS);
        try {
            config.setStatsSaveInterval(newInterval);
            config.saveConfig();
            plugin.refreshStatisticsAutosaveSchedule();
            sendMessage(sender, tr(sender, "command.config.interval-set",
                Map.of(KEY_SECONDS, String.valueOf(config.getStatsSaveInterval()))));
            playSuccessSound(sender);
            return Command.SINGLE_SUCCESS;
        } catch (IOException | RuntimeException e) {
            config.setStatsSaveInterval(previousInterval);
            plugin.refreshStatisticsAutosaveSchedule();
            sendMessage(sender, tr(sender, TR_CONFIG_SAVE_FAILED,
                Map.of(KEY_ERROR, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, "Failed to update stats save interval", e);
            return 0;
        }
    }

    private RewardDeliveryMode parseRewardDeliveryMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return null;
        }

        String normalizedMode = rawMode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        RewardDeliveryMode configuredMode = RewardDeliveryMode.fromConfigValue(normalizedMode);
        if (configuredMode != null) {
            return configuredMode;
        }

        return switch (normalizedMode) {
            case "vanilla", "retrieve" -> RewardDeliveryMode.VANILLA_RETRIEVE;
            case KEY_PLAYER, "drop", "drop_player" -> RewardDeliveryMode.DROP_AT_PLAYER;
            default -> null;
        };
    }

    private CompletableFuture<Suggestions> suggestRewardDeliveryModes(
        @SuppressWarnings("unused") CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (RewardDeliveryMode mode : RewardDeliveryMode.values()) {
            suggestIfMatching(builder, remaining, mode.getConfigValue());
        }
        suggestIfMatching(builder, remaining, "vanilla");
        suggestIfMatching(builder, remaining, "drop");
        return builder.buildFuture();
    }

    private String rewardDeliveryModeList() {
        return String.join(", ",
            RewardDeliveryMode.VANILLA_RETRIEVE.getConfigValue(),
            RewardDeliveryMode.INVENTORY.getConfigValue(),
            RewardDeliveryMode.DROP_AT_PLAYER.getConfigValue());
    }

    private int executeStatsOwnPlayer(CommandContext<CommandSourceStack> context) {
        try {
            CommandSender sender = context.getSource().getSender();
            if (!(sender instanceof Player)) {
                sendMessage(sender, tr(sender, "stats.console-usage"));
                playErrorSound(sender);
                return 0;
            }
            handleStats(sender, new String[0]);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_GENERAL_ERROR));
            playErrorSound(context.getSource().getSender());
            plugin.getLogger().log(Level.SEVERE, "Error executing stats command", e);
            return 0;
        }
    }

    private int executeStatsSpecificPlayer(CommandContext<CommandSourceStack> context) {
        try {
            String playerName = StringArgumentType.getString(context, KEY_PLAYER);
            handleStats(context.getSource().getSender(), new String[]{"stats", playerName});
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_GENERAL_ERROR));
            playErrorSound(context.getSource().getSender());
            plugin.getLogger().log(Level.SEVERE, "Error executing stats command", e);
            return 0;
        }
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().trackStatistics()) {
            sendMessage(sender, tr(sender, "stats.disabled"));
            return;
        }
        StatsTarget target;
        if (args.length > 1) {
            String playerName = args[1];
            target = resolveStatsTarget(playerName);
            if (target == null) {
                sendMessage(sender, tr(sender, "stats.player-not-found",
                    Map.of(KEY_PLAYER, playerName)));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sendMessage(sender, tr(sender, "stats.console-usage"));
                return;
            }
            Player player = (Player) sender;
            target = new StatsTarget(player.getUniqueId(), player.getName());
        }
        PlayerStats stats = plugin.getStatisticsManager().getStats(target.playerId());
        if (stats == null) {
            sendMessage(sender, tr(sender, "stats.no-stats"));
            return;
        }

        sendMessage(sender, tr(sender, "stats.header",
            Map.of(KEY_PLAYER, target.playerName())));
        sendMessage(sender, tr(sender, "stats.total-catches",
            Map.of("total", String.valueOf(stats.getTotalCaught()))));
        sendMessage(sender, tr(sender, "stats.rare-catches",
            Map.of("rare", String.valueOf(stats.getRareCaught()))));
        Map<String, Integer> tierCounts = stats.getTierCounts();
        if (!tierCounts.isEmpty()) {
            sendMessage(sender, tr(sender, "stats.tier-breakdown"));
            tierCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> sendMessage(sender, tr(sender, "stats.tier-count",
                        Map.of("tier", StringFormatting.formatMaterialName(entry.getKey()),
                            KEY_COUNT, String.valueOf(entry.getValue())))));
        }
    }

    private StatsTarget resolveStatsTarget(String playerName) {
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return new StatsTarget(onlinePlayer.getUniqueId(), onlinePlayer.getName());
        }

        OfflinePlayer cachedPlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (cachedPlayer != null) {
            String resolvedName = cachedPlayer.getName() != null ? cachedPlayer.getName() : playerName;
            return new StatsTarget(cachedPlayer.getUniqueId(), resolvedName);
        }

        return plugin.getStatisticsManager().getAllStats().values().stream()
            .filter(stats -> stats.getPlayerName().equalsIgnoreCase(playerName))
            .findFirst()
            .map(stats -> new StatsTarget(
                stats.getPlayerUuid(),
                stats.getPlayerName()
            ))
            .orElse(null);
    }

    private List<String> getKnownStatsPlayerNames() {
        Set<String> playerNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Bukkit.getOnlinePlayers().forEach(player -> playerNames.add(player.getName()));
        plugin.getStatisticsManager().getAllStats().values().forEach(stats -> {
            String playerName = stats.getPlayerName();
            if (!playerName.isBlank()) {
                playerNames.add(playerName);
            }
        });
        return List.copyOf(playerNames);
    }

    private record StatsTarget(UUID playerId, String playerName) {
    }

    private int executeStatsReset(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            String playerArg = StringArgumentType.getString(context, KEY_PLAYER);
            StatsTarget target = resolveStatsTarget(playerArg);
            if (target == null) {
                sendMessage(sender, tr(sender, "command.player_not_found",
                    Map.of(KEY_PLAYER, playerArg)));
                playErrorSound(sender);
                return 0;
            }

            boolean reset = plugin.getStatisticsManager().resetStats(target.playerId());
            if (!reset) {
                sendMessage(sender, tr(sender, "stats.reset.no-entry",
                    Map.of(KEY_PLAYER, target.playerName())));
                playErrorSound(sender);
                return 0;
            }

            sendMessage(sender, tr(sender, "stats.reset.success",
                Map.of(KEY_PLAYER, target.playerName())));
            playSuccessSound(sender);
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException e) {
            sendMessage(sender, tr(sender, TR_GENERAL_ERROR));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, "Error executing stats reset command", e);
            return 0;
        }
    }

    private int executeTop(CommandSourceStack source, int limit) {
        try {
            if (!plugin.getConfigManager().trackStatistics()) {
                sendMessage(source.getSender(), tr(source.getSender(), "stats.disabled"));
                return 0;
            }

            final int MIN_LIMIT = 1;
            final int MAX_LIMIT = 50;

            if (limit < MIN_LIMIT) {
                limit = MIN_LIMIT;
            } else if (limit > MAX_LIMIT) {
                limit = MAX_LIMIT;
                sendMessage(source.getSender(), tr(source.getSender(), "stats.limit-capped",
                    Map.of(KEY_LIMIT, String.valueOf(MAX_LIMIT))));
            }

            Optional<List<PlayerStats>> topFishersOpt = readTopFishers(source.getSender(), limit);
            if (topFishersOpt.isEmpty()) return 0;
            List<PlayerStats> topFishers = topFishersOpt.get();
            if (topFishers.isEmpty()) {
                sendMessage(source.getSender(), tr(source.getSender(), "stats.no-stats"));
                return 0;
            }

            sendMessage(source.getSender(), tr(source.getSender(), "stats.top-header",
                Map.of(KEY_LIMIT, String.valueOf(Math.min(limit, topFishers.size())))));

            int rank = 1;
            for (PlayerStats ps : topFishers) {
                renderLeaderboardEntry(source.getSender(), ps, rank);
                rank++;
            }
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException e) {
            sendMessage(source.getSender(), tr(source.getSender(), TR_GENERAL_ERROR));
            plugin.getLogger().log(Level.SEVERE, "Error executing top command", e);
            return 0;
        }
    }

    private Optional<List<PlayerStats>> readTopFishers(CommandSender sender, int limit) {
        try {
            return Optional.of(plugin.getStatisticsManager().getTopFishers(limit));
        } catch (RuntimeException statsError) {
            sendMessage(sender, tr(sender, "stats.retrieve-failed",
                Map.of(KEY_ERROR, statsError.getMessage())));
            playErrorSound(sender);
            plugin.getLogger().log(Level.WARNING, "Failed to read top fishers", statsError);
            return Optional.empty();
        }
    }

    private void renderLeaderboardEntry(CommandSender sender, PlayerStats ps, int rank) {
        try {
            String name = ps.getPlayerName().isEmpty() ? resolveOfflineName(ps) : ps.getPlayerName();
            sendMessage(sender, tr(sender, "stats.top-entry",
                Map.of("rank", String.valueOf(rank), KEY_PLAYER, name, "catches", String.valueOf(ps.getTotalCaught()))));
        } catch (RuntimeException entryError) {
            plugin.getLogger().log(Level.WARNING, entryError,
                () -> "Failed to format leaderboard entry: " + entryError.getMessage());
        }
    }

    private String resolveTestRollBiome(Player player, String biomeArg) {
        if (biomeArg == null || biomeArg.isBlank()) {
            return player.getWorld().getComputedBiome(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
            ).getKey().asString();
        }
        NamespacedKey biomeKey = parseRegistryKey(biomeArg);
        return biomeKey != null ? biomeKey.asString() : biomeArg;
    }

    private int runTestRolls(PaperPlayer platformPlayer, String biome, int count,
                              Map<String, Integer> tierCounts, Map<String, Integer> identifierCounts) {
        int nullRolls = 0;
        for (int i = 0; i < count; i++) {
            CustomDrop drop = plugin.getDropManager().getRandomDrop(platformPlayer, biome, 1.0D);
            if (drop == null) {
                nullRolls++;
                continue;
            }
            tierCounts.merge(drop.getTier(), 1, Integer::sum);
            identifierCounts.merge(drop.getIdentifier(), 1, Integer::sum);
        }
        return nullRolls;
    }

    private String optionalStringArg(CommandContext<CommandSourceStack> context, String name) {
        try {
            return StringArgumentType.getString(context, name);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    private int optionalIntArg(CommandContext<CommandSourceStack> context, String name, int fallback) {
        try {
            return IntegerArgumentType.getInteger(context, name);
        } catch (IllegalArgumentException _) {
            return fallback;
        }
    }

    private String resolveOfflineName(PlayerStats ps) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ps.getPlayerUuid());
            return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
        } catch (RuntimeException nameError) {
            plugin.getLogger().log(Level.WARNING, nameError,
                () -> "Failed to resolve player name for " + ps.getPlayerUuid());
            return "Unknown";
        }
    }

    private int executeDrops(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().getSender() instanceof Player player) {
                return plugin.getGUIManager().openMenu(player, KEY_DROPS) ? Command.SINGLE_SUCCESS : 0;
            }
            handleDrops(context.getSource().getSender(), new String[0]);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_GENERAL_ERROR));
            playErrorSound(context.getSource().getSender());
            plugin.getLogger().log(Level.SEVERE, "Error executing drops command", e);
            return 0;
        }
    }

    private int executeDropsCategory(CommandContext<CommandSourceStack> context) {
        try {
            CommandSender sender = context.getSource().getSender();
            String category = normalizeDropCategory(StringArgumentType.getString(context, KEY_CATEGORY));
            Map<String, List<CustomDrop>> categories = plugin.getDropManager().getDropCategories();
            if (categories.get(category) == null) {
                sendUnknownDropCategory(sender, category);
                playErrorSound(sender);
                return 0;
            }

            if (sender instanceof Player player) {
                return plugin.getGUIManager().openMenu(player, KEY_DROPS, Map.of(
                    "viewing_category", Boolean.TRUE,
                    KEY_CATEGORY, category
                )) ? Command.SINGLE_SUCCESS : 0;
            }

            displayDropCategory(sender, category);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_GENERAL_ERROR));
            playErrorSound(context.getSource().getSender());
            plugin.getLogger().log(Level.SEVERE, "Error executing drops command", e);
            return 0;
        }
    }

    private void handleDrops(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String category = normalizeDropCategory(args[1]);
            if (plugin.getDropManager().getDropCategories().get(category) != null) {
                displayDropCategory(sender, category);
            } else {
                sendUnknownDropCategory(sender, category);
            }
        } else {
            Map<String, List<CustomDrop>> categories = plugin.getDropManager().getDropCategories();
            sendMessage(sender, tr(sender, "drops.header"));
            for (String category : sortedDropCategoryIds()) {
                sendMessage(sender, tr(sender, "drops.category-entry",
                    Map.of(
                        KEY_CATEGORY, category,
                        "label", StringFormatting.formatCategoryName(category),
                        KEY_COUNT, String.valueOf(categories.get(category).size())
                    )));
            }
            sendMessage(sender, tr(sender, "drops.usage-hint"));
        }
    }

    private String normalizeDropCategory(String category) {
        if (category == null) {
            return "";
        }

        Map<String, List<CustomDrop>> categories = plugin.getDropManager().getDropCategories();
        String normalizedCategory = category.toLowerCase(Locale.ROOT);
        if (categories.get(normalizedCategory) != null) {
            return normalizedCategory;
        }

        String biomeCategory = BIOME_PREFIX + normalizedCategory;
        if (categories.get(biomeCategory) != null) {
            return biomeCategory;
        }

        return normalizedCategory;
    }

    private void sendUnknownDropCategory(CommandSender sender, String category) {
        sendMessage(sender, tr(sender, "drops.category-not-found",
            Map.of(KEY_CATEGORY, category)));
        sendMessage(sender, tr(sender, "drops.available-categories",
            Map.of("categories", String.join(", ", sortedDropCategoryIds()))));
        sendMessage(sender, tr(sender, "drops.category-help"));
    }

    private void displayDropCategory(CommandSender sender, String category) {
        List<CustomDrop> drops = plugin.getDropManager().getDropCategories().get(category);
        if (drops == null || drops.isEmpty()) {
            sendMessage(sender, tr(sender, "drops.category-not-found",
                Map.of(KEY_CATEGORY, category)));
            return;
        }
        sendMessage(sender, tr(sender, "drops.category-header",
            Map.of(
                KEY_CATEGORY, category,
                "label", StringFormatting.formatCategoryName(category)
            )));
        for (CustomDrop drop : drops) {
            String name = drop.getCustomName() != null ? drop.getCustomName() : drop.getIdentifier();
            String weight = String.valueOf(drop.getWeight());
            sendMessage(sender, tr(sender, "drops.drop-entry",
                Map.of(
                    "name", name,
                    "weight", weight,
                    "amount", String.valueOf(drop.getAmount())
                )));
        }
    }

    private CompletableFuture<Suggestions> suggestDropCategories(
        @SuppressWarnings("unused") CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String category : sortedDropCategoryIds()) {
            suggestIfMatching(builder, remaining, category);
            if (category.startsWith(BIOME_PREFIX)) {
                suggestIfMatching(builder, remaining, category.substring(BIOME_PREFIX.length()));
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestKnownStatsPlayers(
        @SuppressWarnings("unused") CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        getKnownStatsPlayerNames().forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOnlinePlayers(
        @SuppressWarnings("unused") CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        Bukkit.getOnlinePlayers().forEach(player -> builder.suggest(player.getName()));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestRodTiers(
        @SuppressWarnings("unused") CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        builder.suggest("basic");
        builder.suggest("advanced");
        builder.suggest(TIER_LEGENDARY);
        return builder.buildFuture();
    }

    private void suggestIfMatching(SuggestionsBuilder builder, String remaining, String suggestion) {
        if (suggestion.startsWith(remaining)) {
            builder.suggest(suggestion);
        }
    }

    private List<String> sortedDropCategoryIds() {
        return plugin.getDropManager().getDropCategories().keySet().stream()
            .sorted(this::compareDropCategoryIds)
            .toList();
    }

    private int compareDropCategoryIds(String left, String right) {
        int rankCompare = Integer.compare(dropCategoryRank(left), dropCategoryRank(right));
        if (rankCompare != 0) {
            return rankCompare;
        }
        return StringFormatting.formatCategoryName(left)
            .compareToIgnoreCase(StringFormatting.formatCategoryName(right));
    }

    private int dropCategoryRank(String category) {
        if (category == null) {
            return 4;
        }

        String normalizedCategory = category.toLowerCase(Locale.ROOT);
        return switch (normalizedCategory) {
            case "global" -> 0;
            case "rare" -> 1;
            case TIER_LEGENDARY -> 2;
            default -> normalizedCategory.startsWith(BIOME_PREFIX) ? 3 : 4;
        };
    }

    private int executeHelp(CommandContext<CommandSourceStack> context) {
        try {
            sendHelpMessage(context.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException e) {
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_GENERAL_ERROR));
            playErrorSound(context.getSource().getSender());
            plugin.getLogger().log(Level.SEVERE, "Error executing help command", e);
            return 0;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sendMessage(sender, tr(sender, "command.help.header"));
        sendMessage(sender, tr(sender, "command.help.help"));
        if (sender instanceof Player player) {
            sendPlayerHelp(sender, player);
        } else {
            sendConsoleHelp(sender);
        }
        sendMessage(sender, tr(sender, "command.help.footer"));
    }

    private void sendPlayerHelp(CommandSender sender, Player player) {
        sendIfHasPermission(sender, player, PermissionNodes.GUI, "command.help.gui");
        sendIfHasPermission(sender, player, PermissionNodes.GUI, "command.help.rod");
        sendIfHasPermission(sender, player, PermissionNodes.STATS_VIEW, "command.help.stats");
        sendIfHasPermission(sender, player, PermissionNodes.STATS_LEADERBOARD, "command.help.top");
        sendIfHasPermission(sender, player, PermissionNodes.DROPS_VIEW, "command.help.drops");
        sendIfHasPermission(sender, player, PermissionNodes.ADMIN_RELOAD, "command.help.reload");
        sendIfHasPermission(sender, player, PermissionNodes.ADMIN_GIVE, "command.help.give");
        sendIfHasPermission(sender, player, PermissionNodes.ADMIN_DEBUG, "command.help.debug");
        sendIfHasPermission(sender, player, PermissionNodes.ADMIN_CONFIG, "command.help.config");
        sendIfHasPermission(sender, player, PermissionNodes.ADMIN_CONFIG, "command.help.particle");
    }

    private void sendIfHasPermission(CommandSender sender, Player player, String permission, String key) {
        if (player.hasPermission(permission)) {
            sendMessage(sender, tr(sender, key));
        }
    }

    private void sendConsoleHelp(CommandSender sender) {
        for (String key : List.of(
            "command.help.reload", "command.help.stats", "command.help.top",
            "command.help.drops", "command.help.give", "command.help.debug",
            "command.help.config", "command.help.particle")) {
            sendMessage(sender, tr(sender, key));
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        Component fullMessage = ConfiguredText.parse(plugin.getConfigManager().getPrefix())
            .append(ConfiguredText.parse(message));
        sender.sendMessage(fullMessage);
    }

    private void playSuccessSound(CommandSender sender) {
        playSound(sender, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
    }

    private void playErrorSound(CommandSender sender) {
        playSound(sender, Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
    }

    private void playSound(CommandSender sender, Sound sound, float volume, float pitch) {
        if (!plugin.getConfigManager().useSounds() || !(sender instanceof Player player)) {
            return;
        }

        plugin.getPlatformScheduler().runForPlayer(new PaperPlayer(player), () -> {
            if (player.isOnline()) {
                player.playSound(player, sound, SoundCategory.MASTER, volume, pitch);
            }
        });
    }

    private boolean isValidParticleType(String particleName) {
        return ParticleOptions.isConfigurableParticleName(particleName);
    }

    private CompletableFuture<Suggestions> suggestParticles(
        @SuppressWarnings("unused") CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        ParticleOptions.configurableNames().forEach(builder::suggest);
        return builder.buildFuture();
    }

    private int executeParticleInfo(CommandContext<CommandSourceStack> context) {
        try {
            CommandSender sender = context.getSource().getSender();
            sendMessage(sender, tr(sender, "command.particle.header"));
            sendMessage(sender, tr(sender, "command.particle.current"));
            sendMessage(sender, tr(sender, "command.particle.catch-line",
                Map.of("type", plugin.getConfigManager().getCatchParticle())));
            sendMessage(sender, tr(sender, "command.particle.bubble-line",
                Map.of("type", plugin.getConfigManager().getBubbleParticle())));
            sendMessage(sender, tr(sender, "command.particle.success-line",
                Map.of("type", plugin.getConfigManager().getSuccessParticle())));
            sendMessage(sender, tr(sender, "command.particle.xp-line",
                Map.of("type", plugin.getConfigManager().getXpParticle())));
            sendMessage(sender, tr(sender, "command.particle.usage-header"));
            sendMessage(sender, tr(sender, "command.particle.usage-catch"));
            sendMessage(sender, tr(sender, "command.particle.usage-bubble"));
            sendMessage(sender, tr(sender, "command.particle.usage-success"));
            sendMessage(sender, tr(sender, "command.particle.usage-xp"));
            return Command.SINGLE_SUCCESS;
        } catch (Exception _) {
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_GENERAL_ERROR));
            playErrorSound(context.getSource().getSender());
            return 0;
        }
    }

    private int executeParticleCatch(CommandContext<CommandSourceStack> context) {
        return updateParticleSetting(
            context,
            plugin.getConfigManager()::getCatchParticle,
            plugin.getConfigManager()::setCatchParticle,
            "command.particle.catch-set",
            "catch"
        );
    }

    private int executeParticleBubble(CommandContext<CommandSourceStack> context) {
        return updateParticleSetting(
            context,
            plugin.getConfigManager()::getBubbleParticle,
            plugin.getConfigManager()::setBubbleParticle,
            "command.particle.bubble-set",
            "bubble"
        );
    }

    private int executeParticleSuccess(CommandContext<CommandSourceStack> context) {
        return updateParticleSetting(
            context,
            plugin.getConfigManager()::getSuccessParticle,
            plugin.getConfigManager()::setSuccessParticle,
            "command.particle.success-set",
            "success"
        );
    }

    private int executeParticleXp(CommandContext<CommandSourceStack> context) {
        return updateParticleSetting(
            context,
            plugin.getConfigManager()::getXpParticle,
            plugin.getConfigManager()::setXpParticle,
            "command.particle.xp-set",
            "xp"
        );
    }

    private int updateParticleSetting(
        CommandContext<CommandSourceStack> context,
        Supplier<String> currentValueSupplier,
        Consumer<String> setter,
        String successKey,
        String settingName
    ) {
        CommandSender sender = context.getSource().getSender();
        String previousValue = currentValueSupplier.get();

        try {
            String type = StringArgumentType.getString(context, "type");
            String particleType = ParticleOptions.normalize(type);

            if (!isValidParticleType(particleType)) {
                sendMessage(sender, tr(sender, "command.particle.invalid-type",
                    Map.of("type", particleType)));
                playErrorSound(sender);
                return 0;
            }

            setter.accept(particleType);
            plugin.getConfigManager().saveConfig();
            sendMessage(sender, tr(sender, successKey,
                Map.of("type", particleType)));
            playSuccessSound(sender);
            return Command.SINGLE_SUCCESS;
        } catch (IOException | RuntimeException e) {
            setter.accept(previousValue);
            sendMessage(sender, tr(sender, TR_GENERAL_ERROR));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, e, () -> "Failed to update " + settingName + " particle setting");
            return 0;
        }
    }

    private int executeValidate(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            Map<String, List<CustomDrop>> categories = plugin.getDropManager().getDropCategories();
            boolean nexoAvailable = plugin.getPlatformServer() != null && plugin.getPlatformServer().isNexoEnabled();

            ValidationCounts counts = new ValidationCounts();
            sendMessage(sender, "<gold><st>            </st> <bold>MythicRod Validate</bold> <st>            </st>");

            for (Map.Entry<String, List<CustomDrop>> entry : categories.entrySet()) {
                validateCategory(sender, entry.getKey(), entry.getValue(), nexoAvailable, counts);
            }
            int problems = counts.problems + counts.duplicates;

            if (problems == 0) {
                sendMessage(sender, "<green>✓ Validated " + counts.dropsChecked + " drops across "
                    + counts.categoriesChecked + " categories - no problems found.");
                playSuccessSound(sender);
            } else {
                sendMessage(sender, "<red>✗ Found " + problems + " problem(s) across "
                    + counts.dropsChecked + " drops in " + counts.categoriesChecked + " categories.");
                sendMessage(sender, "<gray>Fix entries above, then run <yellow>/mythicrod reload</yellow>.");
                playErrorSound(sender);
            }
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException e) {
            sendMessage(sender, tr(sender, TR_GENERAL_ERROR));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, "Error executing validate command", e);
            return 0;
        }
    }

    private static final class ValidationCounts {
        int problems;
        int dropsChecked;
        int categoriesChecked;
        int duplicates;
    }

    private void validateCategory(CommandSender sender, String categoryKey, List<CustomDrop> drops,
                                   boolean nexoAvailable, ValidationCounts counts) {
        if (drops == null || drops.isEmpty()) return;
        counts.categoriesChecked++;

        Map<String, Integer> identifierCounts = new HashMap<>();
        for (CustomDrop drop : drops) {
            counts.dropsChecked++;
            String identifier = drop.getIdentifier();
            identifierCounts.merge(identifier == null ? "<null>" : identifier, 1, Integer::sum);
            counts.problems += validateDrop(sender, categoryKey, drop, nexoAvailable);
        }
        for (Map.Entry<String, Integer> idCount : identifierCounts.entrySet()) {
            if (idCount.getValue() > 1) {
                counts.duplicates++;
                sendMessage(sender, "<yellow>⚠ Duplicate identifier <white>" + idCount.getKey()
                    + "</white> appears " + idCount.getValue() + " times in category <white>"
                    + categoryKey + CLOSE_WHITE);
            }
        }
    }

    private int validateDrop(CommandSender sender, String category, CustomDrop drop, boolean nexoAvailable) {
        String identifier = drop.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            sendMessage(sender, ERR_PREFIX + category + "</white>: drop has no identifier");
            return 1;
        }
        int problems = validateBasicFields(sender, category, drop, identifier);
        problems += validateItemSource(sender, category, drop, identifier, nexoAvailable);
        problems += validateEnchantments(sender, drop, identifier);
        problems += validateBiomes(sender, drop, identifier);
        problems += validatePermission(sender, drop, identifier);
        return problems;
    }

    private int validateBasicFields(CommandSender sender, String category, CustomDrop drop, String identifier) {
        int problems = 0;
        if (drop.getWeight() <= 0) {
            sendMessage(sender, ERR_PREFIX + category + PATH_SEP_WHITE + identifier
                + "</white>: weight must be >= 1 (was " + drop.getWeight() + ")");
            problems++;
        }
        if (drop.getAmount() <= 0) {
            sendMessage(sender, ERR_PREFIX + category + PATH_SEP_WHITE + identifier
                + "</white>: amount must be >= 1 (was " + drop.getAmount() + ")");
            problems++;
        }
        return problems;
    }

    private int validateItemSource(CommandSender sender, String category, CustomDrop drop,
                                    String identifier, boolean nexoAvailable) {
        if (drop.isNexoItem()) {
            if (nexoAvailable) return 0;
            sendMessage(sender, WARN_PREFIX + category + PATH_SEP_WHITE + identifier
                + "</white>: Nexo item but Nexo plugin is not enabled");
            return 1;
        }
        Material material = Material.matchMaterial(identifier);
        if (material == null) {
            sendMessage(sender, ERR_PREFIX + category + PATH_SEP_WHITE + identifier
                + "</white>: unknown material");
            return 1;
        }
        if (!material.isItem()) {
            sendMessage(sender, ERR_PREFIX + category + PATH_SEP_WHITE + identifier
                + "</white>: material is not an obtainable item");
            return 1;
        }
        return 0;
    }

    private int validateEnchantments(CommandSender sender, CustomDrop drop, String identifier) {
        int problems = 0;
        for (Map.Entry<String, Integer> enchant : drop.getEnchantments().entrySet()) {
            if (enchant.getValue() == null || enchant.getValue() < 1) {
                sendMessage(sender, WARN_PREFIX + identifier + "</white>: enchantment '"
                    + enchant.getKey() + "' has invalid level " + enchant.getValue());
                problems++;
                continue;
            }
            NamespacedKey enchantKey = parseRegistryKey(enchant.getKey());
            if (enchantKey == null
                    || RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(enchantKey) == null) {
                sendMessage(sender, WARN_PREFIX + identifier + "</white>: unknown enchantment '"
                    + enchant.getKey() + "'");
                problems++;
            }
        }
        return problems;
    }

    private int validateBiomes(CommandSender sender, CustomDrop drop, String identifier) {
        int problems = 0;
        for (String biome : drop.getBiomes()) {
            if (biome == null || biome.isBlank()) continue;
            NamespacedKey biomeKey = parseRegistryKey(biome);
            if (biomeKey == null
                    || RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(biomeKey) == null) {
                sendMessage(sender, WARN_PREFIX + identifier + "</white>: unknown biome '"
                    + biome + "'");
                problems++;
            }
        }
        return problems;
    }

    private int validatePermission(CommandSender sender, CustomDrop drop, String identifier) {
        String permission = drop.getPermission();
        if (permission == null || permission.isBlank() || permission.startsWith("mythicrod.")) return 0;
        sendMessage(sender, WARN_PREFIX + identifier
            + "</white>: permission '" + permission + "' is outside the mythicrod.* namespace");
        return 1;
    }

    private NamespacedKey parseRegistryKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        try {
            if (trimmed.contains(":")) {
                return NamespacedKey.fromString(trimmed);
            }
            return NamespacedKey.minecraft(trimmed);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    private int executeTestRoll(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, tr(sender, TR_PLAYER_ONLY));
                playErrorSound(sender);
                return 0;
            }

            String biomeArg = optionalStringArg(context, "biome");
            int count = Math.clamp(optionalIntArg(context, KEY_COUNT, 100), 1, 10000);
            String biome = resolveTestRollBiome(player, biomeArg);

            PaperPlayer platformPlayer = new PaperPlayer(player);
            Map<String, Integer> tierCounts = new LinkedHashMap<>();
            tierCounts.put(TIER_LEGENDARY, 0);
            tierCounts.put("rare", 0);
            tierCounts.put("uncommon", 0);
            tierCounts.put("common", 0);
            Map<String, Integer> identifierCounts = new HashMap<>();
            int nullRolls = runTestRolls(platformPlayer, biome, count, tierCounts, identifierCounts);

            sendMessage(sender, "<gold><st>            </st> <bold>Test Roll</bold> <st>            </st>");
            sendMessage(sender, "<gray>Biome: <white>" + biome + "</white> · Rolls: <white>"
                + count + "</white> · No-eligible: <white>" + nullRolls + CLOSE_WHITE);

            int totalHits = count - nullRolls;
            for (Map.Entry<String, Integer> tier : tierCounts.entrySet()) {
                int tierCount = tier.getValue();
                double pct = totalHits == 0 ? 0.0D : (tierCount * 100.0D / totalHits);
                String color = tierColor(tier.getKey());
                sendMessage(sender, color + tier.getKey() + "</color>: <white>" + tierCount
                    + "</white> <gray>(" + String.format(Locale.ROOT, "%.1f", pct) + "%)");
            }

            List<Map.Entry<String, Integer>> topIdentifiers = identifierCounts.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed())
                .limit(5)
                .toList();
            if (!topIdentifiers.isEmpty()) {
                sendMessage(sender, "<gray>Top identifiers:");
                for (Map.Entry<String, Integer> id : topIdentifiers) {
                    sendMessage(sender, "<gray>  · <white>" + id.getKey()
                        + "</white> <yellow>x" + id.getValue() + "</yellow>");
                }
            }
            playSuccessSound(sender);
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException e) {
            sendMessage(sender, tr(sender, TR_GENERAL_ERROR));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, "Error executing testroll command", e);
            return 0;
        }
    }

    private String tierColor(String tier) {
        return switch (tier) {
            case TIER_LEGENDARY -> "<gold>";
            case "rare" -> "<aqua>";
            case "uncommon" -> "<green>";
            default -> GRAY_PREFIX;
        };
    }

    private int executeRodInspect(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, tr(sender, TR_PLAYER_ONLY));
                playErrorSound(sender);
                return 0;
            }

            sendMessage(sender, "<gold><st>            </st> <bold>Rod Inspect</bold> <st>            </st>");
            inspectRodSlot(sender, player, EquipmentSlot.HAND, "Main hand");
            inspectRodSlot(sender, player, EquipmentSlot.OFF_HAND, "Off hand");
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException e) {
            sendMessage(sender, tr(sender, TR_GENERAL_ERROR));
            playErrorSound(sender);
            plugin.getLogger().log(Level.SEVERE, "Error executing rod inspect command", e);
            return 0;
        }
    }

    private void inspectRodSlot(CommandSender sender, Player player, EquipmentSlot slot, String label) {
        ItemStack item = switch (slot) {
            case HAND -> player.getInventory().getItemInMainHand();
            case OFF_HAND -> player.getInventory().getItemInOffHand();
            default -> null;
        };

        if (item == null || item.getType().isAir()) {
            sendMessage(sender, GRAY_PREFIX + label + ": <dark_gray>empty");
            return;
        }
        if (item.getType() != Material.FISHING_ROD) {
            sendMessage(sender, GRAY_PREFIX + label + ": <yellow>not a fishing rod (<white>"
                + item.getType().name() + "</white>)");
            return;
        }
        if (!rodFactory.isCustomRod(item)) {
            sendMessage(sender, GRAY_PREFIX + label + ": <yellow>vanilla fishing rod");
            return;
        }

        String tier = rodFactory.getRodTier(item);
        if (tier == null || tier.isBlank()) {
            tier = MythicRodKeys.DEFAULT_ROD_TIER;
        }
        double multiplier = plugin.getConfigManager().getRodLuckMultiplier(tier);
        sendMessage(sender, GRAY_PREFIX + label + ": <green>MythicRod</green> <gray>· tier=<white>"
            + tier + "</white> · rare-luck=<white>" + String.format(Locale.ROOT, "%.2fx", multiplier)
            + CLOSE_WHITE);
    }

    private CompletableFuture<Suggestions> suggestBiomes(
        @SuppressWarnings("unused") CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (Biome biome : RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)) {
            String key = biome.getKey().asString();
            if (key.toLowerCase(Locale.ROOT).contains(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    }

    private int executeDebug(CommandContext<CommandSourceStack> context) {
        try {
            CommandSender sender = context.getSource().getSender();

            sendMessage(sender, tr(sender, "command.debug.header"));

            int categoryCount = plugin.getDropManager() != null
                ? plugin.getDropManager().getDropCategories().size()
                : 0;
            int dropCount = plugin.getDropManager() != null
                ? plugin.getDropManager().getTotalDropCount()
                : 0;
            int trackedPlayers = plugin.getStatisticsManager() != null
                ? plugin.getStatisticsManager().getAllStats().size()
                : 0;
            long totalCatches = plugin.getStatisticsManager() != null
                ? plugin.getStatisticsManager().getTotalCatches()
                : 0L;
            sendMessage(sender, tr(sender, "command.debug.runtime",
                Map.of(
                    "categories", String.valueOf(categoryCount),
                    KEY_DROPS, String.valueOf(dropCount),
                    "players", String.valueOf(trackedPlayers),
                    "catches", String.valueOf(totalCatches)
                )));
            sendMessage(sender, tr(sender, "command.debug.folia-support",
                Map.of("status", tr(sender, plugin.isFoliaRuntime() ? "general.enabled" : "general.disabled"))));

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), tr(context.getSource().getSender(), TR_GENERAL_ERROR));
            playErrorSound(context.getSource().getSender());
            plugin.getLogger().log(Level.SEVERE, "Error executing debug command", e);
            return 0;
        }
    }
}
