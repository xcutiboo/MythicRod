package io.xcutiboo.mythicrod.spigot.fishing;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.fishing.EffectsService;
import io.xcutiboo.mythicrod.fishing.FishingService;
import io.xcutiboo.mythicrod.fishing.FishingService.FishingResult;
import io.xcutiboo.mythicrod.fishing.RewardService;

public class FishingListener implements Listener {
    private final MythicRod plugin;
    private final FishingService fishingService;
    private final RewardService rewardService;
    private final EffectsService effectsService;

    public FishingListener(MythicRod plugin, FishingService fishingService, RewardService rewardService, EffectsService effectsService) {
        this.plugin = plugin;
        this.fishingService = fishingService;
        this.rewardService = rewardService;
        this.effectsService = effectsService;
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
            case BITE -> {} // Note: Paper has LURED state (Paper 1.21+), Spigot does not
        }
    }

    private void handleFishingStart(Player player, FishHook hook, UUID hookId) {
        fishingService.startFishing(hookId, player.getUniqueId(), hook.getLocation());
        plugin.getLogger().fine("Player " + player.getName() + " started fishing with hook " + hookId);
    }

    private void handleCaughtFish(PlayerFishEvent event, Player player, FishHook hook, UUID hookId) {
        boolean debugMode = plugin.getConfigManager().isDebugMode();
        if (debugMode) {
            plugin.getLogger().info("========================================");
            plugin.getLogger().info("CAUGHT_FISH event fired for " + player.getName());
        }

        Location hookLoc = hook.getLocation();
        FishingResult result = fishingService.processCatch(hookId, player, hookLoc);

        if (!result.isSuccess()) {
            if (debugMode) {
                plugin.getLogger().info("  Result: " + result.getType() + " - skipping");
                plugin.getLogger().info("========================================");
            }
            return;
        }

        CustomDrop drop = result.getDrop();
        if (debugMode) {
            plugin.getLogger().info("  Biome: " + result.getBiomeName());
            plugin.getLogger().info("  Selected drop: " + drop.getMaterial() + " x" + drop.getAmount());
        }

        Entity caughtEntity = event.getCaught();
        if (caughtEntity instanceof Item vanillaItem) {
            if (debugMode) {
                plugin.getLogger().info("  Removing vanilla item");
            }
            vanillaItem.remove();
        }

        if (!rewardService.deliverReward(player, drop)) {
            if (debugMode) {
                plugin.getLogger().warning("  Failed to deliver reward");
            }
            return;
        }

        rewardService.sendCatchMessage(player, drop);
        effectsService.spawnCatchEffects(player, hookLoc);

        if (plugin.getConfigManager().trackStatistics()) {
            plugin.getStatisticsManager().recordCatch(player, drop);
        }

        int xpAmount = rewardService.calculateExperience(drop);
        rewardService.giveExperience(player, xpAmount);
        effectsService.spawnExperienceEffects(player, xpAmount);

        if (debugMode) {
            plugin.getLogger().info("  SUCCESS - Custom drop given!");
            plugin.getLogger().info("========================================");
        }
    }

    private void handleReelIn(UUID hookId) {
        fishingService.endFishing(hookId);
    }

    private void handleFishingEnd(UUID hookId) {
        fishingService.endFishing(hookId);
    }

    private void handleCaughtEntity(UUID hookId) {
        fishingService.endFishing(hookId);
    }


    public void cleanupHooks() {
        fishingService.cleanupStaleHooks(plugin.getServer());
    }
}
