package io.xcutiboo.mythicrod.spigot.gui.menus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.spigot.gui.utils.ItemBuilder;
import io.xcutiboo.mythicrod.metrics.StatisticsManager.PlayerStats;
public class StatsMenu extends BaseMenu {
    private boolean viewingLeaderboard = false;
    public StatsMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }
    @Override
    protected int getSize() {
        return 54;
    }
    @Override
    protected String getTitle() {
        if (viewingLeaderboard) {
            return "&6&lMythicRod &8⚡ &7Top Fishers";
        }
        return "&6&lMythicRod &8⚡ &7Your Statistics";
    }
    @Override
    protected void build() {
        if (viewingLeaderboard) {
            buildLeaderboard();
        } else {
            buildPersonalStats();
        }
    }
    private void buildPersonalStats() {
        fillBorder();
        PlayerStats stats = plugin.getStatisticsManager().getPlayerStats(player);
        // Total catches display
        ItemStack totalItem = new ItemBuilder(Material.FISHING_ROD)
                .name("&e&lTotal Catches")
                .lore(
                        "&7You've caught a total of",
                        "",
                        "&6" + stats.getTotalCatches() + " items",
                        "",
                        "&7Keep fishing to increase this!"
                )
                .glow(true)
                .build();
        setItem(11, totalItem);
        // Rare catches display
        ItemStack rareItem = new ItemBuilder(Material.DIAMOND)
                .name("&b&lRare Catches")
                .lore(
                        "&7Special items you've caught",
                        "&7with low drop rates",
                        "",
                        "&b" + stats.getRareCatches() + " rare items",
                        "",
                        "&7Rare catches have ≤5% weight"
                )
                .glow(true)
                .build();
        setItem(13, rareItem);
        // Success rate calculation
        double rareRate = stats.getTotalCatches() > 0
                ? (stats.getRareCatches() * 100.0 / stats.getTotalCatches())
                : 0.0;
        ItemStack rateItem = new ItemBuilder(Material.CLOCK)
                .name("&d&lRare Drop Rate")
                .lore(
                        "&7Percentage of your catches",
                        "&7that were rare items",
                        "",
                        "&d" + String.format("%.2f", rareRate) + "%",
                        "",
                        "&7Higher is luckier!"
                )
                .build();
        setItem(15, rateItem);
        // Top materials section
        Map<String, Integer> materialCounts = stats.getMaterialCounts();
        if (!materialCounts.isEmpty()) {
            List<Map.Entry<String, Integer>> topMaterials = materialCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .toList();
            // Display title
            ItemStack topMaterialsTitle = new ItemBuilder(Material.CHEST)
                    .name("&6&lYour Top Materials")
                    .lore("&7Items you've caught most often")
                    .build();
            setItem(29, topMaterialsTitle);
            // Display top materials
            int slot = 30;
            for (Map.Entry<String, Integer> entry : topMaterials) {
                try {
                    Material material = Material.valueOf(entry.getKey());
                    ItemStack materialItem = new ItemBuilder(material)
                            .name("&e" + formatMaterialName(entry.getKey()))
                            .lore(
                                    "&7Caught: &f" + entry.getValue() + " times",
                                    "",
                                    "&7Percentage: &f" + String.format("%.1f",
                                            (entry.getValue() * 100.0 / stats.getTotalCatches())) + "%"
                            )
                            .build();
                    setItem(slot, materialItem);
                    slot++;
                    if (slot >= 35) break;
                } catch (IllegalArgumentException e) {
                    // Invalid material, skip it
                }
            }
        } else {
            ItemStack noDataItem = new ItemBuilder(Material.BARRIER)
                    .name("&c&lNo Data Yet")
                    .lore(
                            "&7Go fishing to start tracking",
                            "&7your catch statistics!"
                    )
                    .build();
            setItem(31, noDataItem);
        }
        // Leaderboard button
        ItemStack leaderboardItem = new ItemBuilder(Material.GOLDEN_APPLE)
                .name("&6&lView Leaderboard")
                .lore(
                        "&7See the top fishers on the server",
                        "",
                        "&eClick to view"
                )
                .glow(true)
                .build();
        setItem(49, leaderboardItem, event -> {
            viewingLeaderboard = true;
            refresh();
        });
        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("&e← Back to Main Menu")
                .build();
        setItem(45, backItem, event -> {
            plugin.getGUIManager().openMainHub(player);
        });
        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("&c&lClose")
                .build();
        setItem(53, closeItem, event -> player.closeInventory());
    }
    private void buildLeaderboard() {
        fillBorder();
        Map<UUID, Integer> topFishers = plugin.getStatisticsManager().getTopFishers(10);
        if (topFishers == null || topFishers.isEmpty()) {
            ItemStack noDataItem = new ItemBuilder(Material.BARRIER)
                    .name("&c&lNo Statistics Yet")
                    .lore(
                            "&7No one has caught anything yet!",
                            "&7Be the first to start fishing."
                    )
                    .build();
            setItem(22, noDataItem);
        } else {
            // Display top fishers with proper slot calculation
            List<Map.Entry<UUID, Integer>> entries = new ArrayList<>(topFishers.entrySet());
            int displaySlot = 0; // Index within content area
            final int contentStart = 10;
            final int contentPerRow = 7; // Slots 10-16, 19-25, 28-34, 37-43
            for (Map.Entry<UUID, Integer> entry : entries) {
                // Calculate actual slot position
                int row = displaySlot / contentPerRow;
                int col = displaySlot % contentPerRow;
                int slot = contentStart + (row * 9) + col;
                // Don't go past row 4
                if (row >= 4) {
                    break;
                }
                OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(entry.getKey());
                String playerName = topPlayer.getName() != null ? topPlayer.getName() : "Unknown";
                int catches = Math.max(0, entry.getValue());
                int rank = displaySlot + 1;
                // Choose medal/icon based on rank
                Material icon = switch (rank) {
                    case 1 -> Material.GOLD_BLOCK;
                    case 2 -> Material.IRON_BLOCK;
                    case 3 -> Material.COPPER_BLOCK;
                    default -> Material.PLAYER_HEAD;
                };
                boolean isCurrentPlayer = entry.getKey().equals(player.getUniqueId());
                List<String> lore = new ArrayList<>();
                lore.add("&7Player: &f" + playerName);
                lore.add("&7Total Catches: &f" + catches);
                lore.add("");
                if (isCurrentPlayer) {
                    lore.add("&a&lThis is you!");
                }
                if (rank <= 3) {
                    lore.add("&e&l" + getOrdinal(rank) + " Place!");
                }
                ItemStack playerItem = new ItemBuilder(icon)
                        .name("&6#" + rank + " &e" + playerName)
                        .lore(lore)
                        .glow(isCurrentPlayer)
                        .build();
                setItem(slot, playerItem);
                displaySlot++;
            }
        }
        // Info panel
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name("&6&lLeaderboard Info")
                .lore(
                        "&7This leaderboard shows the",
                        "&7top 10 fishers by total catches",
                        "",
                        "&7Keep fishing to climb higher!"
                )
                .build();
        setItem(49, infoItem);
        // Back to stats button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("&e← Back to Your Stats")
                .build();
        setItem(45, backItem, event -> {
            viewingLeaderboard = false;
            refresh();
        });
        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("&c&lClose")
                .build();
        setItem(53, closeItem, event -> player.closeInventory());
    }
    private void fillBorder() {
        ItemStack borderItem = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        for (int i = 0; i < 9; i++) {
            setItem(i, borderItem);
            setItem(45 + i, borderItem);
        }
        for (int row = 1; row < 5; row++) {
            setItem(row * 9, borderItem);
            setItem(row * 9 + 8, borderItem);
        }
    }
    private String formatMaterialName(String materialName) {
        String[] parts = materialName.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
    private String getOrdinal(int number) {
        if (number >= 11 && number <= 13) {
            return number + "th";
        }
        return switch (number % 10) {
            case 1 -> number + "st";
            case 2 -> number + "nd";
            case 3 -> number + "rd";
            default -> number + "th";
        };
    }
}

