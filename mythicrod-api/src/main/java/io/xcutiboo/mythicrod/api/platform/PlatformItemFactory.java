package io.xcutiboo.mythicrod.api.platform;

import io.xcutiboo.mythicrod.api.Result;
import org.jetbrains.annotations.NotNull;

/// Creates MythicRod-compatible platform items from stable identifiers.
///
/// External integrations should prefer this factory over constructing Paper
/// implementation classes directly. That keeps plugins compatible with
/// MythicRod's supported custom-item integrations and future platform adapters.
public interface PlatformItemFactory {

    /// Attempts to create an item for the supplied identifier.
    ///
    /// @param identifier platform identifier such as a Bukkit material name or
    ///                   integration-backed custom item id
    /// @param amount requested amount
    /// @return success/failure result describing the creation outcome
    @NotNull
    Result<@NotNull PlatformItem> createItem(@NotNull String identifier, int amount);

    /// Checks whether this factory understands the supplied identifier.
    ///
    /// @param identifier platform identifier to probe
    /// @return `true` if `createItem(String, int)` can service it
    boolean canCreate(@NotNull String identifier);
}
