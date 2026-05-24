package io.xcutiboo.mythicrod.paper.util;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/// Tier-coded particle and sound feedback for catch events.
///
/// The palette follows the loot-tier convention every player has
/// learned across the last two decades of role-playing games:
/// white-common, green-uncommon, blue-rare, purple-legendary,
/// orange-mythic. Each tier pairs one hero particle with one short
/// sound; legendary and mythic add a brief rising-helix animation
/// for spectacle without breaking the budget (player-scoped packets
/// only, capped at one second).
///
/// Calls fire only at the player who caught the drop. Other players
/// nearby never receive the packets, so the cost stays bounded no
/// matter how busy the lake is.
public final class TierVisualEffects {

    private static final Color COMMON     = Color.fromRGB(0xFF, 0xFF, 0xFF);
    private static final Color UNCOMMON   = Color.fromRGB(0x55, 0xFF, 0x55);
    private static final Color RARE       = Color.fromRGB(0x55, 0x55, 0xFF);
    private static final Color LEGENDARY  = Color.fromRGB(0xAA, 0x00, 0xAA);
    private static final Color MYTHIC     = Color.fromRGB(0xFF, 0xAA, 0x00);

    private static final float DUST_SIZE        = 1.2f;
    private static final int   HELIX_TICKS      = 20;   // 1 second of spectacle.
    private static final int   HELIX_POINTS     = 3;    // particles per tick.
    private static final double HELIX_RADIUS    = 0.7;
    private static final double HELIX_LIFT      = 0.12; // y rise per tick.

    private TierVisualEffects() {
    }

    /// Plays the catch celebration tied to the given tier at the
    /// player's current location. Falls back to the common cue when
    /// the tier name is unknown. Legendary and mythic schedule a
    /// short rising-helix animation on the player's owner thread.
    public static void playCatch(@NotNull JavaPlugin plugin, @NotNull Player player, @NotNull String tier) {
        Location at = player.getLocation().add(0, 1.0, 0);
        switch (tier.toLowerCase(Locale.ROOT)) {
            case "uncommon" -> uncommon(player, at);
            case "rare"     -> rare(player, at);
            case "legendary" -> {
                legendaryBurst(player, at);
                helix(plugin, player, LEGENDARY, MYTHIC);
            }
            case "mythic", "mythical" -> {
                mythicBurst(player, at);
                helix(plugin, player, MYTHIC, COMMON);
            }
            default -> common(player, at);
        }
    }

    private static void common(Player player, Location at) {
        player.spawnParticle(Particle.HAPPY_VILLAGER, at, 6, 0.4, 0.3, 0.4, 0);
        player.playSound(at, Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
    }

    private static void uncommon(Player player, Location at) {
        player.spawnParticle(Particle.END_ROD, at, 8, 0.35, 0.4, 0.35, 0.02);
        Particle.DustOptions dust = new Particle.DustOptions(UNCOMMON, DUST_SIZE);
        player.spawnParticle(Particle.DUST, at, 12, 0.4, 0.4, 0.4, 0, dust);
        player.playSound(at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }

    private static void rare(Player player, Location at) {
        Particle.DustOptions dust = new Particle.DustOptions(RARE, DUST_SIZE);
        player.spawnParticle(Particle.DUST, at, 20, 0.5, 0.5, 0.5, 0, dust);
        player.spawnParticle(Particle.ENCHANT, at, 30, 0.5, 0.5, 0.5, 0.5);
        player.playSound(at, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.0f);
    }

    private static void legendaryBurst(Player player, Location at) {
        Particle.DustTransition transition =
            new Particle.DustTransition(LEGENDARY, MYTHIC, DUST_SIZE + 0.3f);
        player.spawnParticle(Particle.DUST_COLOR_TRANSITION, at, 30,
            0.6, 0.7, 0.6, 0, transition);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, at, 12,
            0.4, 0.5, 0.4, 0.3);
        player.playSound(at, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    private static void mythicBurst(Player player, Location at) {
        Particle.DustTransition transition =
            new Particle.DustTransition(MYTHIC, COMMON, DUST_SIZE + 0.5f);
        player.spawnParticle(Particle.DUST_COLOR_TRANSITION, at, 40,
            0.7, 0.8, 0.7, 0, transition);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, at, 20,
            0.5, 0.6, 0.5, 0.5);
        player.playSound(at, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.35f, 0.8f);
    }

    /// Schedules a rising-helix animation around the player on the
    /// player's owner thread. Uses Particle.DUST_COLOR_TRANSITION so
    /// the trail crossfades from start to end while it rises.
    private static void helix(JavaPlugin plugin, Player player, Color from, Color to) {
        Particle.DustTransition transition =
            new Particle.DustTransition(from, to, DUST_SIZE);
        AtomicInteger ticks = new AtomicInteger(0);
        player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }
            int tick = ticks.getAndIncrement();
            if (tick >= HELIX_TICKS) {
                task.cancel();
                return;
            }
            spawnHelixSlice(player, transition, tick);
        }, () -> { }, 1L, 1L);
    }

    /// Spawns one slice of a rising helix around the player. Two
    /// points per tick on opposite sides give the double-helix look
    /// without doubling the particle count.
    private static void spawnHelixSlice(Player player, Particle.DustTransition transition, int tick) {
        Location base = player.getLocation();
        for (int p = 0; p < HELIX_POINTS; p++) {
            double angle = (tick * 0.45) + (p * (2 * Math.PI / HELIX_POINTS));
            double x = base.getX() + HELIX_RADIUS * Math.cos(angle);
            double z = base.getZ() + HELIX_RADIUS * Math.sin(angle);
            double y = base.getY() + 0.2 + (tick * HELIX_LIFT);
            player.spawnParticle(Particle.DUST_COLOR_TRANSITION, x, y, z,
                1, 0, 0, 0, 0, transition);
        }
    }

}
