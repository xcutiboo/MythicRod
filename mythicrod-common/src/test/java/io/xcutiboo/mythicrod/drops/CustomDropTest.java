package io.xcutiboo.mythicrod.drops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CustomDropTest {

    @Test
    void tierThresholdsMatchWeightBuckets() {
        assertEquals("legendary", drop(1).getTier());
        assertEquals("rare", drop(5).getTier());
        assertEquals("uncommon", drop(15).getTier());
        assertEquals("common", drop(20).getTier());
    }

    @Test
    void nexoFlagFollowsIdentifierPrefix() {
        CustomDrop nexo = CustomDrop.createNexoDrop("ancient_pearl", 25, 1);
        assertTrue(nexo.isNexoItem());
        assertEquals("ancient_pearl", nexo.getNexoItemId());

        CustomDrop vanilla = new CustomDrop(new DropConfigurationRecord(
            "DIAMOND", 10, 1, null, List.of(), 0,
            Map.of(), List.of(), false, null, List.of(), null));
        assertFalse(vanilla.isNexoItem());
    }

    @Test
    void createItemThrowsBecauseCustomDropHasNoPlatformItemFactory() {
        CustomDrop drop = drop(10);
        assertThrows(UnsupportedOperationException.class, drop::createItem);
    }

    private static CustomDrop drop(int weight) {
        return new CustomDrop(new DropConfigurationRecord(
            "DIAMOND", weight, 1, null, List.of(), 0,
            Map.of(), List.of(), false, null, List.of(), null));
    }
}
