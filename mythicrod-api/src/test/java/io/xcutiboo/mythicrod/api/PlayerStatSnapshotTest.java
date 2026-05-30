package io.xcutiboo.mythicrod.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerStatSnapshotTest {

    private static final UUID UID = UUID.randomUUID();
    private static final Instant T = Instant.EPOCH;

    @Test
    void rejectsNullReferenceArgs() {
        assertThrows(NullPointerException.class, () -> new PlayerStatSnapshot(
            null, "name", 0, 0, 0, 0, 0, 0, 0, 0, 0, T, T));
        assertThrows(NullPointerException.class, () -> new PlayerStatSnapshot(
            UID, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, T, T));
        assertThrows(NullPointerException.class, () -> new PlayerStatSnapshot(
            UID, "name", 0, 0, 0, 0, 0, 0, 0, 0, 0, null, T));
        assertThrows(NullPointerException.class, () -> new PlayerStatSnapshot(
            UID, "name", 0, 0, 0, 0, 0, 0, 0, 0, 0, T, null));
    }

    @Test
    void rejectsNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerStatSnapshot(
            UID, "name", -1, 0, 0, 0, 0, 0, 0, 0, 0, T, T));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStatSnapshot(
            UID, "name", 0, 0, 0, 0, 0, 0, -1, 0, 0, T, T));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStatSnapshot(
            UID, "name", 0, 0, 0, 0, 0, 0, 0, 0, -1, T, T));
    }

    @Test
    void emptyReturnsZeroedSnapshotForPlayer() {
        PlayerStatSnapshot snapshot = PlayerStatSnapshot.empty(UID, "Daisy");
        assertEquals("Daisy", snapshot.playerName());
        assertEquals(0, snapshot.totalCaught());
        assertEquals(0, snapshot.legendaryRodUses());
        assertEquals(0, snapshot.mythicRodUses());
        assertEquals(Instant.EPOCH, snapshot.lastFished());
    }

    @Test
    void statTypeEnumExposesFourSortCriteria() {
        assertEquals(4, PlayerStatSnapshot.StatType.values().length);
        assertEquals(PlayerStatSnapshot.StatType.TOTAL_CAUGHT,
            PlayerStatSnapshot.StatType.valueOf("TOTAL_CAUGHT"));
    }

    @Test
    void exposesAllFieldsViaCanonicalAccessors() {
        PlayerStatSnapshot snapshot = new PlayerStatSnapshot(
            UID, "Xcutiboo", 100, 70, 20, 8, 2, 50, 30, 20, 5, T, T);
        assertEquals(UID, snapshot.playerUuid());
        assertEquals("Xcutiboo", snapshot.playerName());
        assertEquals(100, snapshot.totalCaught());
        assertEquals(70, snapshot.commonCaught());
        assertEquals(20, snapshot.uncommonCaught());
        assertEquals(8, snapshot.rareCaught());
        assertEquals(2, snapshot.legendaryCaught());
        assertEquals(50, snapshot.basicRodUses());
        assertEquals(30, snapshot.advancedRodUses());
        assertEquals(20, snapshot.legendaryRodUses());
        assertEquals(5, snapshot.mythicRodUses());
    }
}
