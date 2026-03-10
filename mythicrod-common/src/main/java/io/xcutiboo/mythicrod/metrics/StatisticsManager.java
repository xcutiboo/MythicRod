package io.xcutiboo.mythicrod.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.drops.CustomDrop;

public class StatisticsManager {
    private final MythicRodPlugin plugin;
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();

    public StatisticsManager(MythicRodPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (plugin.getConfigManager().trackStatistics()) {
            loadAllStats();
            startAutoSaveTask();
        }
    }

    private void startAutoSaveTask() {
        long saveIntervalTicks = plugin.getConfigManager().getStatsSaveInterval() * 20L;
        plugin.getPlatform().getScheduler().runAsyncTimer(this::saveAll, saveIntervalTicks, saveIntervalTicks);
    }

    private void loadAllStats() {
        PlatformConfiguration statsConfig = plugin.getConfigManager().getStatsConfig();
        if (statsConfig == null) {
            plugin.getLogger().warning("Could not load statistics - config is null");
            return;
        }

        PlatformConfiguration playersSection = statsConfig.getSection("players");
        if (playersSection == null) {
            return;
        }

        for (String uuidString : playersSection.getKeys("", false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                PlatformConfiguration playerSection = playersSection.getSection(uuidString);
                if (playerSection == null) {
                    continue;
                }

                int totalCatches = playerSection.getInt("total-catches", 0);
                int rareCatches = playerSection.getInt("rare-catches", 0);

                PlayerStats stats = new PlayerStats(uuid);
                stats.setTotalCatches(totalCatches);
                stats.setRareCatches(rareCatches);

                PlatformConfiguration materialsSection = playerSection.getSection("materials");
                if (materialsSection != null) {
                    for (String material : materialsSection.getKeys("", false)) {
                        int count = materialsSection.getInt(material, 0);
                        stats.getMaterialCounts().put(material, count);
                    }
                }

                playerStats.put(uuid, stats);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in statistics: " + uuidString);
            }
        }

        plugin.getLogger().info("Loaded statistics for " + playerStats.size() + " players");
    }

    public void recordCatch(PlatformPlayer player, CustomDrop drop) {
        if (!plugin.getConfigManager().trackStatistics()) {
            return;
        }

        UUID playerUuid = player.getUniqueId();
        PlayerStats stats = playerStats.computeIfAbsent(playerUuid, PlayerStats::new);
        
        stats.incrementTotalCatches();
        
        String identifier = drop.getIdentifier();
        stats.incrementMaterialCount(identifier);
        
        // Items with chance <= 5% are considered rare
        int rareThreshold = 5;
        if (drop.getChance() <= rareThreshold) {
            stats.incrementRareCatches();
        }
    }

    public PlayerStats getPlayerStats(UUID playerUuid) {
        return playerStats.computeIfAbsent(playerUuid, PlayerStats::new);
    }

    public PlayerStats getPlayerStats(PlatformPlayer player) {
        return getPlayerStats(player.getUniqueId());
    }

    public Map<UUID, Integer> getTopFishers(int limit) {
        Map<UUID, Integer> result = new LinkedHashMap<>();
        List<Map.Entry<UUID, PlayerStats>> entries = new ArrayList<>(playerStats.entrySet());
        
        entries.sort((e1, e2) -> e2.getValue().getTotalCatches() - e1.getValue().getTotalCatches());
        
        int count = 0;
        for (Map.Entry<UUID, PlayerStats> entry : entries) {
            if (count >= limit) {
                break;
            }
            result.put(entry.getKey(), entry.getValue().getTotalCatches());
            count++;
        }
        
        return result;
    }

    public void saveAll() {
        if (!plugin.getConfigManager().trackStatistics() || playerStats.isEmpty()) {
            return;
        }

        PlatformConfiguration statsConfig = plugin.getConfigManager().getStatsConfig();
        if (statsConfig == null) {
            plugin.getLogger().warning("Could not save statistics - config is null");
            return;
        }

        statsConfig.set("players", null);

        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerStats stats = entry.getValue();
            String uuidString = uuid.toString();

            statsConfig.set("players." + uuidString + ".total-catches", stats.getTotalCatches());
            statsConfig.set("players." + uuidString + ".rare-catches", stats.getRareCatches());

            for (Map.Entry<String, Integer> materialEntry : stats.getMaterialCounts().entrySet()) {
                statsConfig.set("players." + uuidString + ".materials." + materialEntry.getKey(), materialEntry.getValue());
            }
        }

        plugin.getConfigManager().saveStats();
    }

    public void reload() {
        playerStats.clear();
        if (plugin.getConfigManager().trackStatistics()) {
            loadAllStats();
        }
    }

    public void cleanup() {
        saveAll();
    }

    public int getTotalCatches() {
        return playerStats.values().stream()
            .mapToInt(PlayerStats::getTotalCatches)
            .sum();
    }

    public Map<String, Object> getPlayerStatistics(PlatformPlayer player) {
        PlayerStats stats = getPlayerStats(player);
        Map<String, Object> result = new HashMap<>();

        result.put("total_catches", stats.getTotalCatches());
        result.put("rare_catches", stats.getRareCatches());
        
        int totalItems = stats.getMaterialCounts().values().stream()
                .mapToInt(Integer::intValue).sum();
        result.put("total_items_caught", totalItems);

        result.put("unique_types", stats.getMaterialCounts().size());
        
        List<Map<String, Object>> topCatches = stats.getMaterialCounts().entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .map(entry -> {
                    Map<String, Object> catchData = new HashMap<>();
                    catchData.put("material", entry.getKey());
                    catchData.put("count", entry.getValue());
                    return catchData;
                })
                .toList();
        result.put("top_catches", topCatches);

        return result;
    }

    public static class PlayerStats {
        private final UUID playerUuid;
        private int totalCatches = 0;
        private int rareCatches = 0;
        private final Map<String, Integer> materialCounts = new HashMap<>();

        public PlayerStats(UUID playerUuid) {
            this.playerUuid = playerUuid;
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public int getTotalCatches() {
            return totalCatches;
        }

        public void setTotalCatches(int totalCatches) {
            this.totalCatches = totalCatches;
        }

        public void incrementTotalCatches() {
            totalCatches++;
        }

        public int getRareCatches() {
            return rareCatches;
        }

        public void setRareCatches(int rareCatches) {
            this.rareCatches = rareCatches;
        }

        public void incrementRareCatches() {
            rareCatches++;
        }

        public Map<String, Integer> getMaterialCounts() {
            return materialCounts;
        }

        public void incrementMaterialCount(String material) {
            materialCounts.put(material, materialCounts.getOrDefault(material, 0) + 1);
        }
    }
}
