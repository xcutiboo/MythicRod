package io.xcutiboo.mythicrod.metrics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.internal.runtime.MythicRodRuntime;
import io.xcutiboo.mythicrod.stats.PlayerStats;

class StatisticsManagerTest {

    @TempDir
    private Path dataFolder;

    @Test
    void recordCatchIncrementsTierCountAndTotal() {
        FakeRuntime runtime = new FakeRuntime(dataFolder.toFile());
        StatisticsManager manager = new StatisticsManager(runtime);
        manager.initialize();
        UUID uuid = UUID.randomUUID();
        manager.loadPlayer(uuid, "Daisy");

        manager.recordCatch(uuid, "common");
        manager.recordCatch(uuid, "common");
        manager.recordCatch(uuid, "legendary");

        PlayerStats stats = manager.getStats(uuid);
        assertEquals(3, stats.getTotalCaught());
        assertEquals(2, stats.getCommonCaught());
        assertEquals(1, stats.getLegendaryCaught());
    }

    @Test
    void recordRodUseIncrementsCorrectRodCounter() {
        FakeRuntime runtime = new FakeRuntime(dataFolder.toFile());
        StatisticsManager manager = new StatisticsManager(runtime);
        manager.initialize();
        UUID uuid = UUID.randomUUID();
        manager.loadPlayer(uuid, "Daisy");

        manager.recordRodUse(uuid, "basic");
        manager.recordRodUse(uuid, "basic");
        manager.recordRodUse(uuid, "advanced");
        manager.recordRodUse(uuid, "legendary");

        PlayerStats stats = manager.getStats(uuid);
        assertEquals(2, stats.getBasicRodUses());
        assertEquals(1, stats.getAdvancedRodUses());
        assertEquals(1, stats.getLegendaryRodUses());
    }

    @Test
    void resetStatsClearsLoadedCountersAndPersistsZeros() {
        FakeRuntime runtime = new FakeRuntime(dataFolder.toFile());
        StatisticsManager manager = new StatisticsManager(runtime);
        manager.initialize();
        UUID uuid = UUID.randomUUID();
        manager.loadPlayer(uuid, "Daisy");
        manager.recordCatch(uuid, "rare");
        manager.recordCatch(uuid, "rare");
        manager.saveAll();

        manager.resetStats(uuid);

        PlayerStats stats = manager.getStats(uuid);
        assertEquals(0, stats.getTotalCaught());
        assertEquals(0, stats.getRareCaught());
    }

    @Test
    void getTopFishersReturnsHighestTotalsFirst() {
        FakeRuntime runtime = new FakeRuntime(dataFolder.toFile());
        StatisticsManager manager = new StatisticsManager(runtime);
        manager.initialize();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        manager.loadPlayer(a, "A");
        manager.loadPlayer(b, "B");
        manager.loadPlayer(c, "C");
        manager.recordCatch(a, "common");
        manager.recordCatch(b, "common");
        manager.recordCatch(b, "common");
        manager.recordCatch(c, "common");
        manager.recordCatch(c, "common");
        manager.recordCatch(c, "common");

        List<PlayerStats> top = manager.getTopFishers(2);
        assertEquals(2, top.size());
        assertEquals(c, top.get(0).getPlayerUuid());
        assertEquals(b, top.get(1).getPlayerUuid());
    }

    @Test
    void persistsLastFishedTimestamp() {
        FakeRuntime runtime = new FakeRuntime(dataFolder.toFile());
        StatisticsManager manager = new StatisticsManager(runtime);
        UUID playerId = UUID.randomUUID();

        manager.initialize();
        manager.loadPlayer(playerId, "Alex");
        manager.recordCatch(playerId, "rare");

        PlayerStats recordedStats = manager.getStats(playerId);
        assertNotNull(recordedStats);
        long recordedLastFished = recordedStats.getLastFished();
        assertTrue(recordedLastFished > 0L);

        manager.saveAll();

        StatisticsManager restoredManager = new StatisticsManager(runtime);
        restoredManager.initialize();

        PlayerStats restoredStats = restoredManager.getStats(playerId);
        assertNotNull(restoredStats);
        assertEquals(recordedLastFished, restoredStats.getLastFished());
    }

