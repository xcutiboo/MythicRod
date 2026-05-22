package io.xcutiboo.mythicrod.drops;

import java.util.List;
import java.util.Map;

/// Mutable-shaped view of the fields the GUI editor and admin command pass into
/// {@link DropManager#addDrop(String, EditableDropFields)} and
/// {@link DropManager#updateDrop(CustomDrop, String, EditableDropFields)}.
///
/// {@link DropConfigurationRecord} enforces invariants on the persistent shape
/// (non-null identifier, positive weight). This record represents the raw,
/// possibly-invalid input from a user interface; the manager validates and
/// normalizes it before constructing the persistent record.
public record EditableDropFields(
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
    List<String> biomes
) {}
