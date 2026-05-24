package io.xcutiboo.mythicrod.paper.gui.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;
import io.xcutiboo.mythicrod.paper.util.StringFormatting;
import io.xcutiboo.mythicrod.stats.PlayerStats;

/// Statistics GUI menu showing personal stats and global leaderboard.
///
/// Uses {@link io.xcutiboo.mythicrod.metrics.StatisticsManager#getStats(UUID)}
/// for personal stats and falls back to a temporary zeroed view for first-time players,
/// while using
/// {@link io.xcutiboo.mythicrod.metrics.StatisticsManager#getTopFishers(int)}
/// for the leaderboard (returns `List<PlayerStats>` sorted by totalCaught).
public class StatsMenu extends BaseMenu {
    private static final String CTX_COUNT = "count";


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

    private void buildPersonalStats() {
        fillBorder(Material.PURPLE_STAINED_GLASS_PANE);

        PlayerStats stats = plugin.getStatisticsManager().getStats(getPlayer().getUniqueId());
        if (stats == null) {
            stats = new PlayerStats(getPlayer().getUniqueId(), getPlayer().getName());
        }
        int total = stats.getTotalCaught();
        int rarePlus = stats.getRareCaught() + stats.getLegendaryCaught();
        double rareRate = total > 0 ? (rarePlus * 100.0 / total) : 0.0;

        renderTotalsCard(total);
        renderRareCard(rarePlus);
        renderRateCard(rareRate);
        renderTierBreakdown(stats.getTierCounts(), total);
        renderLeaderboardButton();
        renderPersonalBackButton();
        renderPersonalCloseButton();
    }

    private void renderTotalsCard(int total) {
        ItemStack item = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.stats.total_catches"))
                .lore(
                        tr("gui.stats.total_catches_lore1"),
                        "",
                        tr("gui.stats.total_catches_lore2", Map.of(CTX_COUNT, String.valueOf(total))),
                        "",
                        tr("gui.stats.total_catches_lore3")
                )
                .glow(true)
                .build();
        setItem(11, item);
    }

    private void renderRareCard(int rarePlus) {
        ItemStack item = new ItemBuilder(Material.DIAMOND)
                .name(tr("gui.stats.rare_catches"))
                .lore(
                        tr("gui.stats.rare_catches_lore1"),
                        tr("gui.stats.rare_catches_lore2"),
                        "",
                        tr("gui.stats.rare_catches_lore3", Map.of(CTX_COUNT, String.valueOf(rarePlus))),
                        "",
                        tr("gui.stats.rare_catches_lore4")
                )
                .glow(true)
                .build();
        setItem(13, item);
    }

    private void renderRateCard(double rareRate) {
        ItemStack item = new ItemBuilder(Material.CLOCK)
                .name(tr("gui.stats.drop_rate"))
                .lore(
                        tr("gui.stats.drop_rate_lore1"),
                        tr("gui.stats.drop_rate_lore2"),
                        "",
                        tr("gui.stats.drop_rate_lore3", Map.of("rate", String.format("%.2f%%", rareRate))),
                        "",
                        tr("gui.stats.drop_rate_lore4")
                )
                .build();
        setItem(15, item);
    }

    private void renderTierBreakdown(Map<String, Integer> tierCounts, int total) {
        if (tierCounts.isEmpty()) {
            renderNoTierData();
            return;
        }
        List<Map.Entry<String, Integer>> topTiers = tierCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .toList();

        ItemStack title = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(tr("gui.stats.tier_breakdown"))
                .lore(tr("gui.stats.tier_breakdown_lore"))
                .build();
        setItem(29, title);

        int slot = 30;
        for (Map.Entry<String, Integer> entry : topTiers) {
            if (slot >= 35) break;
            setItem(slot, buildTierItem(entry, total));
            slot++;
        }
    }

    private ItemStack buildTierItem(Map.Entry<String, Integer> entry, int total) {
        double pct = entry.getValue() * 100.0 / Math.max(1, total);
        return new ItemBuilder(iconForTier(entry.getKey()))
                .name(tr("gui.stats.tier_name", Map.of("tier", StringFormatting.formatMaterialName(entry.getKey()))))
                .lore(
                        tr("gui.stats.tier_caught", Map.of(CTX_COUNT, String.valueOf(entry.getValue()))),
                        "",
                        tr("gui.stats.tier_percentage", Map.of("percent", String.format("%.1f%%", pct)))
                )
                .build();
    }

    private void renderNoTierData() {
        ItemStack item = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.stats.no_data"))
                .lore(tr("gui.stats.no_data_lore1"), tr("gui.stats.no_data_lore2"))
                .build();
        setItem(31, item);
    }

