package io.xcutiboo.mythicrod.paper.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

class ItemBuilderFormattingTest {

    @Test
    void nameMigratesLegacyFormattingCodes() {
        assertEquals(
            Component.text("Legacy Blade")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true),
            ItemBuilder.deserializeConfiguredText("&c&lLegacy Blade")
        );
    }

    @Test
    void loreMigratesLegacyFormattingCodesAndDisablesItalic() {
        assertEquals(
            Component.text("Treasure from the deep")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false),
            ItemBuilder.deserializeConfiguredText("&eTreasure from the deep")
                .decoration(TextDecoration.ITALIC, false)
        );
    }

    @Test
    void invalidMiniMessageFallsBackToPlainText() {
        assertEquals(
            Component.text("<red"),
            ItemBuilder.deserializeConfiguredText("<red")
        );
    }
}