    private static final class FakeRuntime implements MythicRodRuntime {
        private final File dataFolder;
        private final Logger logger = Logger.getLogger(FakeRuntime.class.getName());
        private final MapPlatformConfiguration config = new MapPlatformConfiguration();

        private FakeRuntime(File dataFolder) {
            this.dataFolder = dataFolder;
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public File getDataFolder() {
            return dataFolder;
        }

        @Override
        public PlatformServer getPlatform() {
            return null;
        }

        @Override
        public PlatformConfiguration loadConfig(File file) {
            return config;
        }

        @Override
        public PlatformConfiguration createEmptyConfig() {
            return new MapPlatformConfiguration();
        }
    }

    private static final class MapPlatformConfiguration implements PlatformConfiguration {
        private final Map<String, Object> values;
        private final String prefix;

        private MapPlatformConfiguration() {
            this(new ConcurrentHashMap<>(), "");
        }

        private MapPlatformConfiguration(Map<String, Object> values, String prefix) {
            this.values = values;
            this.prefix = prefix;
        }

        @Override
        public boolean contains(String path) {
            return values.containsKey(qualify(path));
        }

        @Override
        public String getString(String path) {
            return getString(path, null);
        }

        @Override
        public String getString(String path, String def) {
            Object value = values.get(qualify(path));
            return value instanceof String stringValue ? stringValue : def;
        }

        @Override
        public int getInt(String path) {
            return getInt(path, 0);
        }

        @Override
        public int getInt(String path, int def) {
            Object value = values.get(qualify(path));
            return value instanceof Number number ? number.intValue() : def;
        }

        @Override
        public boolean getBoolean(String path) {
            return getBoolean(path, false);
        }

        @Override
        public boolean getBoolean(String path, boolean def) {
            Object value = values.get(qualify(path));
            return value instanceof Boolean booleanValue ? Boolean.TRUE.equals(booleanValue) : def;
        }

        @Override
        public double getDouble(String path) {
            return getDouble(path, 0.0D);
        }

        @Override
        public double getDouble(String path, double def) {
            Object value = values.get(qualify(path));
            return value instanceof Number number ? number.doubleValue() : def;
        }

        @Override
        public List<String> getStringList(String path) {
            return Collections.emptyList();
        }

        @Override
        public List<Map<?, ?>> getMapList(String path) {
            return Collections.emptyList();
        }

        @Override
        public void set(String path, Object value) {
            values.put(qualify(path), value);
        }

        @Override
        public Set<String> getKeys(boolean deep) {
            Set<String> keys = new LinkedHashSet<>();
            String keyPrefix = prefix.isEmpty() ? "" : prefix + ".";
            for (String key : values.keySet()) {
                if (!key.startsWith(keyPrefix)) {
                    continue;
                }

                String relativeKey = key.substring(keyPrefix.length());
                if (relativeKey.isEmpty()) {
                    continue;
                }
                keys.add(deep ? relativeKey : relativeKey.split("\\.", 2)[0]);
            }
            return keys;
        }

        @Override
        public PlatformConfiguration getSection(String path) {
            String absolutePath = qualify(path);
            String sectionPrefix = absolutePath + ".";
            boolean hasValues = values.keySet().stream().anyMatch(key -> key.startsWith(sectionPrefix));
            return hasValues ? new MapPlatformConfiguration(values, absolutePath) : null;
        }

        @Override
        public void save(File file) throws IOException {
            // Test stub: in-memory configuration is never persisted to disk.
        }

        private String qualify(String path) {
            if (path == null || path.isEmpty()) {
                return prefix;
            }
            return prefix.isEmpty() ? path : prefix + "." + path;
        }
    }
}
