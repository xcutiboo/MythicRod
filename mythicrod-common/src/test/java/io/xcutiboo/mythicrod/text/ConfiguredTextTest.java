package io.xcutiboo.mythicrod.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.Test;

class ConfiguredTextTest {

    @Test
    void parseReturnsEmptyForNullOrBlankInput() {
        assertEquals(Component.empty(), ConfiguredText.parse(null));
        assertEquals(Component.empty(), ConfiguredText.parse(""));
    }

    @Test
    void parseRendersMiniMessageTagsIntoNonEmptyComponent() {
        Component component = ConfiguredText.parse("<red>Treasure</red>");
        assertNotEquals(Component.empty(), component);
    }

    @Test
    void parseMigratesLegacyAmpersandSequences() {
        Component component = ConfiguredText.parse("&cTreasure");
        assertNotEquals(Component.empty(), component);
    }
}
