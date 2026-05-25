package io.xcutiboo.mythicrod.drops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DropConfigurationRecordTest {

    @Test
    void rejectsNullOrBlankIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> build(null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> build("", 1, 1));
    }

    @Test
    void rejectsNonPositiveWeightAndAmount() {
        assertThrows(IllegalArgumentException.class, () -> build("DIAMOND", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> build("DIAMOND", 1, 0));
    }

    @Test
    void nullCollectionsBecomeImmutableEmpties() {
        DropConfigurationRecord cfg =new DropConfigurationRecord(
            "DIAMOND", 10, 1, null, null, 0, null, null, false, null, null, null);
        assertEquals(List.of(), cfg.lore());
        assertEquals(Map.of(), cfg.enchantments());
        assertEquals(List.of(), cfg.itemFlags());
        assertEquals(List.of(), cfg.biomes());
    }

    @Test
    void collectionsAreDefensivelyCopied() {
        List<String> lore = new ArrayList<>();
        lore.add("Original");
        Map<String, Integer> enchants = new HashMap<>();
        enchants.put("minecraft:unbreaking", 1);

        DropConfigurationRecord cfg =new DropConfigurationRecord(
            "DIAMOND", 10, 1, null, lore, 0, enchants, null, false, null, null, null);

        lore.add("After construction");
        enchants.put("minecraft:fortune", 1);

        assertEquals(1, cfg.lore().size());
        assertEquals(1, cfg.enchantments().size());
    }

    @Test
    void identifierAndPrimitiveFieldsReflectInput() {
        DropConfigurationRecord cfg =build("DIAMOND", 5, 2);
        assertSame("DIAMOND", cfg.identifier());
        assertEquals(5, cfg.weight());
        assertEquals(2, cfg.amount());
    }

    private static DropConfigurationRecord build(String identifier, int weight, int amount) {
        return new DropConfigurationRecord(
            identifier, weight, amount, null, List.of(), 0,
            Map.of(), List.of(), false, null, List.of(), null);
    }
}
