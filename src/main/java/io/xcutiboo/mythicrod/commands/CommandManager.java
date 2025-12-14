package io.xcutiboo.mythicrod.commands;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
public class CommandManager implements CommandExecutor, TabCompleter {
    private final MythicRod plugin;
    private final String prefix;
    public CommandManager(MythicRod plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getConfigManager().getPrefix();
    }
    public void initialize() {
        try {
            var command = new org.bukkit.command.defaults.BukkitCommand(
                "mythicrod",
                "Access MythicRod features and statistics",
                "/mythicrod [reload|stats|top|drops|help]",
                Arrays.asList("mr", "mrod")
            ) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    return onCommand(sender, this, commandLabel, args);
                }
                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                    return onTabComplete(sender, this, alias, args);
                }
            };
            plugin.getServer().getCommandMap().register("mythicrod", command);
            plugin.getLogger().info("Successfully registered command: /mythicrod");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register command 'mythicrod'", e);
        }
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player && hasPermission(sender, "mythicrod.gui")) {
                handleGUI((Player) sender);
                return true;
            }
            sendHelpMessage(sender);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        try {
            switch (subCommand) {
                case "reload" -> {
                    if (!hasPermission(sender, "mythicrod.admin.reload")) {
                        return true;
                    }
                    handleReload(sender);
                }
                case "gui", "menu" -> {
                    if (!(sender instanceof Player)) {
                        sendMessage(sender, plugin.getLanguageManager().tr("gui.players-only"));
                        return true;
                    }
                    if (!hasPermission(sender, "mythicrod.gui")) {
                        return true;
                    }
                    handleGUI((Player) sender);
                }
                case "stats" -> {
                    if (!hasPermission(sender, "mythicrod.stats.view")) {
                        return true;
                    }
                    handleStats(sender, args);
                }
                case "top" -> {
                    if (!hasPermission(sender, "mythicrod.stats.leaderboard")) {
                        return true;
                    }
                    handleTop(sender, args);
                }
                case "drops" -> {
                    if (!hasPermission(sender, "mythicrod.drops.view")) {
                        return true;
                    }
                    handleDrops(sender, args);
                }
                case "help" -> sendHelpMessage(sender);
                default -> sendHelpMessage(sender);
            }
        } catch (Exception e) {
            sendMessage(sender, plugin.getLanguageManager().tr("general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing command '" + subCommand + "'", e);
        }
        return true;
    }
    private void handleReload(CommandSender sender) {
        plugin.reload();
        sendMessage(sender, plugin.getLanguageManager().tr("commands.reload.success"));
    }
    private void handleGUI(Player player) {
        plugin.getGUIManager().openMainHub(player);
    }
    private void handleStats(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().trackStatistics()) {
            sendMessage(sender, plugin.getLanguageManager().tr("stats.disabled"));
            return;
        }
        OfflinePlayer target;
        if (args.length > 1) {
            String playerName = args[1];
            target = Bukkit.getOfflinePlayerIfCached(playerName);
            if (target == null) {
                // Try by name if not cached
                if (!target.hasPlayedBefore()) {
                    sendMessage(sender, plugin.getLanguageManager().tr("stats.player-not-found",
                        Map.of("player", playerName)));
                    return;
                }
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
    private void handleTop(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().trackStatistics()) {
            sendMessage(sender, plugin.getLanguageManager().tr("stats.disabled"));
            return;
        }
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.min(Math.max(limit, 1), 50);
            } catch (NumberFormatException e) {
                sendMessage(sender, plugin.getLanguageManager().tr("stats.invalid-number",
                    Map.of("number", args[1])));
                return;
            }
        }
        Map<UUID, Integer> topFishers = plugin.getStatisticsManager().getTopFishers(limit);
        if (topFishers.isEmpty()) {
            sendMessage(sender, plugin.getLanguageManager().tr("stats.no-stats"));
            return;
        }
        sendMessage(sender, plugin.getLanguageManager().tr("stats.top-header",
            Map.of("limit", String.valueOf(limit))));
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : topFishers.entrySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String name = player.getName() != null ? player.getName() : "Unknown";
            sendMessage(sender, plugin.getLanguageManager().tr("stats.top-entry",
                Map.of("rank", String.valueOf(rank), "player", name, "catches", String.valueOf(entry.getValue()))));
            rank++;
        }
    }
    private void handleDrops(CommandSender sender, String[] args) {
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
    private void displayDropCategory(CommandSender sender, String category) {
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
    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sendMessage(sender, plugin.getLanguageManager().tr("general.no-permission"));
        return false;
    }
    private void sendHelpMessage(CommandSender sender) {
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
    private void sendMessage(CommandSender sender, String message) {
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
        sender.sendMessage(component);
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("reload", "stats", "top", "drops", "help"));
            if (sender instanceof Player) {
                subCommands.add("gui");
                subCommands.add("menu");
            }
            return filterCompletions(subCommands, args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "stats" -> null; // Returns online players
                case "drops" -> filterCompletions(
                        new ArrayList<>(plugin.getDropManager().getDropCategories().keySet()),
                        args[1]);
                case "top" -> Arrays.asList("5", "10", "25", "50");
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }
    private List<String> filterCompletions(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerPrefix))
                .sorted()
                .collect(Collectors.toList());
    }
}
