package io.xcutiboo.mythicrod.api.drop;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import io.xcutiboo.mythicrod.api.platform.PlatformDrop;

/// Read-only view of MythicRod's currently loaded drop table.
///
/// The catalog reflects the plugin's active in-memory configuration. Returned
/// collections are snapshots intended for inspection, not mutation.
@ApiStatus.AvailableSince("2026.1.0")
public interface DropCatalog {

    /// Returns all currently registered category keys.
    ///
    /// @return immutable snapshot of loaded category names
    @NotNull
    Set<String> getCategories();

    /// Returns the currently loaded drops for a category.
    ///
    /// @param category category key as reported by `getCategories()`
    /// @return immutable snapshot of drops for that category, or an empty list
    @NotNull
    @SuppressWarnings("java:S1452")
    // The wildcard lets MythicRod return its concrete CustomDrop list without
    // forcing a defensive copy on every catalog query.
    List<? extends PlatformDrop> getDrops(@NotNull String category);

    /// Returns every currently loaded drop across all categories.
    ///
    /// @return immutable snapshot of all loaded drops
    @NotNull
    @SuppressWarnings("java:S1452")
    List<? extends PlatformDrop> getAllDrops();

    /// Returns the number of currently loaded drops.
    ///
    /// @return total drop count across all categories
    int getTotalDropCount();
}
