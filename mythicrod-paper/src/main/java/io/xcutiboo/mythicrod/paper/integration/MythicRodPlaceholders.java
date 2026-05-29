package io.xcutiboo.mythicrod.paper.integration;

import java.util.Locale;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.paper.api.PaperMythicRodAPI;
import io.xcutiboo.mythicrod.stats.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/// PlaceholderAPI expansion exposing MythicRod stats as `%mythicrod_*%`
/// tokens for scoreboards, tab lists, hologram plugins, and chat
/// formatters.
///
/// Available tokens (all return strings, never null):
///
/// - `%mythicrod_total%` - lifetime catches
/// - `%mythicrod_common%` / `%mythicrod_uncommon%` / `%mythicrod_rare%`
///   / `%mythicrod_legendary%` - catches by tier
/// - `%mythicrod_rod_tier%` - the player's selected default rod tier
///   (basic, advanced, legendary, mythic). Returns "basic" when no
///   choice has been made.
/// - `%mythicrod_version%` - running plugin version
///
/// Returns the empty string when the player is offline and has no
/// persisted stats yet, except `%mythicrod_version%` which always
/// resolves.
public final class MythicRodPlaceholders extends PlaceholderExpansion {

    private final MythicRod plugin;
    private final PaperMythicRodAPI api;

    public MythicRodPlaceholders(@NotNull MythicRod plugin, @NotNull PaperMythicRodAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mythicrod";
    }

    @Override
    public @NotNull String getAuthor() {
        return "xcutiboo";
    }

    @Override
    public @NotNull String getVersion() {
        return api.getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        String token = params.toLowerCase(Locale.ROOT);
        if ("version".equals(token)) {
            return api.getVersion();
        }
        if (player == null) {
            return "";
        }
        UUID id = player.getUniqueId();
        if ("rod_tier".equals(token)) {
            return readRodTier(id);
        }
        PlayerStats stats = plugin.getStatisticsManager() != null
            ? plugin.getStatisticsManager().getStats(id)
            : null;
        if (stats == null) {
            return "";
        }
        return switch (token) {
            case "total" -> String.valueOf(stats.getTotalCaught());
            case "common" -> String.valueOf(stats.getCommonCaught());
            case "uncommon" -> String.valueOf(stats.getUncommonCaught());
            case "rare" -> String.valueOf(stats.getRareCaught());
            case "legendary" -> String.valueOf(stats.getLegendaryCaught());
            default -> null;
        };
    }

    private @NotNull String readRodTier(@NotNull UUID id) {
        if (plugin.getPlayerDataService() == null) {
            return "basic";
        }
        org.bukkit.entity.Player online = plugin.getServer().getPlayer(id);
        if (online == null) {
            return "basic";
        }
        String tier = plugin.getPlayerDataService().getRodTier(online);
        return tier != null && !tier.isBlank() ? tier : "basic";
    }
}
