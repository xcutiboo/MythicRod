package io.xcutiboo.mythicrod.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformItemFactory;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.api.platform.PlatformWorld;
import io.xcutiboo.mythicrod.internal.runtime.MythicRodRuntime;

class PlayerPreferencesTest {

    @TempDir
    Path tempDir;

    @Test
    void reloadFromDiskWaitsForInFlightSave() throws Exception {
        FakePlatformServer platform = new FakePlatformServer();
        PlayerPreferences preferences = new PlayerPreferences(new FakeRuntime(tempDir.toFile(), platform));
        UUID playerId = UUID.randomUUID();
        File playersFile = tempDir.resolve("players.yml").toFile();
        BlockingSave blockingSave = platform.blockNextSave();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            preferences.setLanguage(playerId, "ja_JP");
            assertTrue(blockingSave.awaitStarted(2, TimeUnit.SECONDS));

            Future<?> reloadFuture = executor.submit(preferences::reloadFromDisk);
            assertBlocked(reloadFuture);

            blockingSave.release();
            reloadFuture.get(2, TimeUnit.SECONDS);

            assertEquals("ja_JP", preferences.getLanguage(playerId));
            assertEquals("ja_JP", platform.getPersistedValue(playersFile, languagePath(playerId)));
        } finally {
            blockingSave.release();
            executor.shutdownNow();
            preferences.shutdown();
        }
    }

    @Test
    void shutdownWaitsForInFlightSave() throws Exception {
        FakePlatformServer platform = new FakePlatformServer();
        PlayerPreferences preferences = new PlayerPreferences(new FakeRuntime(tempDir.toFile(), platform));
        UUID playerId = UUID.randomUUID();
        File playersFile = tempDir.resolve("players.yml").toFile();
        BlockingSave blockingSave = platform.blockNextSave();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            preferences.setLanguage(playerId, "en_US");
            assertTrue(blockingSave.awaitStarted(2, TimeUnit.SECONDS));

            Future<?> shutdownFuture = executor.submit(preferences::shutdown);
            assertBlocked(shutdownFuture);

            blockingSave.release();
            shutdownFuture.get(2, TimeUnit.SECONDS);

            assertEquals("en_US", platform.getPersistedValue(playersFile, languagePath(playerId)));
        } finally {
            blockingSave.release();
            executor.shutdownNow();
        }
    }

    @Test
    void reloadFromDiskLoadsLatestPersistedSnapshot() {
        FakePlatformServer platform = new FakePlatformServer();
        FakeRuntime runtime = new FakeRuntime(tempDir.toFile(), platform);
        File playersFile = tempDir.resolve("players.yml").toFile();
        UUID playerId = UUID.randomUUID();
        String languagePath = languagePath(playerId);

        platform.putPersistedValue(playersFile, languagePath, "en_US");
        PlayerPreferences preferences = new PlayerPreferences(runtime);
        assertEquals("en_US", preferences.getLanguage(playerId));

        platform.putPersistedValue(playersFile, languagePath, "ja_JP");
        preferences.reloadFromDisk();

        assertEquals("ja_JP", preferences.getLanguage(playerId));
        preferences.shutdown();
    }

    @Test
    void reloadFromDiskKeepsExistingPreferenceVisibleWhileReloadIsBlocked() throws Exception {
        FakePlatformServer platform = new FakePlatformServer();
        FakeRuntime runtime = new FakeRuntime(tempDir.toFile(), platform);
        File playersFile = tempDir.resolve("players.yml").toFile();
        UUID playerId = UUID.randomUUID();
        String storedLanguagePath = languagePath(playerId);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        platform.putPersistedValue(playersFile, storedLanguagePath, "en_US");
        PlayerPreferences preferences = new PlayerPreferences(runtime);
        BlockingLoad blockingLoad = platform.blockNextPlayersKeyRead();

        try {
            Future<?> reloadFuture = executor.submit(preferences::reloadFromDisk);
            assertTrue(blockingLoad.awaitStarted(2, TimeUnit.SECONDS));

            assertEquals("en_US", preferences.getLanguage(playerId));

            blockingLoad.release();
            reloadFuture.get(2, TimeUnit.SECONDS);
            assertEquals("en_US", preferences.getLanguage(playerId));
        } finally {
            blockingLoad.release();
            executor.shutdownNow();
            preferences.shutdown();
        }
    }

    private static void assertBlocked(Future<?> future)
            throws InterruptedException, ExecutionException {
        TimeoutException timeout = assertThrows(TimeoutException.class, () -> future.get(200, TimeUnit.MILLISECONDS));
        assertNotNull(timeout);
    }

    private static String languagePath(UUID playerId) {
        return "players." + playerId + ".language";
    }

    private static final class FakeRuntime implements MythicRodRuntime {
        private final File dataFolder;
        private final FakePlatformServer platform;
        private final Logger logger = Logger.getLogger(FakeRuntime.class.getName());

        private FakeRuntime(File dataFolder, FakePlatformServer platform) {
            this.dataFolder = dataFolder;
            this.platform = platform;
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
            return platform;
        }

        @Override
        public PlatformConfiguration loadConfig(File file) {
            return platform.loadConfiguration(file);
        }

        @Override
        public PlatformConfiguration createEmptyConfig() {
            return platform.createEmptyConfiguration();
        }
    }

    private static final class FakePlatformServer implements PlatformServer {
        private final Logger logger = Logger.getLogger(FakePlatformServer.class.getName());
        private final Map<String, Map<String, Object>> persistedByFile = new ConcurrentHashMap<>();
        private volatile BlockingSave nextBlockingSave;
        private volatile BlockingLoad nextBlockingLoad;

        private BlockingSave blockNextSave() {
            BlockingSave blockingSave = new BlockingSave();
            nextBlockingSave = blockingSave;
            return blockingSave;
        }

        private BlockingLoad blockNextPlayersKeyRead() {
            BlockingLoad blockingLoad = new BlockingLoad();
            nextBlockingLoad = blockingLoad;
            return blockingLoad;
        }

        private void putPersistedValue(File file, String path, String value) {
            String filePath = file.getAbsolutePath();
            Map<String, Object> persistedValues = persistedByFile.get(filePath);
            if (persistedValues == null) {
                persistedValues = new ConcurrentHashMap<>();
                persistedByFile.put(filePath, persistedValues);
            }
            persistedValues.put(path, value);
        }

        private String getPersistedValue(File file, String path) {
            Object value = persistedByFile
                .getOrDefault(file.getAbsolutePath(), Collections.emptyMap())
                .get(path);
            return value instanceof String persisted ? persisted : null;
        }

        private Map<String, Object> snapshot(File file) {
            return new HashMap<>(persistedByFile.getOrDefault(file.getAbsolutePath(), Collections.emptyMap()));
        }

        private void persist(File file, Map<String, Object> values) throws InterruptedException {
            BlockingSave blockingSave = nextBlockingSave;
            if (blockingSave != null) {
                nextBlockingSave = null;
                blockingSave.markStarted();
                blockingSave.awaitRelease();
            }

            persistedByFile.put(file.getAbsolutePath(), new ConcurrentHashMap<>(values));
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public PlatformScheduler getScheduler() {
            return null;
        }

        @Override
        public PlatformPlayer getPlayer(UUID uuid) {
            return null;
        }

        @Override
        public PlatformCommandSender getCommandSender(String name) {
            return null;
        }

        @Override
        public boolean isEntityValid(UUID entityId) {
            return false;
        }

        @Override
        public boolean isNexoEnabled() {
            return false;
        }

        @Override
        public PlatformConfiguration loadConfiguration(File file) {
            BlockingLoad blockingLoad = nextBlockingLoad;
            nextBlockingLoad = null;
            return new FakePlatformConfiguration(this, file, snapshot(file), "", blockingLoad);
        }

        @Override
        public PlatformConfiguration loadConfiguration(java.io.InputStream stream) {
            throw new UnsupportedOperationException("Not needed for PlayerPreferences tests");
        }

        @Override
        public PlatformConfiguration createEmptyConfiguration() {
            return new FakePlatformConfiguration(this, null, new HashMap<>(), "", null);
        }

        @Override
        public PlatformWorld getWorld(String name) {
            return null;
        }

        @Override
        public PlatformItemFactory getItemFactory() {
            return null;
        }

        @Override
        public void dispatchCommandConsole(String command) {
            throw new UnsupportedOperationException("Not needed for PlayerPreferences tests");
        }

        @Override
        public void broadcastMessage(String message) {
            throw new UnsupportedOperationException("Not needed for PlayerPreferences tests");
        }
    }

    private static final class FakePlatformConfiguration implements PlatformConfiguration {
        private final FakePlatformServer server;
        private final File backingFile;
        private final Map<String, Object> values;
        private final String prefix;
        private final BlockingLoad blockingLoad;

        private FakePlatformConfiguration(FakePlatformServer server, File backingFile,
                                          Map<String, Object> values, String prefix,
                                          BlockingLoad blockingLoad) {
            this.server = server;
            this.backingFile = backingFile;
            this.values = values;
            this.prefix = prefix;
            this.blockingLoad = blockingLoad;
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
            if (Boolean.TRUE.equals(value)) {
                return true;
            }
            if (Boolean.FALSE.equals(value)) {
                return false;
            }
            return def;
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
            Object value = values.get(qualify(path));
            if (!(value instanceof List<?> rawList)) {
                return Collections.emptyList();
            }

            List<String> result = new ArrayList<>(rawList.size());
            for (Object entry : rawList) {
                if (entry instanceof String stringEntry) {
                    result.add(stringEntry);
                }
            }
            return result;
        }

        @Override
        public List<Map<?, ?>> getMapList(String path) {
            return Collections.emptyList();
        }

        @Override
        public void set(String path, Object value) {
            String absolutePath = qualify(path);
            if (value == null) {
                values.keySet().removeIf(key -> key.equals(absolutePath) || key.startsWith(absolutePath + "."));
                return;
            }
            values.put(absolutePath, value);
        }

        @Override
        public Set<String> getKeys(boolean deep) {
            if (blockingLoad != null && "players".equals(prefix)) {
                blockingLoad.markStarted();
                blockingLoad.awaitRelease();
            }

            Set<String> keys = new LinkedHashSet<>();
            String keyPrefix = prefix.isEmpty() ? "" : prefix + ".";

            for (String key : values.keySet()) {
                String relativeKey;
                if (prefix.isEmpty()) {
                    relativeKey = key;
                } else if (key.startsWith(keyPrefix)) {
                    relativeKey = key.substring(keyPrefix.length());
                } else {
                    continue;
                }

                if (relativeKey.isEmpty()) {
                    continue;
                }

                keys.add(deep ? relativeKey : firstSegment(relativeKey));
            }
            return keys;
        }

        @Override
        public PlatformConfiguration getSection(String path) {
            String absolutePath = qualify(path);
            String sectionPrefix = absolutePath + ".";
            boolean hasSectionEntries = values.keySet().stream().anyMatch(key -> key.startsWith(sectionPrefix));
            return hasSectionEntries
                ? new FakePlatformConfiguration(server, backingFile, values, absolutePath, blockingLoad)
                : null;
        }

        @Override
        public void save(File file) throws IOException {
            File targetFile = file != null ? file : backingFile;
            assertNotNull(targetFile, "A backing file is required for save()");
            try {
                server.persist(targetFile, values);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while persisting test preferences", e);
            }
        }

        private String qualify(String path) {
            if (path == null || path.isEmpty()) {
                return prefix;
            }
            if (prefix.isEmpty()) {
                return path;
            }
            return prefix + "." + path;
        }

        private static String firstSegment(String path) {
            int separatorIndex = path.indexOf('.');
            return separatorIndex >= 0 ? path.substring(0, separatorIndex) : path;
        }
    }

    private static final class BlockingSave {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private void markStarted() {
            started.countDown();
        }

        private boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
            return started.await(timeout, unit);
        }

        private void awaitRelease() throws InterruptedException {
            release.await();
        }

        private void release() {
            release.countDown();
        }
    }

    private static final class BlockingLoad {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private void markStarted() {
            started.countDown();
        }

        private boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
            return started.await(timeout, unit);
        }

        private void awaitRelease() {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while blocking PlayerPreferences reload", e);
            }
        }

        private void release() {
            release.countDown();
        }
    }
}
