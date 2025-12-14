package io.xcutiboo.mythicrod.fishing;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
public class FishingListener implements Listener {
    private final MythicRod plugin;
    private final Map<UUID, FishingState> activeFishing = new ConcurrentHashMap<>();
    public FishingListener(MythicRod plugin) {
        this.plugin = plugin;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        FishHook hook = event.getHook();
        UUID hookId = hook.getUniqueId();
        PlayerFishEvent.State state = event.getState();
        switch (state) {
            case FISHING -> handleFishingStart(player, hook, hookId);
            case CAUGHT_FISH -> handleCaughtFish(event, player, hook, hookId);
            case REEL_IN -> handleReelIn(hookId);
            case IN_GROUND, FAILED_ATTEMPT -> handleFishingEnd(hookId);
            case CAUGHT_ENTITY -> handleCaughtEntity(hookId);
            case BITE, LURED -> {}
        }
    }
    private void handleFishingStart(Player player, FishHook hook, UUID hookId) {
        FishingState state = new FishingState(player.getUniqueId(), hook.getLocation());
        activeFishing.put(hookId, state);
        plugin.getLogger().fine("Player " + player.getName() + " started fishing with hook " + hookId);
    }
    private void handleCaughtFish(PlayerFishEvent event, Player player, FishHook hook, UUID hookId) {
        boolean debugMode = plugin.getConfigManager().isDebugMode();
        if (debugMode) {
            plugin.getLogger().info("========================================");
            plugin.getLogger().info("CAUGHT_FISH event fired for " + player.getName());
        }
        FishingState state = activeFishing.get(hookId);
        // Prevent duplicate processing
        if (state == null) {
            if (debugMode) {
                plugin.getLogger().warning("  State is NULL for hook " + hookId);
            }
            return;
        }
        if (state.hasReceivedReward()) {
            if (debugMode) {
                plugin.getLogger().info("  Already received reward - skipping");
            }
            return;
        }
        // Mark as processed
        state.setReceivedReward(true);
        if (debugMode) {
            plugin.getLogger().info("  Marked as processed");
        }
        // Get the vanilla caught entity (if any)
        Entity caughtEntity = event.getCaught();
        if (debugMode) {
            plugin.getLogger().info("  Caught entity: " + (caughtEntity != null ? caughtEntity.getType() : "NULL"));
        }
        // Get biome for drop selection
        Location hookLoc = hook.getLocation();
        org.bukkit.block.Biome biome = hookLoc.getWorld().getBiome(hookLoc);
        String biomeName = biome.getKey().getKey();
        if (debugMode) {
            plugin.getLogger().info("  Biome: " + biomeName);
            plugin.getLogger().info("  Calling getRandomDrop...");
        }
        CustomDrop drop = plugin.getDropManager().getRandomDrop(player, biomeName);
        if (drop == null) {
            if (debugMode) {
                plugin.getLogger().warning("  Drop is NULL - letting vanilla handle");
                plugin.getLogger().info("========================================");
            }
            // Don't cancel event, let vanilla handle it
            return;
        }
        if (debugMode) {
            plugin.getLogger().info("  Selected drop: " + drop.getMaterial() + " x" + drop.getAmount());
        }
        // Remove vanilla item if it exists
        if (caughtEntity instanceof Item vanillaItem) {
            if (debugMode) {
                plugin.getLogger().info("  Removing vanilla item");
            }
            vanillaItem.remove();
        }
        // Create and give custom item immediately
        ItemStack customItem = drop.createItemStack();
        if (debugMode) {
            plugin.getLogger().info("  Created custom item: " + customItem.getType() + " x" + customItem.getAmount());
        }
        // Give item to player
        if (player.getInventory().firstEmpty() != -1) {
            if (debugMode) {
                plugin.getLogger().info("  Adding to inventory");
            }
            player.getInventory().addItem(customItem);
        } else {
            if (debugMode) {
                plugin.getLogger().info("  Inventory full - dropping at player");
            }
            // Inventory full - drop near player
            Item droppedItem = player.getWorld().dropItem(player.getLocation().add(0, 0.5, 0), customItem);
            droppedItem.setPickupDelay(0);
            droppedItem.setOwner(player.getUniqueId());
            droppedItem.setVelocity(new Vector(0, 0.1, 0));
        }
        // Send success message immediately
        sendCatchMessage(player, drop);
        // Spawn effects
        spawnCatchEffects(player, hookLoc);
        // Record statistics
        if (plugin.getConfigManager().trackStatistics()) {
            plugin.getStatisticsManager().recordCatch(player, drop);
        }
        // Give experience
        giveExperience(player, drop);
        if (debugMode) {
            plugin.getLogger().info("  SUCCESS - Custom drop given!");
            plugin.getLogger().info("========================================");
        }
    }
    private void handleReelIn(UUID hookId) {
        // Clean up fishing state
        activeFishing.remove(hookId);
    }
    private void handleFishingEnd(UUID hookId) {
        activeFishing.remove(hookId);
    }
    private void handleCaughtEntity(UUID hookId) {
        // Allow vanilla behavior for caught entities
        activeFishing.remove(hookId);
    }
    private void spawnCatchEffects(Player player, Location hookLocation) {
        if (plugin.getConfigManager().useParticles()) {
            // Splash particles at hook location
            player.getWorld().spawnParticle(
                Particle.SPLASH,
                hookLocation,
                30,
                0.3, 0.3, 0.3,
                0.15
            );
            // Bubble particles rising
            player.getWorld().spawnParticle(
                Particle.BUBBLE_POP,
                hookLocation.clone().add(0, 0.3, 0),
                15,
                0.2, 0.2, 0.2,
                0.05
            );
            // Success particles at player
            player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1.5, 0),
                5,
                0.3, 0.3, 0.3,
                0.02
            );
        }
        if (plugin.getConfigManager().useSounds()) {
            // Splash sound
            player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);
            // Retrieve sound
            player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.8f, 1.2f);
            // Success sound (delayed slightly)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 2.0f);
                }
            }, 3L);
        }
    }
    private void sendCatchMessage(Player player, CustomDrop drop) {
        String itemName = drop.getCustomName() != null ?
            drop.getCustomName() :
            formatMaterialName(drop.getMaterial().name());
        String messageKey;
        if (drop.getChance() <= 1) {
            messageKey = "fishing.catch-legendary";
        } else if (drop.getChance() <= 5) {
            messageKey = "fishing.catch-rare";
        } else {
            messageKey = "fishing.catch-normal";
        }
        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(
            plugin.getLanguageManager().tr(messageKey,
                Map.of("item", itemName, "amount", String.valueOf(drop.getAmount())))
        );
        player.sendMessage(message);
    }
    private String formatMaterialName(String materialName) {
        return materialName.replace("_", " ")
            .toLowerCase()
            .substring(0, 1).toUpperCase() + materialName.replace("_", " ")
            .toLowerCase()
            .substring(1);
    }
    private String getRarityColor(int chance) {
        if (chance <= 1) return "§d§l"; // Legendary - Light Purple Bold
        if (chance <= 5) return "§5§l"; // Epic - Dark Purple Bold
        if (chance <= 15) return "§9§l"; // Rare - Blue Bold
        if (chance <= 30) return "§a"; // Uncommon - Green
        return "§f"; // Common - White
    }
    private void giveExperience(Player player, CustomDrop drop) {
        // Give vanilla fishing XP based on rarity
        int xpAmount;
        if (drop.getChance() <= 1) {
            xpAmount = 6; // Legendary
        } else if (drop.getChance() <= 5) {
            xpAmount = 5; // Epic
        } else if (drop.getChance() <= 15) {
            xpAmount = 3; // Rare
        } else if (drop.getChance() <= 30) {
            xpAmount = 2; // Uncommon
        } else {
            xpAmount = 1; // Common
        }
        player.giveExp(xpAmount);
        // Show XP particles
        if (plugin.getConfigManager().useParticles()) {
            player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1.2, 0),
                xpAmount * 2,
                0.2, 0.3, 0.2,
                0.05
            );
        }
    }
    public void cleanupHooks() {
        activeFishing.entrySet().removeIf(entry -> {
            UUID hookId = entry.getKey();
            FishingState state = entry.getValue();
            // Remove if hook entity doesn't exist
            Entity entity = plugin.getServer().getEntity(hookId);
            if (entity == null) {
                return true;
            }
            // Remove if state is older than 5 minutes
            return System.currentTimeMillis() - state.getStartTime() > 300000;
        });
    }
    private static class FishingState {
        private final long startTime;
        private boolean receivedReward;
        public FishingState(UUID playerUuid, Location castLocation) {
            this.startTime = System.currentTimeMillis();
            this.receivedReward = false;
        }
        public long getStartTime() {
            return startTime;
        }
        public boolean hasReceivedReward() {
            return receivedReward;
        }
        public void setReceivedReward(boolean receivedReward) {
            this.receivedReward = receivedReward;
        }
    }
}
