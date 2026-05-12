package io.xcutiboo.mythicrod.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import io.xcutiboo.mythicrod.internal.runtime.MythicRodRuntime;

public class PlayerPreferences {
    private final MythicRodRuntime runtime;
    private final File file;
    private final AtomicReference<Map<UUID, String>> languagePreferences = new AtomicReference<>(Map.of());
    private final ExecutorService saveExecutor;
    private final Object saveLock = new Object();
    private final AtomicBoolean saveRequested = new AtomicBoolean();
    private final AtomicBoolean saveWorkerScheduled = new AtomicBoolean();
    private io.xcutiboo.mythicrod.api.platform.PlatformConfiguration cfg;

    public PlayerPreferences(MythicRodRuntime runtime) {
        this.runtime = runtime;
        this.saveExecutor = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("MythicRod-PlayerPreferences-", 0).factory()
        );
        File dir = runtime.getDataFolder();
        this.file = new File(dir, "players.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                runtime.getLogger().log(Level.SEVERE, "Failed to create players.yml", e);
            }
        }
        this.cfg = runtime.getPlatform().loadConfiguration(file);
        languagePreferences.set(loadPreferences(cfg));
    }

    public String getLanguage(UUID playerId) {
        return languagePreferences.get().get(playerId);
    }

    public void setLanguage(UUID playerId, String locale) {
        while (true) {
            Map<UUID, String> currentPreferences = languagePreferences.get();
            Map<UUID, String> updatedPreferences = new HashMap<>(currentPreferences);
            if (locale == null || locale.isEmpty()) {
                updatedPreferences.remove(playerId);
            } else {
                updatedPreferences.put(playerId, locale);
            }

            if (languagePreferences.compareAndSet(currentPreferences, Map.copyOf(updatedPreferences))) {
                break;
            }
        }
        requestSave();
    }

    public void shutdown() {
        flushPendingSaves();
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            saveExecutor.shutdownNow();
        }
    }

    public void reloadFromDisk() {
        flushPendingSaves();
        synchronized (saveLock) {
            try {
                cfg = runtime.getPlatform().loadConfiguration(file);
                languagePreferences.set(loadPreferences(cfg));
            } catch (Exception e) {
                runtime.getLogger().log(Level.SEVERE, "Failed to reload players.yml", e);
            }
        }
    }

    private Map<UUID, String> loadPreferences(io.xcutiboo.mythicrod.api.platform.PlatformConfiguration sourceConfig) {
        Map<UUID, String> loadedPreferences = new HashMap<>();
        for (String playerId : sourceConfig.getKeys("players", false)) {
            try {
                UUID uuid = UUID.fromString(playerId);
                String language = sourceConfig.getString("players." + playerId + ".language", null);
                if (language != null && !language.isEmpty()) {
                    loadedPreferences.put(uuid, language);
                }
            } catch (IllegalArgumentException e) {
                runtime.getLogger().log(Level.WARNING,
                    "Ignoring invalid player preference entry for " + playerId, e);
            }
        }
        return loadedPreferences.isEmpty() ? Map.of() : Map.copyOf(loadedPreferences);
    }

    private void requestSave() {
        saveRequested.set(true);
        if (!saveWorkerScheduled.compareAndSet(false, true)) {
            return;
        }

        try {
            saveExecutor.execute(this::runSaveWorker);
        } catch (RejectedExecutionException e) {
            saveWorkerScheduled.set(false);
            flushPendingSaves();
        }
    }

    private void runSaveWorker() {
        try {
            flushPendingSaves();
        } finally {
            saveWorkerScheduled.set(false);
            if (saveRequested.get()) {
                requestSave();
            }
        }
    }

    private void flushPendingSaves() {
        while (saveRequested.getAndSet(false)) {
            saveNow();
        }
    }

    private void saveNow() {
        Map<UUID, String> preferencesSnapshot = languagePreferences.get();
        synchronized (saveLock) {
            try {
                cfg.set("players", null);
                for (Map.Entry<UUID, String> entry : preferencesSnapshot.entrySet()) {
                    cfg.set("players." + entry.getKey() + ".language", entry.getValue());
                }
                cfg.save(file);
            } catch (Exception e) {
                runtime.getLogger().log(Level.SEVERE, "Failed to save players.yml", e);
            }
        }
    }
}
