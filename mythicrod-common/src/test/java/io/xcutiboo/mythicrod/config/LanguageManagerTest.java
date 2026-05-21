package io.xcutiboo.mythicrod.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.CleanupMode;

import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.internal.runtime.MythicRodRuntime;
import net.kyori.adventure.text.Component;

import java.nio.file.Path;

class LanguageManagerTest {

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private Path dataFolder;

    @Test
    void unknownKeyFallsBackToKeyItself() {
        LanguageManager lang = newManager();
        assertEquals("not.a.real.key", lang.tr("not.a.real.key"));
    }

    @Test
    void translationLookupHitsCurrentLanguageBeforeFallback() {
        LanguageManager lang = newManager();
        lang.loadTranslations("en_US", Map.of("hello", "Hi"));
        lang.loadTranslations("ja_JP", Map.of("hello", "やあ"));

        lang.setLanguage("ja_JP");

        assertEquals("やあ", lang.tr("hello"));
    }

    @Test
    void missingTranslationFallsBackToEnglish() {
        LanguageManager lang = newManager();
        lang.loadTranslations("en_US", Map.of("hello", "Hi"));
        lang.loadTranslations("ja_JP", Map.of());

        lang.setLanguage("ja_JP");

        assertEquals("Hi", lang.tr("hello"));
    }

    @Test
    void placeholdersAreReplacedInBothCurlyAndPercentForms() {
        LanguageManager lang = newManager();
        lang.loadTranslations("en_US", Map.of(
            "catch", "Caught {item} x{amount}, {percent}%"));
        lang.setLanguage("en_US");

        Map<String, String> values = new HashMap<>();
        values.put("item", "Pearl");
        values.put("amount", "3");
        values.put("percent", "12");

        assertEquals("Caught Pearl x3, 12%", lang.tr("catch", values));
    }

    @Test
    void trInLanguageLooksUpInRequestedLocaleBeforeFallback() {
        LanguageManager lang = newManager();
        lang.loadTranslations("en_US", Map.of("hello", "Hi"));
        lang.loadTranslations("fr_FR", Map.of("hello", "Salut"));

        assertEquals("Salut", lang.trInLanguage("hello", "fr_FR"));
        assertEquals("Hi", lang.trInLanguage("hello", "es_ES"));
    }

    @Test
    void perPlayerLocalePreferenceOverridesGlobalLanguage() {
        LanguageManager lang = newManager();
        lang.loadTranslations("en_US", Map.of("hello", "Hi"));
        lang.loadTranslations("ja_JP", Map.of("hello", "やあ"));
        UUID player = UUID.randomUUID();
        lang.setPlayerLanguage(player, "ja_JP");

        assertEquals("やあ", lang.trForPlayer(player, "hello"));
    }

    @Test
    void resetTranslationsClearsAllStoredEntries() {
        LanguageManager lang = newManager();
        lang.loadTranslations("en_US", Map.of("hello", "Hi"));
        lang.resetTranslations();

        assertEquals("hello", lang.tr("hello"));
    }

    @Test
    void trComponentReturnsRenderableComponent() {
        LanguageManager lang = newManager();
        lang.loadTranslations("en_US", Map.of("greet", "<red>Hello"));
        lang.setLanguage("en_US");

        Component component = lang.trComponent("greet");
        assertNotNull(component);
        assertNotEquals(Component.empty(), component);
    }

    private LanguageManager newManager() {
        FakeRuntime runtime = new FakeRuntime(dataFolder.toFile());
        ConfigManager config = new ConfigManager(runtime, new EmptyConfig());
        return new LanguageManager(runtime, config);
    }

    private static final class FakeRuntime implements MythicRodRuntime {
        private final File dataFolder;
        private final Logger logger = Logger.getLogger(FakeRuntime.class.getName());
        private final PlatformServer server = new FakeServer();

        FakeRuntime(File dataFolder) { this.dataFolder = dataFolder; }
        @Override public Logger getLogger() { return logger; }
        @Override public File getDataFolder() { return dataFolder; }
        @Override public PlatformServer getPlatform() { return server; }
        @Override public PlatformConfiguration loadConfig(File file) { return new EmptyConfig(); }
        @Override public PlatformConfiguration createEmptyConfig() { return new EmptyConfig(); }
    }

    private static final class FakeServer implements PlatformServer {
        @Override public Logger getLogger() { return Logger.getLogger(FakeServer.class.getName()); }
        @Override public io.xcutiboo.mythicrod.api.platform.PlatformScheduler getScheduler() { return null; }
        @Override public io.xcutiboo.mythicrod.api.platform.PlatformPlayer getPlayer(UUID uuid) { return null; }
        @Override public io.xcutiboo.mythicrod.api.platform.PlatformCommandSender getCommandSender(String name) { return null; }
        @Override public boolean isEntityValid(UUID entityId) { return false; }
        @Override public boolean isNexoEnabled() { return false; }
        @Override public PlatformConfiguration loadConfiguration(File file) { return new EmptyConfig(); }
        @Override public PlatformConfiguration loadConfiguration(java.io.InputStream stream) { return new EmptyConfig(); }
        @Override public PlatformConfiguration createEmptyConfiguration() { return new EmptyConfig(); }
        @Override public io.xcutiboo.mythicrod.api.platform.PlatformWorld getWorld(String name) { return null; }
        @Override public io.xcutiboo.mythicrod.api.platform.PlatformItemFactory getItemFactory() { return null; }
        @Override public void dispatchCommandConsole(String command) {
            // test stub
        }
        @Override public void broadcastMessage(String message) {
            // test stub
        }
    }

    private static final class EmptyConfig implements PlatformConfiguration {
        @Override public boolean contains(String path) { return false; }
        @Override public String getString(String path) { return null; }
        @Override public String getString(String path, String def) { return def; }
        @Override public int getInt(String path) { return 0; }
        @Override public int getInt(String path, int def) { return def; }
        @Override public boolean getBoolean(String path) { return false; }
        @Override public boolean getBoolean(String path, boolean def) { return def; }
        @Override public double getDouble(String path) { return 0.0D; }
        @Override public double getDouble(String path, double def) { return def; }
        @Override public java.util.List<String> getStringList(String path) { return java.util.List.of(); }
        @Override public java.util.List<java.util.Map<?, ?>> getMapList(String path) { return java.util.List.of(); }
        @Override public PlatformConfiguration getSection(String path) { return null; }
        @Override public void set(String path, Object value) {
            // test stub: write-through not exercised
        }
        @Override public Set<String> getKeys(boolean deep) { return Set.of(); }
        @Override public Set<String> getKeys(String path, boolean deep) { return Set.of(); }
        @Override public void save(File file) throws IOException {
            // test stub: persistence not exercised
        }
    }
}
