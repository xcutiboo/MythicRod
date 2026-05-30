package io.xcutiboo.mythicrod.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/// Immutable snapshot of a player's MythicRod fishing statistics.
///
/// Returned by `MythicRodAPI#getPlayerStats(UUID)` and
/// `MythicRodAPI#getTopPlayers(StatType, int)`.
///
/// Snapshots represent a point-in-time read. Fetch a new snapshot after more
/// fishing activity if fresh values matter.
///
/// @param playerUuid UUID of the player
/// @param playerName last-known display name of the player
/// @param totalCaught total number of custom drops caught
/// @param commonCaught number of common tier drops caught
/// @param uncommonCaught number of uncommon tier drops caught
/// @param rareCaught number of rare tier drops caught
/// @param legendaryCaught number of legendary tier drops caught
/// @param basicRodUses number of casts made with a basic rod
/// @param advancedRodUses number of casts made with an advanced rod
/// @param legendaryRodUses number of casts made with a legendary rod
/// @param mythicRodUses number of casts made with a mythic rod
/// @param lastFished timestamp of the player's most recent catch, or `Instant.EPOCH`
///                   if they have never fished
/// @param snapshotTime when this snapshot was taken
@ApiStatus.AvailableSince("2026.1.0")
public record PlayerStatSnapshot(
        @NotNull UUID playerUuid,
        @NotNull String playerName,
        int totalCaught,
        int commonCaught,
        int uncommonCaught,
        int rareCaught,
        int legendaryCaught,
        int basicRodUses,
        int advancedRodUses,
        int legendaryRodUses,
        int mythicRodUses,
        @NotNull Instant lastFished,
        @NotNull Instant snapshotTime
) {

    /// Creates a validated immutable stats snapshot.
    ///
    /// @throws NullPointerException if a required reference field is null
    /// @throws IllegalArgumentException if any count or rod-usage value is negative
    public PlayerStatSnapshot {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(lastFished, "lastFished");
        Objects.requireNonNull(snapshotTime, "snapshotTime");

        requireNonNegative("totalCaught", totalCaught);
        requireNonNegative("commonCaught", commonCaught);
        requireNonNegative("uncommonCaught", uncommonCaught);
        requireNonNegative("rareCaught", rareCaught);
        requireNonNegative("legendaryCaught", legendaryCaught);
        requireNonNegative("basicRodUses", basicRodUses);
        requireNonNegative("advancedRodUses", advancedRodUses);
        requireNonNegative("legendaryRodUses", legendaryRodUses);
        requireNonNegative("mythicRodUses", mythicRodUses);
    }

    private static void requireNonNegative(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }

    /// Creates a zeroed snapshot for a player with no recorded MythicRod activity.
    ///
    /// @param playerUuid UUID of the player
    /// @param playerName last-known player name
    /// @return zeroed snapshot for an unseen player
    @NotNull
    public static PlayerStatSnapshot empty(@NotNull UUID playerUuid, @NotNull String playerName) {
        return new PlayerStatSnapshot(
                playerUuid, playerName,
                0, 0, 0, 0, 0,
                0, 0, 0, 0,
                Instant.EPOCH,
                Instant.now()
        );
    }

    /// Sort criteria supported by `MythicRodAPI#getTopPlayers(StatType, int)`.
    public enum StatType {
        /// Sort by `PlayerStatSnapshot#totalCaught()`.
        TOTAL_CAUGHT,
        /// Sort by `PlayerStatSnapshot#rareCaught()`.
        RARE_CAUGHT,
        /// Sort by `PlayerStatSnapshot#legendaryCaught()`.
        LEGENDARY_CAUGHT,
        /// Sort by `PlayerStatSnapshot#lastFished()` descending.
        LAST_FISHED
    }
}
