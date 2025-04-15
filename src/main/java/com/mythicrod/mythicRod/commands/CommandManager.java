package com.mythicrod.mythicrod.commands;

import com.mythicrod.mythicrod.MythicRod;
import com.mythicrod.mythicrod.drops.CustomDrop;
import com.mythicrod.mythicrod.metrics.StatisticsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final MythicRod plugin;
    private final String prefix;

    public CommandManager(MythicRod plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getConfigManager().getPrefix();

        plugin.getCommand("mythicrod").setExecutor(this);
        plugin.getCommand("mythicrod").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!hasPermission(sender, "mythicrod.admin.reload")) {
                    return true;
                }
                handleReload(sender);
                break;

            case "stats":
                if (!hasPermission(sender, "mythicrod.stats")) {
                    return true;
                }
                handleStats(sender, args);
                break;

            case "top":
                if (!hasPermission(sender, "mythicrod.stats.top")) {
                    return true;
                }
                handleTop(sender, args);
                break;

            case "drops":
                if (!hasPermission(sender, "mythicrod.drops")) {
                    return true;
                }
                handleDrops(sender, args);
                break;

            case "help":
                sendHelpMessage(sender);
                break;

            default:
                sendMessage(sender, "&cUnknown command. Use &6/mythicrod help &cfor help.");
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reload();
        sendMessage(sender, "&aConfiguration reloaded successfully!");
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().trackStatistics()) {
            sendMessage(sender, "&cStatistics tracking is disabled in the configuration.");
            return;
        }

        OfflinePlayer target;

        if (args.length > 1) {
            String playerName = args[1];
            target = Bukkit.getOfflinePlayer(playerName);

            if (target == null || !target.hasPlayedBefore()) {
                sendMessage(sender, "&cPlayer &e" + playerName + "&c not found or has never played before.");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "&cYou must specify a player name when using this command from console.");
                return;
            }

            target = (Player) sender;
        }

        StatisticsManager.PlayerStats stats = plugin.getStatisticsManager().getPlayerStats(target);

        sendMessage(sender, "&6----- &e" + target.getName() + "'s Fishing Stats &6-----");
        sendMessage(sender, "&eTotal Catches: &f" + stats.getTotalCatches());
        sendMessage(sender, "&eRare Catches: &f" + stats.getRareCatches());

        Map<String, Integer> materialCounts = stats.getMaterialCounts();
        if (!materialCounts.isEmpty()) {
            sendMessage(sender, "&6----- &eTop Materials Caught &6-----");

            materialCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> sendMessage(sender, "&e" + entry.getKey() + ": &f" + entry.getValue()));
        }
    }

    private void handleTop(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().trackStatistics()) {
            sendMessage(sender, "&cStatistics tracking is disabled in the configuration.");
            return;
        }

        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.min(Math.max(limit, 1), 50);
            } catch (NumberFormatException e) {
                sendMessage(sender, "&cInvalid number: " + args[1]);
                return;
            }
        }

        Map<UUID, Integer> topFishers = plugin.getStatisticsManager().getTopFishers(limit);

        if (topFishers.isEmpty()) {
            sendMessage(sender, "&cNo fishing statistics available yet.");
            return;
        }

        sendMessage(sender, "&6----- &eTop " + limit + " Fishers &6-----");

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : topFishers.entrySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String name = player.getName() != null ? player.getName() : "Unknown";

            sendMessage(sender, "&e#" + rank + ": &f" + name + " &7- &f" + entry.getValue() + " catches");
            rank++;
        }
    }

    private void handleDrops(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String category = args[1].toLowerCase();

            if (plugin.getDropManager().getDropCategories().containsKey(category)) {
                displayDropCategory(sender, category);
            } else {
                sendMessage(sender, "&cDrop category &e" + category + "&c not found.");
                sendMessage(sender, "&6Available categories: &e"
                        + String.join(", ", plugin.getDropManager().getDropCategories().keySet()));
            }
        } else {
            Map<String, List<CustomDrop>> categories = plugin.getDropManager().getDropCategories();

            sendMessage(sender, "&6----- &eMythicRod Drop Categories &6-----");
            for (String category : categories.keySet()) {
                sendMessage(sender, "&e" + category + ": &f" + categories.get(category).size() + " drops");
            }

            sendMessage(sender, "&7Use &f/mythicrod drops <category>&7 to view drops in a category.");
        }
    }

    private void displayDropCategory(CommandSender sender, String category) {
        List<CustomDrop> drops = plugin.getDropManager().getDropCategories().get(category);

        if (drops == null || drops.isEmpty()) {
            sendMessage(sender, "&cNo drops found in category &e" + category);
            return;
        }

        sendMessage(sender, "&6----- &eMythicRod Drops: " + category + " &6-----");

        for (CustomDrop drop : drops) {
            String name = drop.getCustomName() != null ? drop.getCustomName() : drop.getMaterial().name();
            sendMessage(sender, "&e" + name + " &7- &fChance: " + drop.getChance()
                    + ", Amount: " + drop.getAmount());
        }
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }

        sendMessage(sender, "&cYou don't have permission to use this command.");
        return false;
    }

    private void sendHelpMessage(CommandSender sender) {
        sendMessage(sender, "&6----- &eMythicRod Commands &6-----");
        sendMessage(sender, "&e/mythicrod reload &7- Reload the plugin configuration");
        sendMessage(sender, "&e/mythicrod stats [player] &7- View fishing stats");
        sendMessage(sender, "&e/mythicrod top [limit] &7- View top fishers");
        sendMessage(sender, "&e/mythicrod drops [category] &7- View available drops");
        sendMessage(sender, "&e/mythicrod help &7- Show this help message");
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "stats", "top", "drops", "help");
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "stats":
                    return null;

                case "drops":
                    return filterCompletions(new ArrayList<>(
                            plugin.getDropManager().getDropCategories().keySet()), args[1]);

                case "top":
                    return Arrays.asList("5", "10", "25", "50");
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
