package io.xcutiboo.mythicrod.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerStatsTest {

    @Test
    void loadFromPersistedRestoresSnapshotFields() {
        PlayerStats stats = new PlayerStats(UUID.randomUUID(), "Xcutiboo");
        stats.loadFromPersisted(new PlayerStats.PersistedSnapshot(
            42, 30, 8, 3, 1, 25, 12, 5, 1_700_000_000_000L));

        assertEquals(42, stats.getTotalCaught());
        assertEquals(30, stats.getCommonCaught());
        assertEquals(8, stats.getUncommonCaught());
        assertEquals(3, stats.getRareCaught());
        assertEquals(1, stats.getLegendaryCaught());
        assertEquals(25, stats.getBasicRodUses());
        assertEquals(12, stats.getAdvancedRodUses());
        assertEquals(5, stats.getLegendaryRodUses());
        assertEquals(1_700_000_000_000L, stats.getLastFished());
    }

    @Test
    void tierCountsExposeRecordedRarityBreakdown() {
        PlayerStats stats = new PlayerStats(UUID.randomUUID(), "Xcutiboo");

        stats.incrementCommonCaught();
        stats.incrementCommonCaught();
        stats.incrementUncommonCaught();
        stats.incrementRareCaught();
        stats.incrementLegendaryCaught();

        Map<String, Integer> tierCounts = stats.getTierCounts();

        assertEquals(List.of("legendary", "rare", "uncommon", "common"), List.copyOf(tierCounts.keySet()));
        assertEquals(1, tierCounts.get("legendary"));
        assertEquals(1, tierCounts.get("rare"));
        assertEquals(1, tierCounts.get("uncommon"));
        assertEquals(2, tierCounts.get("common"));
        assertThrows(UnsupportedOperationException.class, () -> tierCounts.put("common", 99));
    }
}
