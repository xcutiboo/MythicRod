package io.xcutiboo.mythicrod.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void migrateHandlesNullAndEmptyAndNonLegacyInputs() {
        assertNull(MiniMessageMigrator.migrate(null));
        assertEquals("", MiniMessageMigrator.migrate(""));
        assertEquals("<red>Treasure</red>", MiniMessageMigrator.migrate("<red>Treasure</red>"));
    }

    @Test
    void migrateConvertsLegacyColorAndFormatCodes() {
        assertEquals("<red>Hi <bold>there", MiniMessageMigrator.migrate("&cHi &lthere"));
        assertEquals("<obfuscated>X", MiniMessageMigrator.migrate("&kX"));
        assertEquals("<reset>plain", MiniMessageMigrator.migrate("&rplain"));
    }

    @Test
    void migrateWithSerializerPassesThroughWhenNoLegacyCodesPresent() {
        assertEquals("<gold>Plain", MiniMessageMigrator.migrateWithSerializer("<gold>Plain"));
        assertNull(MiniMessageMigrator.migrateWithSerializer(null));
    }
}
