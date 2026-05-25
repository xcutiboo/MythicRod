package io.xcutiboo.mythicrod.api.platform;

import java.util.List;

/// Read-only drop descriptor exposed through `DropCatalog`.
///
/// This describes a loaded reward configuration entry, not necessarily a fully
/// materialized item instance. Integrations should treat instances as immutable
/// snapshots of the current drop table.
public interface PlatformDrop {

    /// Returns the stable identifier used by MythicRod's item factory and config.
    ///
    /// @return stable material or integration identifier
    String getIdentifier();

    /// Returns the configured relative roll weight for this drop.
    ///
    /// The value is compared against other eligible drops in the same roll; it
    /// is not a normalized percentage.
    ///
    /// @return configured relative roll weight
    int getWeight();

    /// Returns the configured reward amount.
    ///
    /// @return configured stack amount
    int getAmount();

    /// Returns MythicRod's rarity tier label for this drop.
    ///
    /// The default implementation follows the same weight-to-tier mapping used
    /// by MythicRod's built-in drop descriptors and statistics pipeline.
    ///
    /// @return one of `common`, `uncommon`, `rare`, or `legendary`
    default String getTier() {
        int weight = getWeight();
        if (weight <= 1) {
            return "legendary";
        }
        if (weight <= 5) {
            return "rare";
        }
        if (weight <= 15) {
            return "uncommon";
        }
        return "common";
    }

    /// Returns `true` when this drop points at a Nexo-backed item identifier.
    ///
    /// @return `true` when the identifier targets a Nexo-backed item
    boolean isNexoItem();

    /// Returns the permission node required for eligibility, or `null` when unrestricted.
    ///
    /// @return required permission node, or `null` when unrestricted
    String getPermission();

    /// Returns an immutable snapshot of biome constraints, or an empty list when global.
    ///
    /// @return immutable biome filter snapshot
    List<String> getBiomes();

    /// Attempts to materialize the drop as an item.
    ///
    /// Some catalog entries are configuration descriptors only and may reject
    /// direct materialization. External plugins that need item creation should
    /// prefer `MythicRodAPI#getItemFactory()`.
    ///
    /// @return Materialized platform item.
    /// @throws UnsupportedOperationException when the descriptor cannot create an item directly.
    PlatformItem createItem();
}
