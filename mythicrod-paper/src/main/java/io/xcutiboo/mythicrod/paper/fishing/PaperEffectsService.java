package io.xcutiboo.mythicrod.paper.fishing;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.api.platform.PlatformLocation;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.service.EffectsService;
import io.xcutiboo.mythicrod.paper.platform.PaperLocation;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PaperEffectsService implements EffectsService {
    private final MythicRod plugin;
    private final PlatformScheduler scheduler;

    @Override
    public void spawnCatchEffects(PlatformPlayer player, PlatformLocation location) {
        if (player == null || !player.isOnline()) return;
        if (location == null || location.getWorldName() == null) return;

        scheduler.runAtLocation(location, () -> {
            Player bukkitPlayer = ((PaperPlayer) player).getBukkitPlayer();
            Location bukkitLoc = PaperLocation.toBukkit(location, plugin.getServer());
            if (bukkitLoc == null) return;

            if (plugin.getConfigManager().useParticles()) {
                spawnParticles(bukkitPlayer, bukkitLoc);
            }

            if (plugin.getConfigManager().useSounds()) {
                playSounds(bukkitPlayer, bukkitLoc, location);
            }
        });
    }

    @Override
    public void spawnExperienceEffects(PlatformPlayer player, int xpAmount) {
        if (player == null || !player.isOnline()) return;
        if (!plugin.getConfigManager().useParticles()) return;

        Player bukkitPlayer = ((PaperPlayer) player).getBukkitPlayer();
        Location playerLoc = bukkitPlayer.getLocation();
        
        if (playerLoc.getWorld() != null) {
            playerLoc.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                playerLoc.add(0, 1.2, 0),
                xpAmount * 2,
                0.2, 0.3, 0.2,
                0.05
            );
        }
    }

    private void spawnParticles(Player player, Location hookLocation) {
        try {
            hookLocation.getWorld().spawnParticle(
                Particle.SPLASH, hookLocation, 30, 0.3, 0.3, 0.3, 0.15);
            hookLocation.getWorld().spawnParticle(
                Particle.BUBBLE_POP, hookLocation.clone().add(0, 0.3, 0), 15, 0.2, 0.2, 0.2, 0.05);

            Location playerLoc = player.getLocation();
            playerLoc.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER, playerLoc.add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.02);
        } catch (Exception e) {
            plugin.getLogger().fine("Particle error: " + e.getMessage());
        }
    }

    private void playSounds(Player player, Location hookLocation, PlatformLocation platformLoc) {
        try {
            player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);
            player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.8f, 1.2f);
            
            scheduler.runAtLocationDelayed(platformLoc, () -> {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 2.0f);
                }
            }, 3L);
        } catch (Exception e) {
            plugin.getLogger().fine("Sound error: " + e.getMessage());
        }
    }
}
