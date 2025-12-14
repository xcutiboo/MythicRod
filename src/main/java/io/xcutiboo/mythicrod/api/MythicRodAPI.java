package io.xcutiboo.mythicrod.api;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
public class MythicRodAPI {
    private final MythicRod plugin;
    public MythicRodAPI(MythicRod plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin instance cannot be null");
        }
        this.plugin = plugin;
    }
    public ItemStack getRandomDrop(Player player, String biomeName) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        CustomDrop drop = plugin.getDropManager().getRandomDrop(player, biomeName);
        return drop != null ? drop.createItemStack() : null;
    }
    public boolean recordCatch(Player player, Material material, int amount) {
        if (player == null || material == null) {
            throw new IllegalArgumentException("Player and material cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!plugin.getConfigManager().trackStatistics()) {
            return false;
        }
        CustomDrop drop = new CustomDrop(material, 10, amount);
        plugin.getStatisticsManager().recordCatch(player, drop);
        return true;
    }
    public Map<String, Object> getPlayerStatistics(OfflinePlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (!plugin.getConfigManager().trackStatistics()) {
            return Collections.emptyMap();
        }
        StatisticsManager.PlayerStats stats = plugin.getStatisticsManager().getPlayerStats(player);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalCatches", stats.getTotalCatches());
        result.put("rareCatches", stats.getRareCatches());
        result.put("materialCounts", stats.getMaterialCounts());
        return Collections.unmodifiableMap(result);
    }
    public Map<UUID, Integer> getTopFishers(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        if (!plugin.getConfigManager().trackStatistics()) {
            return Collections.emptyMap();
        }
        return plugin.getStatisticsManager().getTopFishers(limit);
    }
    public Map<String, List<CustomDrop>> getDropCategories() {
        return Collections.unmodifiableMap(plugin.getDropManager().getDropCategories());
    }
    public boolean hasDropCategoryPermission(Player player, String category) {
        if (player == null || category == null) {
            throw new IllegalArgumentException("Player and category cannot be null");
        }
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
            plugin.getLogger().log(Level.SEVERE, "Error reloading MythicRod", e);
            return false;
        }
    }
}
