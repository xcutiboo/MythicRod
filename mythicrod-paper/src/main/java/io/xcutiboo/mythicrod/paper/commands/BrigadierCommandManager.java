package io.xcutiboo.mythicrod.paper.commands;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import io.xcutiboo.mythicrod.stats.PlayerStats;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.cache.MythicRodCache;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import io.xcutiboo.mythicrod.item.RodFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;

public class BrigadierCommandManager {
    private final MythicRod plugin;
    private final RodFactory rodFactory;

    public BrigadierCommandManager(MythicRod plugin) {
        this.plugin = plugin;
        this.rodFactory = new RodFactory(plugin);
    }

    private String tr(String key) {
        return plugin.getLanguageManager().tr(key);
    }

    private String tr(String key, Map<String, String> args) {
        return plugin.getLanguageManager().tr(key, args);
    }

    /**
     * Sends formatted success message with sound (direct message version)
     */
    private void sendSuccess(CommandSourceStack source, String message) {
        if (source.getSender() instanceof Player player) {
            Component msg = MiniMessage.miniMessage().deserialize(
                "<green>✓ " + message + "</green>"
            );
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            source.getSender().sendMessage(Component.text("✓ " + message).color(NamedTextColor.GREEN));
        }
    }

    /**
     * Sends formatted error message with sound (direct message version)
     */
    private void sendError(CommandSourceStack source, String message) {
        if (source.getSender() instanceof Player player) {
            Component msg = MiniMessage.miniMessage().deserialize(
                "<red>✗ " + message + "</red>"
            );
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        } else {
            source.getSender().sendMessage(Component.text("✗ " + message).color(NamedTextColor.RED));
        }
    }

