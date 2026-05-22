package io.xcutiboo.mythicrod.paper.metrics;

import java.util.HashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import io.xcutiboo.mythicrod.paper.MythicRod;

/// Wires bStats custom charts so MythicRod's runtime state shows up on bstats.org.
///
/// The reporter is a lightweight read-only view onto the host plugin; it does
/// not hold mutable telemetry state. Charts are evaluated lazily by bStats on
/// each report cycle, so swapping managers during a reload is safe.
public final class MetricsReporter {

    private static final int BSTATS_PLUGIN_ID = 31484;

    private final MythicRod plugin;
    private Metrics metrics;

    public MetricsReporter(MythicRod plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            this.metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);
            registerCharts();
        } catch (RuntimeException | LinkageError e) {
            this.metrics = null;
            plugin.getSLF4JLogger().warn("bStats metrics are unavailable; continuing without telemetry", e);
        }
    }

    private void registerCharts() {
        if (metrics == null) return;
        registerEnvironmentCharts();
        registerConfigCharts();
        registerToggleCharts();
        registerCatalogCharts();
    }

    private void registerEnvironmentCharts() {
        metrics.addCustomChart(new SimplePie("folia_runtime", () -> plugin.isFoliaRuntime() ? "Folia" : "Paper"));
        metrics.addCustomChart(new SimplePie("language", () ->
            plugin.getLanguageManager() != null ? plugin.getLanguageManager().getLanguage() : "en"));
    }

    private void registerConfigCharts() {
        metrics.addCustomChart(new SimplePie("profile", () ->
            plugin.getConfigManager() != null ? plugin.getConfigManager().getProfile() : "balanced"));
        metrics.addCustomChart(new SimplePie("reward_delivery_mode", () ->
            plugin.getConfigManager() != null
                ? plugin.getConfigManager().getRewardDeliveryMode().getConfigValue()
                : "vanilla_retrieve"));
    }

    private void registerToggleCharts() {
        metrics.addCustomChart(new SimplePie("statistics_enabled", () ->
            enabledDisabled(plugin.getConfigManager() != null && plugin.getConfigManager().trackStatistics())));
        metrics.addCustomChart(new SimplePie("biome_drops_enabled", () ->
            enabledDisabled(plugin.getConfigManager() != null
                && plugin.getConfigManager().enableBiomeSpecificDrops())));
        metrics.addCustomChart(new SimplePie("permissions_enabled", () ->
            enabledDisabled(plugin.getConfigManager() != null && plugin.getConfigManager().usePermissions())));
        metrics.addCustomChart(new SimplePie("particles_enabled", () ->
            enabledDisabled(plugin.getConfigManager() != null && plugin.getConfigManager().useParticles())));
        metrics.addCustomChart(new SimplePie("sounds_enabled", () ->
            enabledDisabled(plugin.getConfigManager() != null && plugin.getConfigManager().useSounds())));
        metrics.addCustomChart(new SimplePie("nexo_enabled", () ->
            enabledDisabled(plugin.getPlatformServer() != null && plugin.getPlatformServer().isNexoEnabled())));
    }

    private void registerCatalogCharts() {
        metrics.addCustomChart(new SingleLineChart("configured_drops", () ->
            plugin.getDropManager() != null ? plugin.getDropManager().getTotalDropCount() : 0));
        metrics.addCustomChart(new SingleLineChart("configured_drop_categories", () ->
            plugin.getDropManager() != null ? plugin.getDropManager().getDropCategories().size() : 0));
        metrics.addCustomChart(new SingleLineChart("tracked_players", () ->
            plugin.getStatisticsManager() != null ? plugin.getStatisticsManager().getAllStats().size() : 0));
        metrics.addCustomChart(new SingleLineChart("total_catches", () ->
            plugin.getStatisticsManager() != null
                ? (int) Math.min(plugin.getStatisticsManager().getTotalCatches(), Integer.MAX_VALUE)
                : 0));
        metrics.addCustomChart(new AdvancedPie("drop_category_share", () -> {
            Map<String, Integer> share = new HashMap<>();
            if (plugin.getDropManager() == null) return share;
            plugin.getDropManager().getDropCategories().forEach((category, drops) -> {
                int count = drops != null ? drops.size() : 0;
                if (count > 0) share.put(category, count);
            });
            return share;
        }));
    }

    private static String enabledDisabled(boolean enabled) {
        return enabled ? "Enabled" : "Disabled";
    }
}
