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
        DropConfigurationRecord record = new DropConfigurationRecord(
            "DIAMOND", 10, 1, null, null, 0, null, null, false, null, null, null);
        assertEquals(List.of(), record.lore());
        assertEquals(Map.of(), record.enchantments());
        assertEquals(List.of(), record.itemFlags());
        assertEquals(List.of(), record.biomes());
    }

    @Test
    void collectionsAreDefensivelyCopied() {
        List<String> lore = new ArrayList<>();
        lore.add("Original");
        Map<String, Integer> enchants = new HashMap<>();
        enchants.put("minecraft:unbreaking", 1);

        DropConfigurationRecord record = new DropConfigurationRecord(
            "DIAMOND", 10, 1, null, lore, 0, enchants, null, false, null, null, null);

        lore.add("After construction");
        enchants.put("minecraft:fortune", 1);

        assertEquals(1, record.lore().size());
        assertEquals(1, record.enchantments().size());
    }

    @Test
    void identifierAndPrimitiveFieldsReflectInput() {
        DropConfigurationRecord record = build("DIAMOND", 5, 2);
        assertSame("DIAMOND", record.identifier());
        assertEquals(5, record.weight());
        assertEquals(2, record.amount());
    }

    private static DropConfigurationRecord build(String identifier, int weight, int amount) {
        return new DropConfigurationRecord(
            identifier, weight, amount, null, List.of(), 0,
            Map.of(), List.of(), false, null, List.of(), null);
    }
}
