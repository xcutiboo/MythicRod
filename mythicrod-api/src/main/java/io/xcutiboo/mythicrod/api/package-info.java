/// Stable MythicRod integration contracts.
///
/// External plugins normally start with {@link io.xcutiboo.mythicrod.api.MythicRodAPI},
/// then work with {@link io.xcutiboo.mythicrod.api.ExternalDropProvider},
/// {@link io.xcutiboo.mythicrod.api.PlayerStatSnapshot}, and
/// {@link io.xcutiboo.mythicrod.api.drop.DropCatalog}. Platform-neutral value
/// types live in {@link io.xcutiboo.mythicrod.api.platform}.
///
/// The Paper runtime publishes {@code MythicRodAPI} through Bukkit's
/// {@code ServicesManager}. Future-backed methods complete on MythicRod-owned
/// async threads and must be rescheduled before touching platform state.
package io.xcutiboo.mythicrod.api;
