package io.xcutiboo.mythicrod.drops;

import java.util.List;
import java.util.Map;

/// Immutable reward configuration after defaults and config aliases are applied.
///
/// `weight` is relative to the other eligible drops in the same roll. It is not
/// a normalized percentage.
public record DropConfigurationRecord(
    String identifier,
    int weight,
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
        if (weight <= 0) {
            throw new IllegalArgumentException("Drop weight must be greater than 0");
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
