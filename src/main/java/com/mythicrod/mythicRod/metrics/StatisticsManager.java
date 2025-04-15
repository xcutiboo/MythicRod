package com.mythicrod.mythicrod.metrics;

import com.mythicrod.mythicrod.MythicRod;
import com.mythicrod.mythicrod.drops.CustomDrop;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatisticsManager {

    private final MythicRod plugin;
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private BukkitTask autoSaveTask;

    public StatisticsManager(MythicRod plugin) {
        this.plugin = plugin;

        if (plugin.getConfigManager().trackStatistics()) {
            loadAllStats();
            startAutoSaveTask();
        }
    }

    private void startAutoSaveTask() {
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::saveAll, 12000L, 12000L);
    }

    private void loadAllStats() {
        FileConfiguration statsConfig = plugin.getConfigManager().getStatsConfig();

        if (statsConfig == null) {
            plugin.getLogger().warning("Could not load statistics - config is null");
            return;
        }

        ConfigurationSection playersSection = statsConfig.getConfigurationSection("players");

        if (playersSection == null) {
            return;
        }

        for (String uuidString : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidString);

                if (playerSection == null) {
                    continue;
                }

                int totalCatches = playerSection.getInt("total-catches", 0);
                int rareCatches = playerSection.getInt("rare-catches", 0);

                PlayerStats stats = new PlayerStats(uuid);
                stats.setTotalCatches(totalCatches);
                stats.setRareCatches(rareCatches);

                ConfigurationSection materialsSection = playerSection.getConfigurationSection("materials");
                if (materialsSection != null) {
                    for (String material : materialsSection.getKeys(false)) {
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

    public void recordCatch(Player player, CustomDrop drop) {
        if (!plugin.getConfigManager().trackStatistics()) {
            return;
        }

        UUID playerUuid = player.getUniqueId();
        PlayerStats stats = playerStats.computeIfAbsent(playerUuid, PlayerStats::new);

        stats.incrementTotalCatches();

        String materialName = drop.getMaterial().name();
        stats.incrementMaterialCount(materialName);

        int rareThreshold = 5;
        if (drop.getChance() <= rareThreshold) {
            stats.incrementRareCatches();
        }
    }

    public PlayerStats getPlayerStats(OfflinePlayer player) {
        UUID playerUuid = player.getUniqueId();
        return playerStats.computeIfAbsent(playerUuid, PlayerStats::new);
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

        FileConfiguration statsConfig = plugin.getConfigManager().getStatsConfig();

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
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        saveAll();
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
