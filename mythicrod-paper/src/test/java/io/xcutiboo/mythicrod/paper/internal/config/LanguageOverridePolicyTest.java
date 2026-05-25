package io.xcutiboo.mythicrod.paper.internal.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LanguageOverridePolicyTest {

    @Test
    void refreshesKnownShippedBlackGuiTitleDefaults() {
        assertFalse(LanguageOverridePolicy.shouldUseDiskOverride(
            "gui.main.title",
            "<black>✦ MythicRod",
            "<gold><bold>MythicRod</bold> <dark_gray>• <aqua>Hub"
        ));
        assertTrue(LanguageOverridePolicy.shouldRefreshDiskValue(
            "gui.main.title",
            "<black>✦ MythicRod"
        ));
        assertTrue(LanguageOverridePolicy.replacementForDiskValue(
            "gui.main.title",
            "<black>✦ MythicRod",
            "<gold><bold>MythicRod</bold> <dark_gray>• <aqua>Hub"
        ).isPresent());
    }

    @Test
    void preservesCustomBlackTitlesThatWereNotShippedDefaults() {
        assertTrue(LanguageOverridePolicy.shouldUseDiskOverride(
            "gui.main.title",
            "<black>Custom Server Fishing",
            "<gold><bold>MythicRod</bold> <dark_gray>• <aqua>Hub"
        ));
        assertFalse(LanguageOverridePolicy.shouldRefreshDiskValue(
            "gui.main.title",
            "<black>Custom Server Fishing"
        ));
    }

    @Test
    void refreshesKnownStaleDropCommandText() {
        assertFalse(LanguageOverridePolicy.shouldUseDiskOverride(
            "drops.category-header",
            "<gold><st>══</st><bold> Drops: <yellow>%category% </bold><gold><st>══</st>",
            "<gold><st>══</st><bold> Drops: <yellow>%label% </bold><gold><st>══</st>"
        ));
        assertTrue(LanguageOverridePolicy.replacementForDiskValue(
            "drops.category-entry",
            "<yellow>  %category%<gray>: <white>%count% drops",
            "<yellow>  %label% <dark_gray>(%category%)<gray>: <white>%count% drops"
        ).orElseThrow().contains("%label%"));
    }

    @Test
    void refreshesKnownWeightTextWithoutOverwritingLocale() {
        assertFalse(LanguageOverridePolicy.shouldUseDiskOverride(
            "drops.drop-entry",
            "<dark_gray>  • <white>%name% <gray>(<yellow>%chance%%<gray> · <yellow>×%amount%<gray>)",
            "<dark_gray>  • <white>%name% <gray>(ウェイト <yellow>%weight%<gray> · <yellow>×%amount%<gray>)"
        ));
        assertTrue(LanguageOverridePolicy.replacementForDiskValue(
            "drops.drop-entry",
            "<dark_gray>  • <white>%name% <gray>(<yellow>%chance%%<gray> · <yellow>×%amount%<gray>)",
            "<dark_gray>  • <white>%name% <gray>(ウェイト <yellow>%weight%<gray> · <yellow>×%amount%<gray>)"
        ).orElseThrow().contains("%weight%"));
        assertTrue(LanguageOverridePolicy.replacementForDiskValue(
            "drops.drop-entry",
            "<dark_gray>  • <white>%name% <gray>(weight <yellow>%chance%<gray> · <yellow>×%amount%<gray>)",
            "<dark_gray>  • <white>%name% <gray>(weight <yellow>%weight%<gray> · <yellow>×%amount%<gray>)"
        ).orElseThrow().contains("%weight%"));
    }

    @Test
    void refreshesKnownDropEditorControlTextAfterExactInputChange() {
        assertFalse(LanguageOverridePolicy.shouldUseDiskOverride(
            "gui.edit_drop.weight.left_click",
            "<yellow>  L-Click: <gray>+1",
            "<yellow>  L-Click: <gray>type exact value"
        ));
        assertTrue(LanguageOverridePolicy.replacementForDiskValue(
            "gui.edit_drop.amount.right_click",
            "<yellow>  R-Click: <gray>-1",
            "<yellow>  R-Click: <gray>+1"
        ).orElseThrow().contains("+1"));
    }

    @Test
    void ignoresRemovedAliasKeys() {
        assertFalse(LanguageOverridePolicy.shouldUseDiskOverride(
            "gui.main_hub.title",
            "<black>✦ MythicRod",
            null
        ));
    }

    @Test
    void preservesOldValuesOutsideRefreshableTitleKeys() {
        assertTrue(LanguageOverridePolicy.shouldUseDiskOverride(
            "gui.main.config.name",
            "<black>✦ MythicRod",
            "<gold><bold>Configuration"
        ));
    }

    @Test
    void preservesMatchingBundledValues() {
        assertTrue(LanguageOverridePolicy.shouldUseDiskOverride(
            "gui.rod.title",
            "<gold><bold>MythicRod</bold> <dark_gray>• <light_purple>Rod",
            "<gold><bold>MythicRod</bold> <dark_gray>• <light_purple>Rod"
        ));
    }
}
