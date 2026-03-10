package io.xcutiboo.mythicrod.paper.commands;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Modern Brigadier command system for Paper with canonical behavior parity.
 */
public class BrigadierCommandManager {
    private final MythicRod plugin;
    private final Component prefix;

    public BrigadierCommandManager(MythicRod plugin) {
        this.plugin = plugin;
        // Use plugin configuration prefix instead of hardcoded style
        String prefixText = plugin.getConfigManager().getPrefix();
        this.prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixText);
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
            .executes(this::executeDefault)
            .then(Commands.literal("gui")
                .requires(source -> source.getSender().hasPermission("mythicrod.gui"))
                .executes(this::executeGui))
            .then(Commands.literal("menu")
                .requires(source -> source.getSender().hasPermission("mythicrod.gui"))
                .executes(this::executeGui))
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("mythicrod.admin.reload"))
                .executes(this::executeReload))
            .then(Commands.literal("stats")
                .requires(source -> source.getSender().hasPermission("mythicrod.stats.view"))
                .executes(this::executeStatsOwnPlayer)
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                        return builder.buildFuture();
                    })
                    .executes(this::executeStatsSpecificPlayer)))
            .then(Commands.literal("top")
                .requires(source -> source.getSender().hasPermission("mythicrod.stats.leaderboard"))
                .executes(context -> executeTop(context.getSource(), 10))
                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                    .executes(context -> executeTop(context.getSource(),
                        IntegerArgumentType.getInteger(context, "limit")))))
            .then(Commands.literal("drops")
                .requires(source -> source.getSender().hasPermission("mythicrod.drops.view"))
                .executes(this::executeDrops)
                .then(Commands.argument("category", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        plugin.getDropManager().getDropCategories().keySet().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(this::executeDropsCategory)))
            .then(Commands.literal("help")
                .executes(this::executeHelp))
            .build();
    }

    private int executeDefault(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().getSender() instanceof Player player) {
                if (player.hasPermission("mythicrod.gui")) {
                    plugin.getGUIManager().openMainHub(player);
                    return Command.SINGLE_SUCCESS;
                }
            }
            return executeHelp(context);
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().trForSender(context.getSource().getSender(), "general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing default command", e);
            return 0;
        }
    }

    private int executeGui(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().getSender() instanceof Player player) {
                plugin.getGUIManager().openMainHub(player);
                return Command.SINGLE_SUCCESS;
            }
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().trForSender(context.getSource().getSender(), "general.player_only"));
            return 0;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().trForSender(context.getSource().getSender(), "general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing gui command", e);
            return 0;
        }
    }

    private int executeReload(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            plugin.reload();
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().trForSender(context.getSource().getSender(), "command.reload.success"));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().trForSender(context.getSource().getSender(), "general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing reload command", e);
            return 0;
        }
    }

    private int executeStatsOwnPlayer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getSender() instanceof Player player)) {
                sendMessage(context.getSource().getSender(), plugin.getLanguageManager().trForSender(context.getSource().getSender(), "stats.console_usage"));
                return 0;
            }
            handleStats(context.getSource().getSender(), new String[0]);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().trForSender(context.getSource().getSender(), "general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing stats command", e);
            return 0;
        }
    }

    private int executeStatsSpecificPlayer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            String playerName = StringArgumentType.getString(context, "player");
            handleStats(context.getSource().getSender(), new String[]{"stats", playerName});
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().trForSender(context.getSource().getSender(), "general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing stats command", e);
            return 0;
        }
    }

    private void handleStats(org.bukkit.command.CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().trackStatistics()) {
            sendMessage(sender, plugin.getLanguageManager().tr("stats.disabled"));
            return;
        }
        OfflinePlayer target;
        if (args.length > 1) {
            String playerName = args[1];
            target = Bukkit.getOfflinePlayerIfCached(playerName);
            if (target == null) {
                sendMessage(sender, plugin.getLanguageManager().tr("stats.player-not-found",
                    Map.of("player", playerName)));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sendMessage(sender, plugin.getLanguageManager().tr("stats.console-usage"));
                return;
            }
            target = (Player) sender;
        }
        StatisticsManager.PlayerStats stats = plugin.getStatisticsManager().getPlayerStats(target);
        sendMessage(sender, plugin.getLanguageManager().tr("stats.header",
            Map.of("player", target.getName() != null ? target.getName() : "Unknown")));
        sendMessage(sender, plugin.getLanguageManager().tr("stats.total-catches",
            Map.of("total", String.valueOf(stats.getTotalCatches()))));
        sendMessage(sender, plugin.getLanguageManager().tr("stats.rare-catches",
            Map.of("rare", String.valueOf(stats.getRareCatches()))));
        Map<String, Integer> materialCounts = stats.getMaterialCounts();
        if (!materialCounts.isEmpty()) {
            sendMessage(sender, plugin.getLanguageManager().tr("stats.top-materials"));
            materialCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> sendMessage(sender, plugin.getLanguageManager().tr("stats.material-count",
                        Map.of("material", entry.getKey(), "count", String.valueOf(entry.getValue())))));
        }
    }

    private int executeTop(CommandSourceStack source, int limit) {
        try {
            if (!plugin.getConfigManager().trackStatistics()) {
                sendMessage(source.getSender(), plugin.getLanguageManager().tr("stats.disabled"));
                return 0;
            }

            limit = Math.clamp(limit, 1, 50);
            Map<UUID, Integer> topFishers = plugin.getStatisticsManager().getTopFishers(limit);

            if (topFishers.isEmpty()) {
                sendMessage(source.getSender(), plugin.getLanguageManager().tr("stats.no-stats"));
                return 0;
            }

            sendMessage(source.getSender(), plugin.getLanguageManager().tr("stats.top-header",
                Map.of("limit", String.valueOf(limit))));
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : topFishers.entrySet()) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                String name = player.getName() != null ? player.getName() : "Unknown";
                sendMessage(source.getSender(), plugin.getLanguageManager().tr("stats.top-entry",
                    Map.of("rank", String.valueOf(rank), "player", name, "catches", String.valueOf(entry.getValue()))));
                rank++;
            }
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(source.getSender(), plugin.getLanguageManager().tr("general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing top command", e);
            return 0;
        }
    }

    private int executeDrops(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().getSender() instanceof Player player) {
                plugin.getGUIManager().openMenu(player, "drops");
                return Command.SINGLE_SUCCESS;
            }
            handleDrops(context.getSource().getSender(), new String[0]);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().tr("general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing drops command", e);
            return 0;
        }
    }

    private int executeDropsCategory(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            String category = StringArgumentType.getString(context, "category");
            handleDrops(context.getSource().getSender(), new String[]{"drops", category});
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().tr("general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing drops command", e);
            return 0;
        }
    }

    private void handleDrops(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length > 1) {
            String category = args[1].toLowerCase();
            if (plugin.getDropManager().getDropCategories().containsKey(category)) {
                displayDropCategory(sender, category);
            } else {
                sendMessage(sender, plugin.getLanguageManager().tr("drops.category-not-found",
                    Map.of("category", category)));
                sendMessage(sender, plugin.getLanguageManager().tr("drops.available-categories",
                    Map.of("categories", String.join(", ", plugin.getDropManager().getDropCategories().keySet()))));
            }
        } else {
            Map<String, List<CustomDrop>> categories = plugin.getDropManager().getDropCategories();
            sendMessage(sender, plugin.getLanguageManager().tr("drops.header"));
            for (String category : categories.keySet()) {
                sendMessage(sender, plugin.getLanguageManager().tr("drops.category-entry",
                    Map.of("category", category, "count", String.valueOf(categories.get(category).size()))));
            }
            sendMessage(sender, plugin.getLanguageManager().tr("drops.usage"));
        }
    }

    private void displayDropCategory(org.bukkit.command.CommandSender sender, String category) {
        List<CustomDrop> drops = plugin.getDropManager().getDropCategories().get(category);
        if (drops == null || drops.isEmpty()) {
            sendMessage(sender, plugin.getLanguageManager().tr("drops.category-not-found",
                Map.of("category", category)));
            return;
        }
        sendMessage(sender, plugin.getLanguageManager().tr("drops.category-header",
            Map.of("category", category)));
        for (CustomDrop drop : drops) {
            String name = drop.getCustomName() != null ? drop.getCustomName() : drop.getMaterial().name();
            sendMessage(sender, plugin.getLanguageManager().tr("drops.drop-entry",
                Map.of("name", name, "chance", String.valueOf(drop.getChance()), "amount", String.valueOf(drop.getAmount()))));
        }
    }

    private int executeHelp(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            sendHelpMessage(context.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), plugin.getLanguageManager().tr("general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing help command", e);
            return 0;
        }
    }

    private void sendHelpMessage(org.bukkit.command.CommandSender sender) {
        sendMessage(sender, plugin.getLanguageManager().tr("commands.help-header"));
        if (sender instanceof Player) {
            sendMessage(sender, plugin.getLanguageManager().tr("commands.help-gui"));
        }
        sendMessage(sender, plugin.getLanguageManager().tr("commands.help-reload"));
        sendMessage(sender, plugin.getLanguageManager().tr("commands.help-stats"));
        sendMessage(sender, plugin.getLanguageManager().tr("commands.help-top"));
        sendMessage(sender, plugin.getLanguageManager().tr("commands.help-drops"));
        sendMessage(sender, plugin.getLanguageManager().tr("commands.help-help"));
    }

    private void sendMessage(org.bukkit.command.CommandSender sender, String message) {
        try {
            // Parse prefix (may contain color codes)
            Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getConfigManager().getPrefix());
            // Parse message (may contain color codes)
            Component messageComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            // Combine prefix and message
            Component fullMessage = prefixComponent.append(messageComponent);
            // Send properly formatted message
            sender.sendMessage(fullMessage);
        } catch (Exception e) {
            // Fallback: send as plain text
            plugin.getLogger().warning("Failed to parse message components: " + e.getMessage());
            sender.sendMessage(message);
        }
    }
}
