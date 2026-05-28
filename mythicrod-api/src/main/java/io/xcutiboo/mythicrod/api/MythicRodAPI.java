package io.xcutiboo.mythicrod.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import io.xcutiboo.mythicrod.api.PlayerStatSnapshot.StatType;
import io.xcutiboo.mythicrod.api.drop.DropCatalog;
import io.xcutiboo.mythicrod.api.platform.PlatformDrop;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformItemFactory;

import org.jetbrains.annotations.Nullable;

/// Public integration contract for MythicRod.
///
/// ## Thread Safety
///
/// - Methods that return `CompletableFuture` execute their work on a
///   MythicRod-owned async scheduler. The future completes on an arbitrary async
///   thread; do not perform world, entity, or inventory mutations in a completion
///   callback. Schedule those changes back to the correct entity, region, or
///   global scheduler.
/// - Methods that are not future-backed must be called from the correct region
///   thread that owns the target entity or world position.
///
/// ## Obtaining the API Instance
///
/// ```java
/// RegisteredServiceProvider<MythicRodAPI> provider =
///     Bukkit.getServicesManager().getRegistration(MythicRodAPI.class);
/// if (provider != null) {
///     MythicRodAPI api = provider.getProvider();
/// }
/// ```
///
/// ## Compatibility
///
/// Methods on this interface are stable unless the changelog calls out a major
/// version break. Platform-specific implementation classes remain internal.
@ApiStatus.AvailableSince("2026.1.0")
public interface MythicRodAPI {

    /// Returns the running plugin version string, such as `"2026.1.0"`.
    ///
    /// @return running MythicRod version string
    @NotNull
    String getVersion();

    /// Returns a read-only catalog of the currently loaded drop table.
    ///
    /// This view is intentionally inspection-only. External plugins should not
    /// depend on MythicRod's mutable internal drop-management services.
    ///
    /// @return read-only snapshot view of the loaded drop table
    @NotNull
    DropCatalog getDropCatalog();

    /// Returns the supported item factory for this runtime.
    ///
    /// This is the preferred way for external plugins to create a `PlatformItem`
    /// that MythicRod can safely consume. Callers should prefer this factory over
    /// instantiating Paper implementation classes directly.
    ///
    /// @return runtime item factory for MythicRod-compatible items
    @NotNull
    PlatformItemFactory getItemFactory();

    /// Convenience wrapper around `getItemFactory()`.
    ///
    /// This keeps simple integrations from reaching into platform services for
    /// the common case of constructing an item by identifier and amount.
    ///
    /// @param identifier Platform item identifier such as `"DIAMOND"` or
    ///                   `"nexo:my_custom_item"`.
    /// @param amount Requested stack size.
    /// @return A success/failure result describing the created platform item.
    @NotNull
    default Result<PlatformItem> createItem(@NotNull String identifier, int amount) {
        return getItemFactory().createItem(identifier, amount);
    }

    /// Registers an external drop provider, injecting its drops into MythicRod's
    /// weighted selection pipeline.
    ///
    /// If a provider with the same key is already registered, it is silently
    /// replaced. Providers are consulted on every fishing event, so keep
    /// `ExternalDropProvider#getWeight` O(1) and non-blocking.
    ///
    /// @param provider Non-null provider to register.
    void registerExternalDropProvider(@NotNull ExternalDropProvider provider);

    /// Unregisters the drop provider identified by the given key.
    ///
    /// @param key The `ExternalDropProvider#getKey()` value of the provider.
    /// @return `true` if a provider with that key was found and removed.
    boolean unregisterExternalDropProvider(@NotNull String key);

    /// Returns a registered external drop provider by key, or empty if not found.
    ///
    /// @param key The provider key.
    /// @return Optional containing the provider, or empty.
    @NotNull
    Optional<ExternalDropProvider> getExternalDropProvider(@NotNull String key);

    /// Returns an unmodifiable view of all currently registered external providers.
    ///
    /// @return Immutable list of providers. May be empty.
    @NotNull
    List<ExternalDropProvider> getExternalDropProviders();

    /// Asynchronously fetches a `PlayerStatSnapshot` for the given player.
    ///
    /// The returned future completes on an arbitrary async thread. To update UI,
    /// entities, worlds, or inventories from the result, schedule back to the
    /// correct Paper/Folia owner scheduler.
    ///
    /// If no data exists for the player, the future completes with
    /// `PlayerStatSnapshot#empty(UUID, String)`.
    ///
    /// MythicRod does not set an internal timeout. Cancelling the returned future
    /// prevents the caller's continuation work, but it may not stop an internal
    /// read that already started. Read failures complete the future exceptionally.
    ///
    /// @param playerId The UUID of the player to query.
    /// @return A future that completes with the player's stat snapshot.
    @NotNull
    CompletableFuture<@NotNull PlayerStatSnapshot> getPlayerStats(@NotNull UUID playerId);

    /// Asynchronously fetches the top `limit` players ranked by `statType`.
    ///
    /// Results are sorted descending (highest first), except for
    /// `StatType#LAST_FISHED`, which is sorted by most-recent first.
    ///
    /// The returned future completes on an arbitrary async thread and follows the
    /// same cancellation, timeout, and error behavior as `getPlayerStats(UUID)`.
    ///
    /// @param statType The statistic to rank by. Must not be null.
    /// @param limit Maximum number of results to return. Clamped to 1-100.
    /// @return A future completing with a sorted, immutable list of snapshots.
    @NotNull
    CompletableFuture<@NotNull List<@NotNull PlayerStatSnapshot>> getTopPlayers(
            @NotNull StatType statType,
            int limit
    );

    /// Returns the drops the given online player would be eligible to roll at
    /// the given biome, after biome filters and permission filters are applied.
    ///
    /// Returns an empty list when the player is offline or no drops are
    /// eligible. The returned list is an immutable snapshot of the current
    /// drop table - subsequent reloads do not retroactively change it.
    ///
    /// Intended for minigame UIs, tutorial overlays, and "what could I catch
    /// here?" inspections. The actual roll is still resolved at catch time and
    /// can be biased or replaced by `MythicRodRewardRollEvent`.
    ///
    /// @param playerId UUID of the player to check eligibility for. Must
    ///                 belong to a currently-online player.
    /// @param biomeKey Biome key such as `"minecraft:ocean"`, or `null` to
    ///                 ignore biome filters.
    /// @return immutable list of eligible drops. Empty when the player is
    ///         offline or has no eligible drops.
    @ApiStatus.AvailableSince("2026.2.0")
    @NotNull
    List<? extends PlatformDrop> previewEligibleDrops(
            @NotNull UUID playerId,
            @Nullable String biomeKey);

    /// Flushes all in-memory player statistics to persistent storage.
    ///
    /// This is called automatically on plugin shutdown but may be invoked
    /// explicitly by external plugins before a server backup.
    ///
    /// The returned future completes on an arbitrary async thread. MythicRod does
    /// not set an internal timeout. Cancelling the returned future does not
    /// guarantee cancellation of an already-started file write. Save failures
    /// complete the future exceptionally.
    ///
    /// @return A future that completes when all data has been written.
    @NotNull
    CompletableFuture<Void> flushAllStats();
}
