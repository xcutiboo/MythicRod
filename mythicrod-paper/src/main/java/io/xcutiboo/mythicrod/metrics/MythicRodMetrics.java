package io.xcutiboo.mythicrod.metrics;

import io.xcutiboo.mythicrod.MythicRod;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class MythicRodMetrics {
    
    private static final int METRICS_ID = 23847;
    
    private final Metrics metrics;
    
    // Thread-safe counters for Folia compatibility
    private final AtomicInteger totalCatches = new AtomicInteger(0);
    private final AtomicInteger customItemCatches = new AtomicInteger(0);
    private final AtomicInteger legendaryCatches = new AtomicInteger(0);
    private final AtomicInteger rareCatches = new AtomicInteger(0);
    private final Map<String, AtomicInteger> rodTierUsage = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AtomicInteger>> biomeRarityData = new ConcurrentHashMap<>();
    
    private final AtomicInteger megaDropCount = new AtomicInteger(0);
    
    public MythicRodMetrics(MythicRod plugin) {
        this.metrics = new Metrics(plugin, METRICS_ID);
        
        rodTierUsage.put("basic", new AtomicInteger(0));
        rodTierUsage.put("advanced", new AtomicInteger(0));
        rodTierUsage.put("legendary", new AtomicInteger(0));
        rodTierUsage.put("epic", new AtomicInteger(0));
        
        initializeCharts();
    }
    
    private void initializeCharts() {
        // Server environment
        metrics.addCustomChart(new SimplePie("server_type", () -> "Paper"));
        metrics.addCustomChart(new SimplePie("folia_supported", () -> "Yes"));
        
        // Plugin configuration
        metrics.addCustomChart(new SimplePie("language", () -> "en_US"));
        metrics.addCustomChart(new SimplePie("statistics_enabled", () -> "Enabled"));
        metrics.addCustomChart(new SimplePie("biome_drops_enabled", () -> "Enabled"));
        
        // Usage metrics - thread-safe counters
        metrics.addCustomChart(new SingleLineChart("total_catches", () -> totalCatches.getAndSet(0)));
        metrics.addCustomChart(new SingleLineChart("legendary_catches", () -> legendaryCatches.getAndSet(0)));
        metrics.addCustomChart(new SingleLineChart("rare_catches", () -> rareCatches.getAndSet(0)));
        metrics.addCustomChart(new SingleLineChart("custom_item_drops", () -> customItemCatches.getAndSet(0)));
        metrics.addCustomChart(new SingleLineChart("mega_drops_triggered", () -> megaDropCount.getAndSet(0)));
        
        // Distribution charts
        metrics.addCustomChart(new SimplePie("preferred_rod_tier", this::getPreferredRodTier));
        metrics.addCustomChart(new DrilldownPie("biome_rarity_distribution", this::getBiomeRarityData));
    }
    
    private String getPreferredRodTier() {
        int basic = rodTierUsage.getOrDefault("basic", new AtomicInteger(0)).get();
        int advanced = rodTierUsage.getOrDefault("advanced", new AtomicInteger(0)).get();
        int legendary = rodTierUsage.getOrDefault("legendary", new AtomicInteger(0)).get();
        
        if (legendary >= advanced && legendary >= basic) return "legendary";
        if (advanced >= basic) return "advanced";
        return "basic";
    }
    
    private Map<String, Map<String, Integer>> getBiomeRarityData() {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        
        biomeRarityData.forEach((biome, rarityMap) -> {
            Map<String, Integer> rarityCounts = new HashMap<>();
            rarityMap.forEach((rarity, count) -> rarityCounts.put(rarity, count.get()));
            result.put(biome, rarityCounts);
        });
        
        return result;
    }
    
    public void recordRodUsage(String tier) {
        if (tier == null) return;
        rodTierUsage.computeIfAbsent(tier.toLowerCase(java.util.Locale.ROOT), k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    public void recordCatch(String biome, String rarity) {
        biomeRarityData.computeIfAbsent(biome.toLowerCase(java.util.Locale.ROOT), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(rarity.toLowerCase(java.util.Locale.ROOT), k -> new AtomicInteger(0))
            .incrementAndGet();
        
        if ("legendary".equalsIgnoreCase(rarity)) {
            legendaryCatches.incrementAndGet();
        }
    }
    
    public Metrics getMetrics() {
        return metrics;
    }
}
