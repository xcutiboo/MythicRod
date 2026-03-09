package io.xcutiboo.mythicrod.paper.fishing;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.fishing.EffectsService;

public class PaperEffectsService implements EffectsService {
    private final MythicRod plugin;

    public PaperEffectsService(MythicRod plugin) {
        this.plugin = plugin;
    }

    @Override
    public void spawnCatchEffects(Player player, Location hookLocation) {
        if (player == null || !player.isOnline()) return;
        if (hookLocation == null || hookLocation.getWorld() == null) return;

        if (plugin.getConfigManager().useParticles()) {
            spawnParticles(player, hookLocation);
        }

        if (plugin.getConfigManager().useSounds()) {
            playSounds(player, hookLocation);
        }
    }

    @Override
    public void spawnExperienceEffects(Player player, int xpAmount) {
        if (player == null || !player.isOnline()) return;
        if (!plugin.getConfigManager().useParticles()) return;

        Location playerLoc = player.getLocation();
        if (playerLoc != null && playerLoc.getWorld() != null) {
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
                Particle.SPLASH,
                hookLocation,
                30,
                0.3, 0.3, 0.3,
                0.15
            );

            hookLocation.getWorld().spawnParticle(
                Particle.BUBBLE_POP,
                hookLocation.clone().add(0, 0.3, 0),
                15,
                0.2, 0.2, 0.2,
                0.05
            );

            Location playerLoc = player.getLocation();
            if (playerLoc != null && playerLoc.getWorld() != null) {
                playerLoc.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    playerLoc.add(0, 1.5, 0),
                    5,
                    0.3, 0.3, 0.3,
                    0.02
                );
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Failed to spawn particles: " + e.getMessage());
        }
    }

    private void playSounds(Player player, Location hookLocation) {
        try {
            player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);
            player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.8f, 1.2f);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    Location playerLoc = player.getLocation();
                    if (playerLoc != null) {
                        player.playSound(playerLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 2.0f);
                    }
                }
            }, 3L);
        } catch (Exception e) {
            plugin.getLogger().fine("Failed to play sounds: " + e.getMessage());
        }
    }
}
