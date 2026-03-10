package io.xcutiboo.mythicrod.spigot.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Brigadier-style command adapter for Spigot.
 * Mirrors the Paper Brigadier command structure exactly, providing:
 * - Identical command tree structure
 * - Matching subcommands and permissions
 * - Equivalent tab completion behavior
 * - Same user experience as Paper
 *
 * This is NOT native Brigadier (Spigot doesn't expose it), but a compatibility layer
 * that provides the same functionality and UX.
 */
public class BrigadierStyleCommandManager implements CommandExecutor, TabCompleter {
    private final MythicRod plugin;

    public BrigadierStyleCommandManager(MythicRod plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        org.bukkit.command.PluginCommand cmd = plugin.getCommand("mythicrod");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
            cmd.setAliases(Arrays.asList("mr", "mrod"));
            cmd.setPermission("mythicrod.command");
        }
        plugin.getLogger().log(java.util.logging.Level.INFO, "[MythicRod-Spigot] Brigadier-style command system initialized with full feature parity");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Default: open GUI if player, otherwise show help
        if (args.length == 0) {
            if (sender instanceof Player player && sender.hasPermission("mythicrod.gui")) {
                plugin.getGUIManager().openMainHub(player);
                return true;
            }
            return executeHelp(sender);
        }

        String subcommand = args[0].toLowerCase();
        return switch (subcommand) {
            case "gui" -> executeGui(sender);
            case "reload" -> executeReload(sender);
            case "stats" -> executeStats(sender, args);
            case "top" -> executeTop(sender, args);
            case "drops" -> executeDrops(sender, args);
            case "help" -> executeHelp(sender);
            default -> executeHelp(sender);
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument: subcommands
            List<String> subcommands = Arrays.asList("gui", "reload", "stats", "top", "drops", "help");
            String partial = args[0].toLowerCase();
            return subcommands.stream()
                    .filter(s -> s.startsWith(partial))
                    .filter(s -> hasPermissionForSubcommand(sender, s))
                    .toList();
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("stats") && sender.hasPermission("mythicrod.stats.view")) {
                // Tab complete player names for stats command
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .toList();
            } else if (subcommand.equals("top") && sender.hasPermission("mythicrod.stats.leaderboard")) {
                // Suggest common limits for top command
                return Arrays.asList("5", "10", "25", "50", "100");
            }
        }

        return completions;
    }

    private boolean hasPermissionForSubcommand(CommandSender sender, String subcommand) {
        return switch (subcommand) {
            case "gui" -> sender.hasPermission("mythicrod.gui");
            case "reload" -> sender.hasPermission("mythicrod.admin.reload");
            case "stats" -> sender.hasPermission("mythicrod.stats.view");
            case "top" -> sender.hasPermission("mythicrod.stats.leaderboard");
            case "drops" -> sender.hasPermission("mythicrod.drops.view");
            case "help" -> sender.hasPermission("mythicrod.command");
            default -> false;
        };
    }

    /**
     * Get the formatted prefix component using the config prefix and color it properly
     */
    private Component getPrefix() {
        try {
            String configPrefix = plugin.getConfigManager().getPrefix();
            return LegacyComponentSerializer.legacyAmpersand().deserialize(configPrefix);
        } catch (Exception e) {
            // Fallback prefix if config fails
            return Component.text("[", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                    .append(Component.text("MythicRod", net.kyori.adventure.text.format.NamedTextColor.AQUA))
                    .append(Component.text("] ", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
        }
    }

    /**
     * Send a formatted message with the MythicRod prefix
     */
    private void sendMessage(CommandSender sender, String message) {
        try {
            Component prefixComponent = getPrefix();
            Component messageComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            Component fullMessage = prefixComponent.append(messageComponent);
            plugin.audiences().sender(sender).sendMessage(fullMessage);
        } catch (Exception e) {
            // Fallback to plain text
            sender.sendMessage("MythicRod: " + message);
        }
    }

    private boolean executeGui(CommandSender sender) {
        if (!sender.hasPermission("mythicrod.gui")) {
            sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "command.help.gui"));
            return true;
        }

        if (sender instanceof Player player) {
            plugin.getGUIManager().openMainHub(player);
            return true;
        }

        sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "general.player_only"));
        return true;
    }

    private boolean executeReload(CommandSender sender) {
        if (!sender.hasPermission("mythicrod.admin.reload")) {
            sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "general.no_permission"));
            return true;
        }

        sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "command.reload.start"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.reload();
                // Schedule message back to main thread (required for thread safety)
                Bukkit.getScheduler().runTask(plugin, () ->
                    sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "command.reload.success"))
                );
            } catch (Exception e) {
                // Schedule error message back to main thread (required for thread safety)
                Bukkit.getScheduler().runTask(plugin, () ->
                    sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "command.reload.failed",
                        java.util.Map.of("error", e.getMessage())))
                );
            }
        });

        return true;
    }

    private boolean executeStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mythicrod.stats.view")) {
            sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "stats.permission_denied"));
            return true;
        }

        Player target;
        if (args.length < 2) {
            // Show own stats
            if (!(sender instanceof Player)) {
                sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "stats.console_usage"));
                return true;
            }
            target = (Player) sender;
        } else {
            // Show another player's stats
            String targetName = args[1];
            target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "command.player_not_found",
                    java.util.Map.of("player", targetName)));
                return true;
            }
        }

        displayStats(sender, target);
        return true;
    }

    private void displayStats(CommandSender sender, Player target) {
        Map<String, Object> stats = plugin.getStatisticsManager().getPlayerStatistics(plugin.getPlatform().getPlayer(target.getUniqueId()));

        var audience = plugin.audiences().sender(sender);
        audience.sendMessage(Component.text("═════════════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
        audience.sendMessage(Component.text("Fishing Stats: ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(target.getName(), NamedTextColor.WHITE)));
        audience.sendMessage(Component.text("═════════════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
        audience.sendMessage(Component.text("Total Catches: ", NamedTextColor.GRAY)
                .append(Component.text(stats.getOrDefault("total_catches", 0).toString(), NamedTextColor.WHITE)));
        audience.sendMessage(Component.text("Items Caught: ", NamedTextColor.GRAY)
                .append(Component.text(stats.getOrDefault("total_items_caught", 0).toString(), NamedTextColor.WHITE)));
        audience.sendMessage(Component.text("Unique Types: ", NamedTextColor.GRAY)
                .append(Component.text(stats.getOrDefault("unique_types", 0).toString(), NamedTextColor.WHITE)));
        audience.sendMessage(Component.text("Rare Catches: ", NamedTextColor.GRAY)
                .append(Component.text(stats.getOrDefault("rare_catches", 0).toString(), NamedTextColor.GOLD)));
        audience.sendMessage(Component.text("═════════════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
    }

    private boolean executeTop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mythicrod.stats.leaderboard")) {
            sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "general.no_permission"));
            return true;
        }

        int limit = 10; // Default
        if (args.length >= 2) {
            try {
                limit = Integer.parseInt(args[1]);
                if (limit < 1 || limit > 100) {
                    sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "stats.limit_invalid"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sendMessage(sender, plugin.getLanguageManager().trForSender((sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE")), "general.error"));
                return true;
            }
        }

        Map<java.util.UUID, Integer> topFishers = plugin.getStatisticsManager().getTopFishers(limit);
        var audience = plugin.audiences().sender(sender);

        audience.sendMessage(Component.text("═══════════════════════════", net.kyori.adventure.text.format.NamedTextColor.AQUA, TextDecoration.BOLD));
        audience.sendMessage(Component.text("Top Fishers Leaderboard", net.kyori.adventure.text.format.NamedTextColor.GOLD, TextDecoration.BOLD));
        audience.sendMessage(Component.text("═══════════════════════════", net.kyori.adventure.text.format.NamedTextColor.AQUA, TextDecoration.BOLD));

        int rank = 1;
        for (Map.Entry<java.util.UUID, Integer> entry : topFishers.entrySet()) {
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
            @SuppressWarnings("null")
            String playerName = (offlinePlayer.getName() != null) ? offlinePlayer.getName() : entry.getKey().toString();
            int catches = entry.getValue();

            Component rankComp = Component.text(rank + ". ", net.kyori.adventure.text.format.NamedTextColor.YELLOW);
            Component name = Component.text(playerName, net.kyori.adventure.text.format.NamedTextColor.WHITE);
            Component count = Component.text(" - " + catches + " catches", net.kyori.adventure.text.format.NamedTextColor.GRAY);

            audience.sendMessage(rankComp.append(name).append(count));
            rank++;
        }

        audience.sendMessage(Component.text("═══════════════════════════", net.kyori.adventure.text.format.NamedTextColor.AQUA, TextDecoration.BOLD));
        return true;
    }

    private boolean executeDrops(CommandSender sender, @SuppressWarnings("unused") String[] args) {
        if (!sender.hasPermission("mythicrod.drops.view")) {
            sendMessage(sender, plugin.getLanguageManager().tr("general.no-permission"));
            return true;
        }

        if (sender instanceof Player player) {
            plugin.getGUIManager().openMenu(player, "drops");
            return true;
        }

        List<CustomDrop> drops = plugin.getDropManager().getAllDrops();
        displayDropsList(sender, drops, "All");
        return true;
    }

    private void displayDropsList(CommandSender sender, List<CustomDrop> drops, String category) {
        var audience = plugin.audiences().sender(sender);
        audience.sendMessage(Component.text("═════════════════════", net.kyori.adventure.text.format.NamedTextColor.AQUA, TextDecoration.BOLD));
        audience.sendMessage(Component.text("Available Drops: ", net.kyori.adventure.text.format.NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(category, net.kyori.adventure.text.format.NamedTextColor.WHITE)));
        audience.sendMessage(Component.text("═════════════════════", net.kyori.adventure.text.format.NamedTextColor.AQUA, TextDecoration.BOLD));

        for (CustomDrop drop : drops) {
            Component line = Component.text("• ", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .append(Component.text(drop.getIdentifier(), net.kyori.adventure.text.format.NamedTextColor.WHITE))
                    .append(Component.text(" (", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                    .append(Component.text(String.format("%.2f%%", (double)drop.getChance()), net.kyori.adventure.text.format.NamedTextColor.AQUA))
                    .append(Component.text(")", net.kyori.adventure.text.format.NamedTextColor.GRAY));
            audience.sendMessage(line);
        }

        audience.sendMessage(Component.text("═════════════════════", net.kyori.adventure.text.format.NamedTextColor.AQUA, TextDecoration.BOLD));
    }

    private boolean executeHelp(CommandSender sender) {
        var audience = plugin.audiences().sender(sender);
        audience.sendMessage(Component.text("═════════════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
        audience.sendMessage(Component.text("MythicRod Commands", NamedTextColor.GOLD, TextDecoration.BOLD));
        audience.sendMessage(Component.text("═════════════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
        audience.sendMessage(Component.text("/mythicrod", NamedTextColor.AQUA)
                .append(Component.text(" - Open main GUI", NamedTextColor.GRAY)));
        audience.sendMessage(Component.text("/mythicrod gui", NamedTextColor.AQUA)
                .append(Component.text(" - Open main GUI", NamedTextColor.GRAY)));
        audience.sendMessage(Component.text("/mythicrod reload", NamedTextColor.AQUA)
                .append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        audience.sendMessage(Component.text("/mythicrod stats [player]", NamedTextColor.AQUA)
                .append(Component.text(" - View statistics", NamedTextColor.GRAY)));
        audience.sendMessage(Component.text("/mythicrod top [limit]", NamedTextColor.AQUA)
                .append(Component.text(" - View leaderboard", NamedTextColor.GRAY)));
        audience.sendMessage(Component.text("/mythicrod drops", NamedTextColor.AQUA)
                .append(Component.text(" - View available drops", NamedTextColor.GRAY)));
        audience.sendMessage(Component.text("═════════════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
        return true;
    }
}
