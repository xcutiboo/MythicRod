package io.xcutiboo.mythicrod.gui.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.item.ItemBuilder;
import io.xcutiboo.mythicrod.stats.PlayerStats;

/**
 * Statistics GUI menu showing personal stats and global leaderboard.
 *
 * <p>Uses {@link io.xcutiboo.mythicrod.metrics.StatisticsManager#getOrCreate(UUID)}
 * for personal stats (creates an empty entry for new players) and
 * {@link io.xcutiboo.mythicrod.metrics.StatisticsManager#getTopFishers(int)}
 * for the leaderboard (returns {@code List<PlayerStats>} sorted by totalCaught).
 */
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
            return tr("gui.stats.leaderboard_title");
        }
        return tr("gui.stats.title");
    }

    @Override
    protected void build() {
        if (viewingLeaderboard) {
            buildLeaderboard();
        } else {
            buildPersonalStats();
        }
    }

    // =========================================================================
    // Personal stats panel
    // =========================================================================

    private void buildPersonalStats() {
        fillBorder(Material.PURPLE_STAINED_GLASS_PANE);

        // getOrCreate — never null, creates empty stats for first-time viewers
        PlayerStats stats = plugin.getStatisticsManager().getOrCreate(getPlayer().getUniqueId());

        // ── Total catches ──────────────────────────────────────────────────────
        ItemStack totalItem = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.stats.total_catches"))
                .lore(
                        tr("gui.stats.total_catches_lore1"),
                        "",
                        tr("gui.stats.total_catches_lore2",
                                Map.of("count", String.valueOf(stats.getTotalCaught()))),
                        "",
                        tr("gui.stats.total_catches_lore3")
                )
                .glow(true)
                .build();
        setItem(11, totalItem);

        // ── Rare catches ───────────────────────────────────────────────────────
        ItemStack rareItem = new ItemBuilder(Material.DIAMOND)
                .name(tr("gui.stats.rare_catches"))
                .lore(
                        tr("gui.stats.rare_catches_lore1"),
                        tr("gui.stats.rare_catches_lore2"),
                        "",
                        tr("gui.stats.rare_catches_lore3",
                                Map.of("count", String.valueOf(stats.getRareCaught()))),
                        "",
                        tr("gui.stats.rare_catches_lore4")
                )
                .glow(true)
                .build();
        setItem(13, rareItem);

        // ── Drop rate ──────────────────────────────────────────────────────────
        int total = stats.getTotalCaught();
        double rareRate = total > 0
                ? (stats.getRareCaught() * 100.0 / total)
                : 0.0;
        ItemStack rateItem = new ItemBuilder(Material.CLOCK)
                .name(tr("gui.stats.drop_rate"))
                .lore(
                        tr("gui.stats.drop_rate_lore1"),
                        tr("gui.stats.drop_rate_lore2"),
                        "",
                        tr("gui.stats.drop_rate_lore3",
                                Map.of("rate", String.format("%.2f%%", rareRate))),
                        "",
                        tr("gui.stats.drop_rate_lore4")
                )
                .build();
        setItem(15, rateItem);

        // ── Top materials ──────────────────────────────────────────────────────
        Map<String, Integer> materialCounts = stats.getMaterialCounts();
        if (!materialCounts.isEmpty()) {
            List<Map.Entry<String, Integer>> topMaterials = materialCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            ItemStack topMaterialsTitle = new ItemBuilder(Material.CHEST)
                    .name(tr("gui.stats.top_materials"))
                    .lore(tr("gui.stats.top_materials_lore"))
                    .build();
            setItem(29, topMaterialsTitle);

            int slot = 30;
            for (Map.Entry<String, Integer> entry : topMaterials) {
                try {
                    Material material = Material.valueOf(entry.getKey());
                    double pct = entry.getValue() * 100.0 / Math.max(1, total);
                    ItemStack materialItem = new ItemBuilder(material)
                            .name(tr("gui.stats.material_name",
                                    Map.of("material", formatMaterialName(entry.getKey()))))
                            .lore(
                                    tr("gui.stats.material_caught",
                                            Map.of("count", String.valueOf(entry.getValue()))),
                                    "",
                                    tr("gui.stats.material_percentage",
                                            Map.of("percent", String.format("%.1f%%", pct)))
                            )
                            .build();
                    setItem(slot, materialItem);
                    slot++;
                    if (slot >= 35) break;
                } catch (IllegalArgumentException ignored) {
                }
            }
        } else {
            ItemStack noDataItem = new ItemBuilder(Material.BARRIER)
                    .name(tr("gui.stats.no_data"))
                    .lore(
                            tr("gui.stats.no_data_lore1"),
                            tr("gui.stats.no_data_lore2")
                    )
                    .build();
            setItem(31, noDataItem);
        }

        // ── Navigation ─────────────────────────────────────────────────────────
        ItemStack leaderboardItem = new ItemBuilder(Material.GOLDEN_APPLE)
                .name(tr("gui.stats.view_leaderboard"))
                .lore(
                        tr("gui.stats.view_leaderboard_lore1"),
                        "",
                        tr("gui.stats.view_leaderboard_lore2")
                )
                .glow(true)
                .build();
        setItem(49, leaderboardItem, event -> {
            playClickSound();
            viewingLeaderboard = true;
            refresh();
        });

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name(tr("gui.stats.back_main"))
                .build();
        setItem(45, backItem, event -> {
            playClickSound();
            plugin.getGUIManager().openMainHub(getPlayer());
        });

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.stats.close"))
                .build();
        setItem(53, closeItem, event -> {
            playClickSound();
            getPlayer().closeInventory();
        });
    }

    // =========================================================================
    // Global leaderboard panel
    // =========================================================================

    private void buildLeaderboard() {
        fillBorder(Material.PURPLE_STAINED_GLASS_PANE);

        // getTopFishers returns List<PlayerStats> sorted desc by totalCaught
        List<PlayerStats> topFishers = plugin.getStatisticsManager().getTopFishers(10);

        if (topFishers.isEmpty()) {
            ItemStack noDataItem = new ItemBuilder(Material.BARRIER)
                    .name(tr("gui.stats.no_statistics"))
                    .lore(
                            tr("gui.stats.no_statistics_lore1"),
                            tr("gui.stats.no_statistics_lore2")
                    )
                    .build();
            setItem(22, noDataItem);
        } else {
            int displaySlot = 0;
            final int contentStart   = 10;
            final int contentPerRow  = 7; // slots 10-16, 19-25, 28-34, 37-43

            for (PlayerStats topStats : topFishers) {
                int row  = displaySlot / contentPerRow;
                int col  = displaySlot % contentPerRow;
                int slot = contentStart + (row * 9) + col;
                if (row >= 4) break;

                UUID topUuid = topStats.getPlayerUuid();

                // Prefer cached player name; fall back to OfflinePlayer lookup
                String playerName = topStats.getPlayerName();
                if (playerName == null || playerName.isEmpty()) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(topUuid);
                    playerName = offlinePlayer.getName() != null
                            ? offlinePlayer.getName()
                            : "Unknown";
                }

                int catches          = Math.max(0, topStats.getTotalCaught());
                int rank             = displaySlot + 1;
                boolean isCurrentPlayer = topUuid.equals(getPlayer().getUniqueId());

                Material icon = switch (rank) {
                    case 1  -> Material.GOLD_BLOCK;
                    case 2  -> Material.IRON_BLOCK;
                    case 3  -> Material.COPPER_BLOCK;
                    default -> Material.PLAYER_HEAD;
                };

                List<String> lore = new ArrayList<>();
                lore.add(tr("gui.stats.player_label",   Map.of("name",  playerName)));
                lore.add(tr("gui.stats.player_catches",  Map.of("count", String.valueOf(catches))));
                lore.add("");
                if (isCurrentPlayer) {
                    lore.add(tr("gui.stats.you_indicator"));
                }
                if (rank <= 3) {
                    lore.add(tr("gui.stats.place_indicator",
                            Map.of("ordinal", getOrdinal(rank))));
                }

                ItemStack playerItem = new ItemBuilder(icon)
                        .name(tr("gui.stats.player_entry",
                                Map.of("rank", String.valueOf(rank), "name", playerName)))
                        .lore(lore)
                        .glow(isCurrentPlayer)
                        .build();
                setItem(slot, playerItem);
                displaySlot++;
            }
        }

        // ── Navigation ─────────────────────────────────────────────────────────
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name(tr("gui.stats.leaderboard_info"))
                .lore(
                        tr("gui.stats.leaderboard_info_lore1"),
                        tr("gui.stats.leaderboard_info_lore2"),
                        "",
                        tr("gui.stats.leaderboard_info_lore3")
                )
                .build();
        setItem(49, infoItem);

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name(tr("gui.stats.back_stats"))
                .build();
        setItem(45, backItem, event -> {
            playClickSound();
            viewingLeaderboard = false;
            refresh();
        });

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.stats.close"))
                .build();
        setItem(53, closeItem, event -> {
            playClickSound();
            getPlayer().closeInventory();
        });
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String formatMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) return "Unknown";
        String[] parts = materialName.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (result.length() > 0) result.append(" ");
            result.append(part.substring(0, 1).toUpperCase(java.util.Locale.ROOT))
                  .append(part.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return result.toString();
    }

    private String getOrdinal(int number) {
        if (number >= 11 && number <= 13) return number + "th";
        return switch (number % 10) {
            case 1  -> number + "st";
            case 2  -> number + "nd";
            case 3  -> number + "rd";
            default -> number + "th";
        };
    }
}
