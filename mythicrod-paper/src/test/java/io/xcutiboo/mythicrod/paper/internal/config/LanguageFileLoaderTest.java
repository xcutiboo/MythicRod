package io.xcutiboo.mythicrod.paper.internal.config;

import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class LanguageFileLoaderTest {

    @Test
    void mergeTranslationsRefreshesKnownStaleDiskValuesAndMutatesOverrides() {
        YamlConfiguration bundledDefaults = new YamlConfiguration();
        bundledDefaults.set("gui.main.title", "<gold><bold>MythicRod</bold> <dark_gray>• <aqua>Hub");

        YamlConfiguration diskOverrides = new YamlConfiguration();
        diskOverrides.set("gui.main.title", "<black>✦ MythicRod");

        LanguageFileLoader.MergeResult mergeResult = LanguageFileLoader.mergeTranslations(
            bundledDefaults,
            diskOverrides
        );

        assertEquals(1, mergeResult.refreshedDiskValues());
        assertEquals(
            "<gold><bold>MythicRod</bold> <dark_gray>• <aqua>Hub",
            mergeResult.translations().get("gui.main.title")
        );
        assertEquals(
            "<gold><bold>MythicRod</bold> <dark_gray>• <aqua>Hub",
            diskOverrides.getString("gui.main.title")
        );
    }

    @Test
    void mergeTranslationsPreservesCustomDiskOverrides() {
        YamlConfiguration bundledDefaults = new YamlConfiguration();
        bundledDefaults.set("gui.main.title", "<gold><bold>MythicRod</bold> <dark_gray>• <aqua>Hub");

        YamlConfiguration diskOverrides = new YamlConfiguration();
        diskOverrides.set("gui.main.title", "<gold><bold>Custom Fishing Hub</bold>");

        LanguageFileLoader.MergeResult mergeResult = LanguageFileLoader.mergeTranslations(
            bundledDefaults,
            diskOverrides
        );

        assertEquals(0, mergeResult.refreshedDiskValues());
        assertEquals(
            "<gold><bold>Custom Fishing Hub</bold>",
            mergeResult.translations().get("gui.main.title")
        );
    }

    @Test
    void flattenYamlJoinsStringListsIntoSingleTranslationValue() {
        YamlConfiguration bundledDefaults = new YamlConfiguration();
        bundledDefaults.set("gui.lore.lines", List.of("<gray>First", "<yellow>Second"));

        LanguageFileLoader.MergeResult mergeResult = LanguageFileLoader.mergeTranslations(bundledDefaults, null);

        assertEquals("<gray>First\n<yellow>Second", mergeResult.translations().get("gui.lore.lines"));
    }
}