    /**
     * Sends formatted info message (direct message version)
     */
    private void sendInfo(CommandSourceStack source, String message) {
        if (source.getSender() instanceof Player player) {
            Component msg = MiniMessage.miniMessage().deserialize(
                "<yellow>ℹ " + message + "</yellow>"
            );
            player.sendMessage(msg);
        } else {
            source.getSender().sendMessage(Component.text("ℹ " + message).color(NamedTextColor.YELLOW));
        }
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
                // BUG-005 FIX: argument was ArgumentTypes.player() but read with
                // StringArgumentType.getString() — type mismatch → CommandSyntaxException.
                // Changed to StringArgumentType.word() so getString() works correctly.
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
            .then(Commands.literal("give")
                .requires(source -> source.getSender().hasPermission("mythicrod.admin.give"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("tier", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("basic");
                            builder.suggest("advanced");
                            builder.suggest("legendary");
                            return builder.buildFuture();
                        })
                        .executes(this::executeGive))))
            .then(Commands.literal("drops")
                .requires(source -> source.getSender().hasPermission("mythicrod.drops.view"))
                .executes(this::executeDrops)
                .then(Commands.argument("category", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        plugin.getDropManager().getDropCategories().keySet().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(this::executeDropsCategory)))
            .then(Commands.literal("debug")
                .requires(source -> source.getSender().hasPermission("mythicrod.admin.debug"))
                .executes(this::executeDebug))
            .then(Commands.literal("saveitem")
                .requires(source -> source.getSender().hasPermission("mythicrod.admin.config"))
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(this::executeSaveItem)))
            .then(Commands.literal("particle")
                .requires(source -> source.getSender().hasPermission("mythicrod.admin.config"))
                .executes(this::executeParticleInfo)
                .then(Commands.literal("catch")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("SPLASH");
                            builder.suggest("BUBBLE_POP");
                            builder.suggest("HAPPY_VILLAGER");
                            builder.suggest("TOTEM");
                            builder.suggest("HEART");
                            builder.suggest("NOTE");
                            builder.suggest("FLAME");
                            return builder.buildFuture();
                        })
                        .executes(this::executeParticleCatch)))
                .then(Commands.literal("bubble")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("BUBBLE_POP");
                            builder.suggest("SPLASH");
                            builder.suggest("HAPPY_VILLAGER");
                            builder.suggest("NOTE");
                            builder.suggest("FLAME");
                            return builder.buildFuture();
                        })
                        .executes(this::executeParticleBubble)))
                .then(Commands.literal("success")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("HAPPY_VILLAGER");
                            builder.suggest("TOTEM");
                            builder.suggest("HEART");
                            builder.suggest("NOTE");
                            builder.suggest("FLAME");
                            builder.suggest("END_ROD");
                            return builder.buildFuture();
                        })
                        .executes(this::executeParticleSuccess))))
            .then(Commands.literal("help")
                .executes(this::executeHelp))
            .build();
    }

    private int executeDefault(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().getSender() instanceof Player player) {
                if (player.hasPermission("mythicrod.gui")) {
                    plugin.getGUIManager().openMainHub(player);
                    return Command.SINGLE_SUCCESS;
                }
            }
            return executeHelp(context);
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), 
                Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error executing default command", e);
            return 0;
        }
    }

    private int executeGui(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().getSender() instanceof Player player) {
                plugin.getGUIManager().openMainHub(player);
                sendSuccess(context.getSource(), "GUI opened!");
                return Command.SINGLE_SUCCESS;
            }
            sendError(context.getSource(), "This command can only be used by players.");
            return 0;
        } catch (Exception e) {
            sendError(context.getSource(), "Error: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error executing gui command", e);
            return 0;
        }
    }

    private int executeGive(CommandContext<CommandSourceStack> context) {
        try {
            CommandSender sender = context.getSource().getSender();
            String playerName = StringArgumentType.getString(context, "player");
            String tier = StringArgumentType.getString(context, "tier");
            if (tier == null) {
                sendMessage(sender,
                    Component.text("Tier cannot be null", NamedTextColor.RED));
                playErrorSound(sender);
                return 0;
            }
            
            Player target = Bukkit.getPlayer(playerName);
            if (target == null || !target.isOnline()) {
                sendMessage(sender, 
                    Component.text("Player not found: " + playerName, NamedTextColor.RED));
                playErrorSound(sender);
                return 0;
            }
            
            ItemStack rod;
            switch (tier.toLowerCase(java.util.Locale.ROOT)) {
                case "basic" -> rod = rodFactory.createBasicRod();
                case "advanced" -> rod = rodFactory.createAdvancedRod();
                case "legendary" -> rod = rodFactory.createLegendaryRod();
                default -> {
                    sendMessage(sender,
                        Component.text("Invalid tier: " + tier + ". Use: basic, advanced, legendary", NamedTextColor.RED));
                    playErrorSound(sender);
                    return 0;
                }
            }
            
            target.getInventory().addItem(rod);
            
            sendMessage(sender,
                Component.text("Gave ", NamedTextColor.GREEN)
                    .append(Component.text(tier, NamedTextColor.AQUA))
                    .append(Component.text(" MythicRod to ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.YELLOW)));
            
            target.sendMessage(Component.text("You received a ", NamedTextColor.GREEN)
                .append(Component.text(tier, NamedTextColor.AQUA))
                .append(Component.text(" MythicRod!", NamedTextColor.GREEN)));
            
            playSuccessSound(sender);
            playSuccessSound(target);
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), 
                Component.text("Error giving rod: " + e.getMessage(), NamedTextColor.RED));
            playErrorSound(context.getSource().getSender());
            plugin.getLogger().log(Level.SEVERE, "Error executing give command", e);
            return 0;
        }
    }
    
    private int executeReload(CommandContext<CommandSourceStack> context) {
        try {
            MythicRodCache cache = plugin.getCache();
            if (cache != null) {
                cache.invalidateAll();
            }
            plugin.reload();
            sendSuccess(context.getSource(), "Configuration reloaded and caches cleared!");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendError(context.getSource(), "Error reloading: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error executing reload command", e);
            return 0;
        }
    }

    private int executeStatsOwnPlayer(CommandContext<CommandSourceStack> context) {
        try {
            CommandSender sender = context.getSource().getSender();
            if (!(sender instanceof Player)) {
                sendMessage(sender, Component.text("Console must specify a player: /mythicrod stats <player>", NamedTextColor.RED));
                return 0;
            }
            handleStats(sender, new String[0]);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), 
                Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error executing stats command", e);
            return 0;
        }
    }

    private int executeStatsSpecificPlayer(CommandContext<CommandSourceStack> context) {
        try {
            String playerName = StringArgumentType.getString(context, "player");
            handleStats(context.getSource().getSender(), new String[]{"stats", playerName});
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), 
                Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error executing stats command", e);
            return 0;
        }
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().trackStatistics()) {
            sendMessage(sender, tr("stats.disabled"));
            return;
        }
        OfflinePlayer target;
        if (args.length > 1) {
            String playerName = args[1];
            target = Bukkit.getOfflinePlayerIfCached(playerName);
            if (target == null) {
                sendMessage(sender, tr("stats.player-not-found",
                    Map.of("player", playerName)));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sendMessage(sender, tr("stats.console-usage"));
                return;
            }
            target = (Player) sender;
        }
        PlayerStats stats = plugin.getStatisticsManager().getOrCreate(target.getUniqueId());
        sendMessage(sender, tr("stats.header",
            Map.of("player", target.getName() != null ? target.getName() : "Unknown")));
        sendMessage(sender, tr("stats.total-catches",
            Map.of("total", String.valueOf(stats.getTotalCaught()))));
        sendMessage(sender, tr("stats.rare-catches",
            Map.of("rare", String.valueOf(stats.getRareCaught()))));
        Map<String, Integer> materialCounts = stats.getMaterialCounts();
        if (!materialCounts.isEmpty()) {
            sendMessage(sender, tr("stats.top-materials"));
            materialCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> sendMessage(sender, tr("stats.material-count",
                        Map.of("material", entry.getKey(), "count", String.valueOf(entry.getValue())))));
        }
    }

    private int executeTop(CommandSourceStack source, int limit) {
        try {
            if (!plugin.getConfigManager().trackStatistics()) {
                sendMessage(source.getSender(), tr("stats.disabled"));
                return 0;
            }

            limit = Math.clamp(limit, 1, 50);
            List<PlayerStats> topFishers = plugin.getStatisticsManager().getTopFishers(limit);

            if (topFishers.isEmpty()) {
                sendMessage(source.getSender(), tr("stats.no-stats"));
                return 0;
            }

            sendMessage(source.getSender(), tr("stats.top-header",
                Map.of("limit", String.valueOf(limit))));
            int rank = 1;
            for (PlayerStats ps : topFishers) {
                // Prefer cached player name; fall back to OfflinePlayer lookup
                String name = ps.getPlayerName();
                if (name == null || name.isEmpty()) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ps.getPlayerUuid());
                    name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
                }
                sendMessage(source.getSender(), tr("stats.top-entry",
                    Map.of("rank", String.valueOf(rank), "player", name, "catches", String.valueOf(ps.getTotalCaught()))));
                rank++;
            }
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(source.getSender(), tr("general.error"));
            plugin.getLogger().log(Level.SEVERE, "Error executing top command", e);
            return 0;
        }
    }

    private int executeDrops(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().getSender() instanceof Player player) {
                plugin.getGUIManager().openMenu(player, "drops");
                return Command.SINGLE_SUCCESS;
            }
            handleDrops(context.getSource().getSender(), new String[0]);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), 
                Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error executing drops command", e);
            return 0;
        }
    }

    private int executeDropsCategory(CommandContext<CommandSourceStack> context) {
        try {
            String category = StringArgumentType.getString(context, "category");
            handleDrops(context.getSource().getSender(), new String[]{"drops", category});
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), 
                Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error executing drops command", e);
            return 0;
        }
    }

    private void handleDrops(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String category = args[1].toLowerCase(java.util.Locale.ROOT);
            if (plugin.getDropManager().getDropCategories().containsKey(category)) {
                displayDropCategory(sender, category);
            } else {
                sendMessage(sender, tr("drops.category-not-found",
                    Map.of("category", category)));
                sendMessage(sender, tr("drops.available-categories",
                    Map.of("categories", String.join(", ", plugin.getDropManager().getDropCategories().keySet()))));
            }
        } else {
            Map<String, List<CustomDrop>> categories = plugin.getDropManager().getDropCategories();
            sendMessage(sender, tr("drops.header"));
            for (String category : categories.keySet()) {
                sendMessage(sender, tr("drops.category-entry",
                    Map.of("category", category, "count", String.valueOf(categories.get(category).size()))));
            }
            sendMessage(sender, tr("drops.usage"));
        }
    }

    private void displayDropCategory(CommandSender sender, String category) {
        List<CustomDrop> drops = plugin.getDropManager().getDropCategories().get(category);
        if (drops == null || drops.isEmpty()) {
            sendMessage(sender, tr("drops.category-not-found",
                Map.of("category", category)));
            return;
        }
        sendMessage(sender, tr("drops.category-header",
            Map.of("category", category)));
        for (CustomDrop drop : drops) {
            String name = drop.getCustomName() != null ? drop.getCustomName() : drop.getIdentifier();
            sendMessage(sender, tr("drops.drop-entry",
                Map.of("name", name, "chance", String.valueOf(drop.getChance()), "amount", String.valueOf(drop.getAmount()))));
        }
    }

    private int executeHelp(CommandContext<CommandSourceStack> context) {
        try {
            sendHelpMessage(context.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), 
                Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error executing help command", e);
            return 0;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sendMessage(sender, tr("commands.help-header"));
        if (sender instanceof Player player) {
            if (player.hasPermission("mythicrod.gui")) {
                sendMessage(sender, tr("commands.help-gui"));
            }
            if (player.hasPermission("mythicrod.stats.view")) {
                sendMessage(sender, tr("commands.help-stats"));
            }
            if (player.hasPermission("mythicrod.stats.leaderboard")) {
                sendMessage(sender, tr("commands.help-top"));
            }
            if (player.hasPermission("mythicrod.drops.view")) {
                sendMessage(sender, tr("commands.help-drops"));
            }
            if (player.hasPermission("mythicrod.admin.reload")) {
                sendMessage(sender, tr("commands.help-reload"));
            }
            if (player.hasPermission("mythicrod.admin.give")) {
                sendMessage(sender, tr("commands.help-give"));
            }
            if (player.hasPermission("mythicrod.admin.debug")) {
                sendMessage(sender, tr("commands.help-debug"));
            }
        } else {
            sendMessage(sender, tr("commands.help-reload"));
            sendMessage(sender, tr("commands.help-stats"));
            sendMessage(sender, tr("commands.help-top"));
            sendMessage(sender, tr("commands.help-drops"));
            sendMessage(sender, tr("commands.help-debug"));
        }
        sendMessage(sender, tr("commands.help-help"));
    }

    private void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(message);
    }
    
    private void sendMessage(CommandSender sender, String message) {
        try {
            // Pure MiniMessage - no legacy serializers
            Component prefixComponent = MiniMessage.miniMessage().deserialize(
                plugin.getConfigManager().getPrefix());
            Component messageComponent = MiniMessage.miniMessage().deserialize(message);
            Component fullMessage = prefixComponent.append(messageComponent);
            sender.sendMessage(fullMessage);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse MiniMessage: " + e.getMessage());
            sender.sendMessage(message);
        }
    }

    /**
     * Plays a success sound for the command sender if they are a player.
     */
    private void playSuccessSound(CommandSender sender) {
        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 1.2f);
        }
    }

    /**
     * Plays an error sound for the command sender if they are a player.
     */
    private void playErrorSound(CommandSender sender) {
        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 0.5f, 1.0f);
        }
    }

    private int executeParticleInfo(CommandContext<CommandSourceStack> context) {
        try {
            CommandSender sender = context.getSource().getSender();
            sendMessage(sender, "<gold><bold>=== Particle Settings ===</bold></gold>");
            sendMessage(sender, "<gray>Current particles:</gray>");
            sendMessage(sender, "  <yellow>Catch:</yellow> <white>" + plugin.getConfigManager().getCatchParticle() + "</white>");
            sendMessage(sender, "  <yellow>Bubble:</yellow> <white>" + plugin.getConfigManager().getBubbleParticle() + "</white>");
            sendMessage(sender, "  <yellow>Success:</yellow> <white>" + plugin.getConfigManager().getSuccessParticle() + "</white>");
            sendMessage(sender, "");
            sendMessage(sender, "<yellow>Usage:</yellow>");
            sendMessage(sender, "<gray>/mythicrod particle catch <type></gray>");
            sendMessage(sender, "<gray>/mythicrod particle bubble <type></gray>");
            sendMessage(sender, "<gray>/mythicrod particle success <type></gray>");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            return 0;
        }
    }

    private int executeParticleCatch(CommandContext<CommandSourceStack> context) {
        try {
            String type = StringArgumentType.getString(context, "type");
            String particleType = type.toUpperCase(java.util.Locale.ROOT);
            plugin.getConfigManager().setCatchParticle(particleType);
            sendMessage(context.getSource().getSender(), "<green>✓ Catch particle set to: <yellow>" + particleType + "</yellow>");
            playSuccessSound(context.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            return 0;
        }
    }

    private int executeParticleBubble(CommandContext<CommandSourceStack> context) {
        try {
            String type = StringArgumentType.getString(context, "type");
            String particleType = type.toUpperCase(java.util.Locale.ROOT);
            plugin.getConfigManager().setBubbleParticle(particleType);
            sendMessage(context.getSource().getSender(), "<green>✓ Bubble particle set to: <yellow>" + particleType + "</yellow>");
            playSuccessSound(context.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            return 0;
        }
    }

    private int executeParticleSuccess(CommandContext<CommandSourceStack> context) {
        try {
            String type = StringArgumentType.getString(context, "type");
            String particleType = type.toUpperCase(java.util.Locale.ROOT);
            plugin.getConfigManager().setSuccessParticle(particleType);
            sendMessage(context.getSource().getSender(), "<green>✓ Success particle set to: <yellow>" + particleType + "</yellow>");
            playSuccessSound(context.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
            return 0;
        }
    }

    private int executeSaveItem(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getSender() instanceof Player player)) {
                sendError(context.getSource(), "This command can only be used by players.");
                return 0;
            }
            
            String id = StringArgumentType.getString(context, "id");
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            
            if (heldItem.getType().isAir()) {
                sendError(context.getSource(), "You must hold an item in your hand to save it!");
                return 0;
            }
            
            // Serialize to Base64
            byte[] bytes = heldItem.serializeAsBytes();
            String base64String = java.util.Base64.getEncoder().encodeToString(bytes);
            
            // Save to config
            plugin.getDropManager().saveBase64Drop(id, base64String, player.getName());
            
            sendSuccess(context.getSource(), "Item saved as drop ID: " + id);
            sendInfo(context.getSource(), "Material: " + heldItem.getType() + " | Amount: " + heldItem.getAmount());
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendError(context.getSource(), "Error saving item: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error executing saveitem command", e);
            return 0;
        }
    }

    private int executeDebug(CommandContext<CommandSourceStack> context) {
        try {
            CommandSender sender = context.getSource().getSender();
            
            sender.sendMessage(Component.text("=== MythicRod Debug Info ===", NamedTextColor.GOLD));
            
            MythicRodCache cache = plugin.getCache();
            if (cache != null) {
                MythicRodCache.CacheStats stats = cache.getStats();
                sender.sendMessage(Component.text("Cache: " + stats.hits() + " hits, " + stats.misses() + " misses (" + String.format("%.2f", stats.hitRate()) + "% hit rate)", NamedTextColor.GRAY));
            }
            
            sender.sendMessage(Component.text("Active fishing sessions: " + plugin.getActiveFishingCount(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Folia support: " + plugin.isFoliaSupported(), NamedTextColor.GRAY));
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendMessage(context.getSource().getSender(), Component.text("Error executing debug command: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error executing debug command", e);
            return 0;
        }
    }
}
