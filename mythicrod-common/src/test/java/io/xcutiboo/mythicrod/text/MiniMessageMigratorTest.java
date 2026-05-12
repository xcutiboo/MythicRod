package io.xcutiboo.mythicrod.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiniMessageMigratorTest {

    @Test
    void detectsLegacyFormattingAndHexColors() {
        assertTrue(MiniMessageMigrator.containsLegacyCodes("&cTreasure"));
        assertTrue(MiniMessageMigrator.containsLegacyCodes("&#AABBCCGem"));
        assertFalse(MiniMessageMigrator.containsLegacyCodes("<red>Treasure</red>"));
    }

    @Test
    void migratesAmpersandHexColorsWithoutSerializerLoss() {
        assertEquals(
            "<#aabbcc><bold>Gem",
            MiniMessageMigrator.migrateWithSerializer("&#AABBCC&lGem")
        );
    }
}
