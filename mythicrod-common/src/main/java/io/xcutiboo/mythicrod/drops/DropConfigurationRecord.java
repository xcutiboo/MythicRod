package io.xcutiboo.mythicrod.drops;

import java.util.List;
import java.util.Map;

public record DropConfigurationRecord(
    String identifier,
    int chance,
    int amount,
    String customName,
    List<String> lore,
    int customModelData,
    Map<String, Integer> enchantments,
    List<String> itemFlags,
    boolean glowing,
    String permission,
    List<String> biomes,
    String nexoItemId
) {
    public DropConfigurationRecord {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        if (chance <= 0) {
            throw new IllegalArgumentException("Chance must be greater than 0");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        lore = lore == null ? List.of() : List.copyOf(lore);
        enchantments = enchantments == null ? Map.of() : Map.copyOf(enchantments);
        itemFlags = itemFlags == null ? List.of() : List.copyOf(itemFlags);
        biomes = biomes == null ? List.of() : List.copyOf(biomes);
    }
}
