package com.mythicrod.mythicrod.fishing;

import com.mythicrod.mythicrod.MythicRod;
import com.mythicrod.mythicrod.drops.CustomDrop;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FishingListener implements Listener {

    private final MythicRod plugin;
    private final Map<UUID, Boolean> rewardedHooks = new HashMap<>();

    public FishingListener(MythicRod plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        FishHook hook = event.getHook();
        Player player = event.getPlayer();
        UUID hookId = hook.getUniqueId();

        if (event.getState() == PlayerFishEvent.State.FISHING) {
            rewardedHooks.put(hookId, false);
            return;
        }

        if (event.getState() == PlayerFishEvent.State.REEL_IN
                || event.getState() == PlayerFishEvent.State.IN_GROUND
                || event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT) {
            rewardedHooks.remove(hookId);
            return;
        }

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (rewardedHooks.containsKey(hookId) && rewardedHooks.get(hookId)) {
                event.setCancelled(true);
                return;
            }

            rewardedHooks.put(hookId, true);
            event.setCancelled(true);

            String biomeName = hook.getLocation().getBlock().getBiome().name();
            CustomDrop drop = plugin.getDropManager().getRandomDrop(player, biomeName);

            if (drop != null) {
                ItemStack itemStack = drop.createItemStack();
                Location hookLocation = hook.getLocation();

                Item item = player.getWorld().dropItem(hookLocation, itemStack);
                item.setVelocity(player.getLocation().subtract(hookLocation).toVector().multiply(0.1));

                if (plugin.getConfigManager().useParticles()) {
                    hookLocation.getWorld().spawnParticle(
                            Particle.WATER_SPLASH,
                            hookLocation,
                            20, 0.3, 0.3, 0.3, 0.1
                    );
                }

                if (plugin.getConfigManager().useSounds()) {
                    player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);
                }

                if (plugin.getConfigManager().trackStatistics()) {
                    plugin.getStatisticsManager().recordCatch(player, drop);
                }
            }
        }

        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            rewardedHooks.remove(hookId);
        }
    }

    public void cleanupHooks() {
        rewardedHooks.entrySet().removeIf(entry
                -> plugin.getServer().getEntity(entry.getKey()) == null);
    }
}
