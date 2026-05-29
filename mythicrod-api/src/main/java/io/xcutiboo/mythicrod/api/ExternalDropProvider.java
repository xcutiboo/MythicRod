package io.xcutiboo.mythicrod.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

/// Extension point for plugins that add rewards to MythicRod's drop selection.
///
/// Register implementations with
/// `MythicRodAPI#registerExternalDropProvider(ExternalDropProvider)` after
/// MythicRod has enabled and exposed its service. Unregister the provider during
/// your own plugin disable phase.
///
/// ## Thread Safety
///
/// Provider methods may be called from a player/entity owner thread on Folia.
/// Implementations must be thread-safe, must not block, and should keep weight
/// checks cheap enough for the fishing event path.
///
/// Use `MythicRodAPI#getItemFactory()` or `MythicRodAPI#createItem(String, int)`
/// to construct MythicRod-compatible items instead of depending on Paper
/// implementation classes directly.
@ApiStatus.AvailableSince("2026.1.0")
public interface ExternalDropProvider {

    /// Returns the stable key for this provider.
    ///
    /// Use a namespace-style value such as `"myplugin:vip_reward"`. The key must
    /// not change across restarts because it is used for provider replacement and
    /// unregistering.
    ///
    /// @return non-empty unique key
    @NotNull
    String getKey();

    /// Returns this provider's relative roll weight for the current player.
    ///
    /// A weight of `0.0` or less means the provider will not be selected. Weight
    /// is relative to all other eligible providers and built-in drops.
    ///
    /// @param player read-only fishing player context
    /// @return weight `>= 0.0`; negative values are treated as zero
    double getWeight(@NotNull PlatformPlayer player);

    /// Builds the reward item after this provider wins the weighted roll.
    ///
    /// Return `null` to abort the reward quietly. MythicRod uses
    /// `getDisplayName()` and `getTier()` for chat, events, and statistics so
    /// external rewards feel native.
    ///
    /// @param player player who triggered the catch
    /// @return reward item, or `null` to skip delivery
    @Nullable
    PlatformItem generateItem(@NotNull PlatformPlayer player);

    /// Returns the MiniMessage display name shown in chat and GUI surfaces.
    ///
    /// @return MiniMessage-formatted display name
    @NotNull
    default String getDisplayName() {
        return "<gray>Unknown Drop</gray>";
    }

    /// Returns the rarity tier used for sorting, effects, and statistics.
    ///
    /// Use `"common"`, `"uncommon"`, `"rare"`, or `"legendary"` unless your
    /// integration has a deliberate custom tier.
    ///
    /// @return lowercase rarity tier
    @NotNull
    default String getTier() {
        return "common";
    }
}
