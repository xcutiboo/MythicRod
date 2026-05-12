package io.xcutiboo.mythicrod.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerStatsTest {

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
