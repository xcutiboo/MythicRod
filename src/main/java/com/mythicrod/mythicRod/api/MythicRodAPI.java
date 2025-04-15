package com.mythicrod.mythicrod.api;

import com.mythicrod.mythicrod.MythicRod;
import com.mythicrod.mythicrod.drops.CustomDrop;
import com.mythicrod.mythicrod.metrics.StatisticsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MythicRodAPI {

    private final MythicRod plugin;

    public MythicRodAPI(MythicRod plugin) {
        this.plugin = plugin;
    }

    public ItemStack getRandomDrop(Player player, String biomeName) {
        CustomDrop drop = plugin.getDropManager().getRandomDrop(player, biomeName);
        return drop != null ? drop.createItemStack() : null;
    }

    public boolean recordCatch(Player player, Material material, int amount) {
        if (!plugin.getConfigManager().trackStatistics()) {
            return false;
        }

        CustomDrop drop = new CustomDrop(material, 10, amount);
        plugin.getStatisticsManager().recordCatch(player, drop);
        return true;
    }

    public Map<String, Object> getPlayerStatistics(OfflinePlayer player) {
        if (!plugin.getConfigManager().trackStatistics()) {
            return null;
        }

        StatisticsManager.PlayerStats stats = plugin.getStatisticsManager().getPlayerStats(player);
        Map<String, Object> result = new java.util.HashMap<>();

        result.put("totalCatches", stats.getTotalCatches());
        result.put("rareCatches", stats.getRareCatches());
        result.put("materialCounts", stats.getMaterialCounts());

        return result;
    }

    public Map<UUID, Integer> getTopFishers(int limit) {
        if (!plugin.getConfigManager().trackStatistics()) {
            return java.util.Collections.emptyMap();
        }

        return plugin.getStatisticsManager().getTopFishers(limit);
    }

    public Map<String, List<CustomDrop>> getDropCategories() {
        return plugin.getDropManager().getDropCategories();
    }

    public boolean hasDropCategoryPermission(Player player, String category) {
        if (!plugin.getConfigManager().usePermissions()) {
            return true;
        }

        return player.hasPermission("mythicrod.drops." + category);
    }

    public boolean reloadConfig() {
        try {
            plugin.reload();
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error reloading MythicRod: " + e.getMessage());
            return false;
        }
    }
}