    private void renderLeaderboardButton() {
        ItemStack item = new ItemBuilder(Material.GOLDEN_APPLE)
                .name(tr("gui.stats.view_leaderboard"))
                .lore(
                        tr("gui.stats.view_leaderboard_lore1"),
                        "",
                        tr("gui.stats.view_leaderboard_lore2")
                )
                .glow(true)
                .build();
        setItem(49, item, () -> {
            playClickSound();
            viewingLeaderboard = true;
            refresh();
        });
    }

    private void renderPersonalBackButton() {
        ItemStack item = new ItemBuilder(Material.ARROW)
                .name(tr("gui.stats.back_main"))
                .build();
        setItem(45, item, () -> {
            playClickSound();
            plugin.getGUIManager().openMainHub(getPlayer());
        });
    }

    private void renderPersonalCloseButton() {
        ItemStack item = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.stats.close"))
                .build();
        setItem(53, item, () -> {
            playClickSound();
            getPlayer().closeInventory();
        });
    }

    private void buildLeaderboard() {
        fillBorder(Material.PURPLE_STAINED_GLASS_PANE);

        List<PlayerStats> topFishers = plugin.getStatisticsManager().getTopFishers(10);
        if (topFishers.isEmpty()) {
            renderNoLeaderboardData();
        } else {
            renderLeaderboardEntries(topFishers);
        }
        renderLeaderboardInfo();
        renderLeaderboardBackButton();
        renderLeaderboardCloseButton();
    }

    private void renderNoLeaderboardData() {
        ItemStack item = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.stats.no_statistics"))
                .lore(
                        tr("gui.stats.no_statistics_lore1"),
                        tr("gui.stats.no_statistics_lore2")
                )
                .build();
        setItem(22, item);
    }

    private void renderLeaderboardEntries(List<PlayerStats> topFishers) {
        final int contentStart = 10;
        final int contentPerRow = 7; // slots 10-16, 19-25, 28-34, 37-43
        int displaySlot = 0;

        for (PlayerStats topStats : topFishers) {
            int row = displaySlot / contentPerRow;
            if (row >= 4) break;
            int col = displaySlot % contentPerRow;
            int slot = contentStart + (row * 9) + col;
            int rank = displaySlot + 1;
            setItem(slot, buildLeaderboardEntry(topStats, rank));
            displaySlot++;
        }
    }

    private ItemStack buildLeaderboardEntry(PlayerStats topStats, int rank) {
        UUID topUuid = topStats.getPlayerUuid();
        String playerName = resolveLeaderboardName(topStats, topUuid);
        int catches = Math.max(0, topStats.getTotalCaught());
        boolean isCurrentPlayer = topUuid.equals(getPlayer().getUniqueId());

        Material icon = switch (rank) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            case 3 -> Material.COPPER_BLOCK;
            default -> Material.PLAYER_HEAD;
        };

        List<String> lore = new ArrayList<>();
        lore.add(tr("gui.stats.player_label", Map.of("name", playerName)));
        lore.add(tr("gui.stats.player_catches", Map.of(CTX_COUNT, String.valueOf(catches))));
        lore.add("");
        if (isCurrentPlayer) {
            lore.add(tr("gui.stats.you_indicator"));
        }
        if (rank <= 3) {
            lore.add(tr("gui.stats.place_indicator",
                    Map.of("ordinal", StringFormatting.getOrdinal(rank))));
        }

        return new ItemBuilder(icon)
                .name(tr("gui.stats.player_entry",
                        Map.of("rank", String.valueOf(rank), "name", playerName)))
                .lore(lore)
                .glow(isCurrentPlayer)
                .build();
    }

    private String resolveLeaderboardName(PlayerStats topStats, UUID topUuid) {
        String playerName = topStats.getPlayerName();
        if (!playerName.isEmpty()) return playerName;
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(topUuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
    }

    private void renderLeaderboardInfo() {
        ItemStack item = new ItemBuilder(Material.BOOK)
                .name(tr("gui.stats.leaderboard_info"))
                .lore(
                        tr("gui.stats.leaderboard_info_lore1"),
                        tr("gui.stats.leaderboard_info_lore2"),
                        "",
                        tr("gui.stats.leaderboard_info_lore3")
                )
                .build();
        setItem(49, item);
    }

    private void renderLeaderboardBackButton() {
        ItemStack item = new ItemBuilder(Material.ARROW)
                .name(tr("gui.stats.back_stats"))
                .build();
        setItem(45, item, () -> {
            playClickSound();
            viewingLeaderboard = false;
            refresh();
        });
    }

    private void renderLeaderboardCloseButton() {
        renderPersonalCloseButton();
    }

    private Material iconForTier(String tier) {
        return switch (tier) {
            case "legendary" -> Material.NETHER_STAR;
            case "rare" -> Material.DIAMOND;
            case "uncommon" -> Material.EMERALD;
            case "common" -> Material.COD;
            default -> Material.PAPER;
        };
    }
}
