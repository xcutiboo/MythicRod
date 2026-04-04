package io.xcutiboo.mythicrod.api;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of a player's MythicRod fishing statistics.
 *
 * <p>Returned by {@link MythicRodAPI#getPlayerStats(UUID)} and
 * {@link MythicRodAPI#getTopPlayers(StatType, int)}.
 *
 * <p>This is a value record — all fields are final and immutable.
 * Snapshots represent a point-in-time read; subsequent fishing activity
 * will not be reflected unless a new snapshot is fetched.
 *
 * @param playerUuid       The UUID of the player.
 * @param playerName       The last-known display name of the player.
 * @param totalCaught      Total number of custom drops caught.
 * @param commonCaught     Number of common tier drops caught.
 * @param uncommonCaught   Number of uncommon tier drops caught.
 * @param rareCaught       Number of rare tier drops caught.
 * @param legendaryCaught  Number of legendary tier drops caught.
 * @param basicRodUses     Number of casts made with a basic rod.
 * @param advancedRodUses  Number of casts made with an advanced rod.
 * @param legendaryRodUses Number of casts made with a legendary rod.
 * @param lastFished       Timestamp of the player's most recent catch, or
 *                         {@link Instant#EPOCH} if they have never fished.
 * @param snapshotTime     When this snapshot was taken.
 */
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
        @NotNull Instant lastFished,
        @NotNull Instant snapshotTime
) {

    /**
     * Compact constructor validates invariants.
     */
    public PlayerStatSnapshot {
        if (totalCaught < 0) throw new IllegalArgumentException("totalCaught must be >= 0");
        if (rareCaught < 0) throw new IllegalArgumentException("rareCaught must be >= 0");
        if (legendaryCaught < 0) throw new IllegalArgumentException("legendaryCaught must be >= 0");
    }

    /**
     * Creates an empty (zero-value) snapshot for a player with no recorded activity.
     *
     * @param playerUuid The player's UUID.
     * @param playerName The player's name.
     * @return An empty snapshot with all counters at 0.
     */
    @NotNull
    public static PlayerStatSnapshot empty(@NotNull UUID playerUuid, @NotNull String playerName) {
        return new PlayerStatSnapshot(
                playerUuid, playerName,
                0, 0, 0, 0, 0,
                0, 0, 0,
                Instant.EPOCH,
                Instant.now()
        );
    }

    /**
     * Enum for selecting the sort criterion in {@link MythicRodAPI#getTopPlayers}.
     */
    public enum StatType {
        /** Sort by {@link #totalCaught()}. */
        TOTAL_CAUGHT,
        /** Sort by {@link #rareCaught()}. */
        RARE_CAUGHT,
        /** Sort by {@link #legendaryCaught()}. */
        LEGENDARY_CAUGHT,
        /** Sort by {@link #lastFished()} descending (most recent first). */
        LAST_FISHED
    }
}
