package io.xcutiboo.mythicrod.paper.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

class FishingListenerCatchMessageTest {

    @Test
    void resolveCatchItemNameComponentPrefersDeliveredItemDisplayName() {
        Component component = FishingListener.resolveCatchItemNameComponent(
            Component.text("Event Reward").color(NamedTextColor.GOLD),
            "<red><bold>Book of Sharpness</bold></red>",
            "ENCHANTED_BOOK"
        );

        assertEquals(
            Component.text("Event Reward").color(NamedTextColor.GOLD),
            component
        );
    }

    @Test
    void resolveCatchItemNameComponentPreservesMiniMessageFormatting() {
        Component component = FishingListener.resolveCatchItemNameComponent(
            "<red><bold>Book of Sharpness</bold></red>",
            "ENCHANTED_BOOK"
        );

        assertEquals(
            Component.text("Book of Sharpness")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true),
            component
        );
    }

    @Test
    void resolveCatchItemNameComponentMigratesLegacyFormattingCodes() {
        Component component = FishingListener.resolveCatchItemNameComponent(
            "&c&lBook of Sharpness",
            "ENCHANTED_BOOK"
        );

        assertEquals(
            Component.text("Book of Sharpness")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true),
            component
        );
    }

    @Test
    void resolveCatchItemNameComponentFallsBackToFormattedIdentifier() {
        Component component = FishingListener.resolveCatchItemNameComponent(null, "NETHERITE_INGOT");

        assertEquals(Component.text("Netherite Ingot"), component);
    }
}
