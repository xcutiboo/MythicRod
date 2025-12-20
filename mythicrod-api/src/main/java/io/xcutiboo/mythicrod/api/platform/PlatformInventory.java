package io.xcutiboo.mythicrod.api.platform;

import java.util.Map;

/// Read/write inventory view used by MythicRod's public API.
///
/// This is intentionally smaller than Bukkit's inventory surface so external
/// providers can interact with player storage without binding themselves to a
/// specific server implementation.
public interface PlatformInventory {

    /// Returns the slot count in this inventory.
    ///
    /// @return slot count in this inventory
    int getSize();

    /// Returns the title for UI-backed inventories when one exists.
    ///
    /// @return human-readable title for UI-backed inventories when available
    String getTitle();

    /// Returns whether the inventory can accept any more items.
    ///
    /// @return `true` when no additional item can be inserted without overflow
    boolean isFull();

    /// Attempts to add an item to the inventory.
    ///
    /// @param item item to insert
    /// @return immutable overflow snapshot using slot indices for entries that could not be inserted
    Map<Integer, PlatformItem> addItem(PlatformItem item);

    /// Returns the item currently stored in the requested slot.
    ///
    /// @param slot zero-based inventory slot
    /// @return item in that slot, or `null` when empty
    PlatformItem getItem(int slot);
}
