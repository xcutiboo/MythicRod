package io.xcutiboo.mythicrod.api;

import io.xcutiboo.mythicrod.api.PlayerStatSnapshot.StatType;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.drops.DropRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API surface for MythicRod.
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>Methods that return {@link CompletableFuture} execute their work on the
 *       async scheduler. The future completes on an arbitrary thread — do NOT
 *       perform world/inventory mutations in the completion callback. Schedule
 *       those back to the entity or region scheduler.</li>
 *   <li>Methods that are <em>not</em> future-backed must be called from the
 *       correct region thread that owns the target entity/world position.</li>
 * </ul>
 *
 * <h2>Obtaining the API instance</h2>
 * <pre>{@code
 * RegisteredServiceProvider<MythicRodAPI> provider =
 *     Bukkit.getServicesManager().getRegistration(MythicRodAPI.class);
 * if (provider != null) {
 *     MythicRodAPI api = provider.getProvider();
 * }
 * }</pre>
 *
 * <h2>API Stability</h2>
 * This interface is stable and versioned. Breaking changes will only occur
 * between major plugin versions and will be clearly documented in the changelog.
 */
public interface MythicRodAPI {

    // =========================================================================
    // Plugin meta
    // =========================================================================

    /**
     * Returns the running plugin version string (e.g. {@code "1.2.0"}).
     *
     * @return Non-null version string.
     */
    @NotNull
    String getVersion();

    // =========================================================================
    // Drop system access
    // =========================================================================

    /**
     * Returns the drop manager, providing read-write access to the weighted
     * drop table loaded from {@code drops.yml}.
     *
     * <p>Mutations via this object take effect immediately in-memory. Call
     * {@link DropManager#saveDrops()} to persist changes to disk.
     *
     * @return Non-null drop manager.
     */
    @NotNull
    DropManager getDropManager();

    /**
     * Returns the drop registry, providing lightweight category enumeration.
     *
     * @return Non-null drop registry.
     */
    @NotNull
    DropRegistry getDropRegistry();

    // =========================================================================
    // External drop provider registration
    // =========================================================================

    /**
     * Registers an external drop provider, injecting its drops into MythicRod's
     * weighted selection pipeline.
     *
     * <p>If a provider with the same key is already registered, it is silently
     * replaced. Providers are consulted on every fishing event — ensure
     * {@link ExternalDropProvider#getWeight} is O(1) and non-blocking.
     *
     * @param provider Non-null provider to register.
     */
    void registerExternalDropProvider(@NotNull ExternalDropProvider provider);

    /**
     * Unregisters the drop provider identified by the given key.
     *
     * @param key The {@link ExternalDropProvider#getKey()} value of the provider.
     * @return {@code true} if a provider with that key was found and removed.
     */
    boolean unregisterExternalDropProvider(@NotNull String key);

    /**
     * Returns a registered external drop provider by key, or empty if not found.
     *
     * @param key The provider key.
     * @return Optional containing the provider, or empty.
     */
    @NotNull
    Optional<ExternalDropProvider> getExternalDropProvider(@NotNull String key);

    /**
     * Returns an unmodifiable view of all currently registered external providers.
     *
     * @return Immutable list of providers. May be empty.
     */
    @NotNull
    List<ExternalDropProvider> getExternalDropProviders();

    // =========================================================================
    // Statistics — CompletableFuture-backed (async-safe)
    // =========================================================================

    /**
     * Asynchronously fetches a {@link PlayerStatSnapshot} for the given player.
     *
     * <p>The returned future completes on an async thread. To update UI or
     * inventory based on the result, schedule back to the player's entity scheduler.
     *
     * <p>If no data exists for the player, the future completes with
     * {@link PlayerStatSnapshot#empty(UUID, String)}.
     *
     * @param playerId The UUID of the player to query.
     * @return A future that completes with the player's stat snapshot.
     */
    @NotNull
    CompletableFuture<@NotNull PlayerStatSnapshot> getPlayerStats(@NotNull UUID playerId);

    /**
     * Asynchronously fetches the top {@code limit} players ranked by {@code statType}.
     *
     * <p>Results are sorted descending (highest first), except for
     * {@link StatType#LAST_FISHED} which is sorted by most-recent first.
     *
     * @param statType The statistic to rank by. Must not be null.
     * @param limit    Maximum number of results to return. Clamped to 1–100.
     * @return A future completing with a sorted, immutable list of snapshots.
     */
    @NotNull
    CompletableFuture<@NotNull List<@NotNull PlayerStatSnapshot>> getTopPlayers(
            @NotNull StatType statType,
            int limit
    );

    /**
     * Flushes all in-memory player statistics to persistent storage.
     * This is called automatically on plugin shutdown but may be invoked
     * explicitly by external plugins (e.g., before a server backup).
     *
     * @return A future that completes when all data has been written.
     */
    @NotNull
    CompletableFuture<Void> flushAllStats();
}
