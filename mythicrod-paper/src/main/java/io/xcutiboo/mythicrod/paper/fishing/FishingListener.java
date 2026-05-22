package io.xcutiboo.mythicrod.paper.fishing;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.api.PlayerStatSnapshot;
import io.xcutiboo.mythicrod.api.Result;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.config.RewardDeliveryMode;
import io.xcutiboo.mythicrod.constants.MythicRodKeys;
import io.xcutiboo.mythicrod.constants.PermissionNodes;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.paper.api.PaperMythicRodAPI;
import io.xcutiboo.mythicrod.paper.events.MythicRodFishCatchEvent;
import io.xcutiboo.mythicrod.paper.events.MythicRodRewardRollEvent;
import io.xcutiboo.mythicrod.paper.events.MythicRodStatsUpdateEvent;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;
import io.xcutiboo.mythicrod.paper.item.PaperPlatformItem;
import io.xcutiboo.mythicrod.paper.item.RodFactory;
import io.xcutiboo.mythicrod.paper.platform.PaperItem;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.paper.util.ParticleOptions;
import io.xcutiboo.mythicrod.paper.util.StringFormatting;
import io.xcutiboo.mythicrod.text.MiniMessageMigrator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Fishing listener for replacing vanilla catches with MythicRod rewards.
 *
 * <p>Important invariants: do not store hook state globally, do not remove the
 * fishing hook to short-circuit vanilla mechanics, and keep Folia entity/item
 * mutation on the owning scheduler path.
 */
public class FishingListener implements Listener {
    private static final String TIER_BASIC = "basic";
    private static final String TIER_ADVANCED = "advanced";
    private static final String TIER_LEGENDARY = "legendary";

    private final MythicRod plugin;
    private final RodFactory rodFactory;

    public FishingListener(MythicRod plugin) {
        this.plugin = plugin;
        this.rodFactory = new RodFactory(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        try {
            if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
                return;
            }

            Player player = event.getPlayer();
            Entity caughtEntity = event.getCaught();

            if (!(caughtEntity instanceof Item caughtItem)) {
                return;
            }

            boolean debugMode = plugin.getConfigManager().isDebugMode();
            if (debugMode) {
                info(() -> "CAUGHT_FISH: " + player.getName() + " caught " + caughtItem.getItemStack().getType());
            }

            Location hookLoc;
            String biomeName;
            try {
                hookLoc = event.getHook().getLocation();
                if (hookLoc.getWorld() == null) {
                    if (debugMode) {
                        plugin.getLogger().warning("Fishing hook location was unavailable during custom catch handling");
                    }
                    return;
                }
                biomeName = hookLoc.getWorld().getComputedBiome(
                    hookLoc.getBlockX(),
                    hookLoc.getBlockY(),
                    hookLoc.getBlockZ()
                ).getKey().asString();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to resolve fishing hook biome", e);
                return;
            }

            PaperPlayer platformPlayer = new PaperPlayer(player);
            double rodLuckMultiplier = resolveRodLuckMultiplier(player, event.getHand());
            double baseWeight = plugin.getApiFacade().getBaseRewardWeight(platformPlayer, biomeName);
            MythicRodRewardRollEvent rollEvent = new MythicRodRewardRollEvent(player, biomeName, baseWeight);
            plugin.getServer().getPluginManager().callEvent(rollEvent);

            final double luckMultiplier = combineLuckMultipliers(rodLuckMultiplier, rollEvent.getLuckMultiplier());
            ItemStack customItem;
            CustomDrop drop;
            if (rollEvent.hasForcedDrop()) {
                drop = rollEvent.getForcedDrop();
                customItem = createItemStack(drop);
                if (debugMode) {
                    info(() -> "Drop forced by external plugin: "
                        + (drop != null ? drop.getIdentifier() : "null"));
                }
            } else {
                PaperMythicRodAPI.RewardResolution resolution = plugin.getApiFacade().resolveReward(
                    platformPlayer,
                    biomeName,
                    luckMultiplier
                );
                if (resolution == null) {
                    if (debugMode) {
                        plugin.getLogger().info("No MythicRod reward selected; preserving vanilla catch");
                    }
                    return;
                }

                drop = resolution.drop();
                customItem = resolution.isExternal()
                    ? unwrapPlatformItem(resolution.externalItem(), drop.getIdentifier())
                    : createItemStack(drop);
            }

            if (drop == null) {
                if (debugMode) {
                    plugin.getLogger().info("Reward roll did not return a drop; preserving vanilla catch");
                }
                return;
            }

            if (debugMode) {
                info(() -> "Selected drop: " + drop.getIdentifier() + " x" + drop.getAmount());
            }

            if (customItem == null || customItem.getType().isAir()) {
                warning(() -> "Resolved reward item was null or AIR for drop '" + drop.getIdentifier() + "'");
                return;
            }

            dispatchCustomDrop(player, caughtItem, customItem, drop, hookLoc);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Error processing fishing catch event", e);
        }
    }

    private double resolveRodLuckMultiplier(Player player, EquipmentSlot hand) {
        String tier = resolveEffectiveRodTier(player, hand);
        return plugin.getConfigManager().getRodLuckMultiplier(tier);
    }

    private String resolveEffectiveRodTier(Player player, EquipmentSlot hand) {
        String heldRodTier = resolveHeldRodTier(player, hand);
        if (isAllowedRodTier(player, heldRodTier)) {
            return normalizeRodTier(heldRodTier);
        }

        String selectedTier = plugin.getPlayerDataService() != null
            ? plugin.getPlayerDataService().getRodTier(player)
            : MythicRodKeys.DEFAULT_ROD_TIER;
        if (isAllowedRodTier(player, selectedTier)) {
            return normalizeRodTier(selectedTier);
        }

        return MythicRodKeys.DEFAULT_ROD_TIER;
    }

    private String resolveHeldRodTier(Player player, EquipmentSlot hand) {
        if (player == null || hand == null) {
            return null;
        }

        ItemStack usedItem = switch (hand) {
            case HAND -> player.getInventory().getItemInMainHand();
            case OFF_HAND -> player.getInventory().getItemInOffHand();
            default -> null;
        };
        if (!rodFactory.isCustomRod(usedItem)) {
            return null;
        }
        return rodFactory.getRodTier(usedItem);
    }

    private boolean isAllowedRodTier(Player player, String tier) {
        return switch (normalizeRodTier(tier)) {
            case TIER_ADVANCED -> player != null && player.hasPermission(PermissionNodes.ROD_ADVANCED);
            case TIER_LEGENDARY -> player != null && player.hasPermission(PermissionNodes.ROD_LEGENDARY);
            case TIER_BASIC -> true;
            default -> false;
        };
    }

    private String normalizeRodTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return MythicRodKeys.DEFAULT_ROD_TIER;
        }

        return switch (tier.trim().toLowerCase(Locale.ROOT)) {
            case TIER_ADVANCED -> TIER_ADVANCED;
            case TIER_LEGENDARY -> TIER_LEGENDARY;
            default -> MythicRodKeys.DEFAULT_ROD_TIER;
        };
    }

    private double combineLuckMultipliers(double rodMultiplier, double eventMultiplier) {
        double safeRodMultiplier = Double.isFinite(rodMultiplier) ? rodMultiplier : 1.0D;
        double safeEventMultiplier = Double.isFinite(eventMultiplier) ? eventMultiplier : 1.0D;
        return Math.clamp(safeRodMultiplier * safeEventMultiplier, 0.01D, 10.0D);
    }

    private void dispatchCustomDrop(Player player, Item caughtItem, ItemStack customItem, CustomDrop drop, Location hookLoc) {
        if (!plugin.isFoliaRuntime()) {
            giveCustomDropOnPlayerThread(player, caughtItem, customItem, drop, hookLoc);
            return;
        }

        try {
            if (caughtItem.getScheduler().run(plugin, foliaTask(() -> schedulePlayerOwnedDropDelivery(
                player,
                caughtItem,
                customItem,
                drop,
                hookLoc
            )), () -> schedulePlayerOwnedDropDelivery(player, caughtItem, customItem, drop, hookLoc)) == null) {
                schedulePlayerOwnedDropDelivery(player, caughtItem, customItem, drop, hookLoc);
            }
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to schedule caught item handling", e);
            schedulePlayerOwnedDropDelivery(player, caughtItem, customItem, drop, hookLoc);
        }
    }

    private void schedulePlayerOwnedDropDelivery(
        Player player,
        Item caughtItem,
        ItemStack customItem,
        CustomDrop drop,
        Location hookLoc
    ) {
        if (player == null) {
            return;
        }

        if (player.getScheduler().run(plugin, foliaTask(() -> {
            try {
                giveCustomDropOnPlayerThread(player, caughtItem, customItem, drop, hookLoc);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, e,
                    () -> "Error giving custom drop to " + player.getName());
            }
        }), null) == null && plugin.getConfigManager().isDebugMode()) {
            info(() -> "Player scheduler rejected drop delivery for " + player.getName());
        }
    }

    /**
     * Applies the final reward from the player's owner thread, or from the main
     * server thread on ordinary Paper. Keeping the Paper path immediate avoids a
     * client-visible flash of the vanilla item before MythicRod replaces it.
     */
    private void giveCustomDropOnPlayerThread(Player player, Item caughtItem, ItemStack customItem, CustomDrop drop, Location hookLoc) {
        if (player == null || !player.isOnline()) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Player went offline before drop delivery");
            }
            return;
        }

        MythicRodFishCatchEvent catchEvent = new MythicRodFishCatchEvent(player, drop, customItem);
        plugin.getServer().getPluginManager().callEvent(catchEvent);
        if (catchEvent.isCancelled()) {
            if (plugin.getConfigManager().isDebugMode()) {
                info(() -> "MythicRodFishCatchEvent cancelled for " + player.getName());
            }
            return;
        }

        final ItemStack finalItem = sanitizeRewardItem(catchEvent.getRewardItem(), drop.getIdentifier());
        if (finalItem == null || finalItem.getType().isAir()) {
            warning(() -> "Final reward item was null or air for drop '" + drop.getIdentifier() + "'");
            return;
        }

        RewardDeliveryMode deliveryMode = plugin.getConfigManager().getRewardDeliveryMode();
        Location feedbackLocation = resolveFeedbackLocation(player, hookLoc, deliveryMode);
        deliverConfiguredReward(player, caughtItem, finalItem, drop, feedbackLocation, deliveryMode);
    }

    private void deliverConfiguredReward(
        Player player,
        Item caughtItem,
        ItemStack rewardItem,
        CustomDrop drop,
        Location feedbackLocation,
        RewardDeliveryMode deliveryMode
    ) {
        switch (deliveryMode) {
            case VANILLA_RETRIEVE -> deliverViaVanillaRetrieve(player, caughtItem, rewardItem, drop, feedbackLocation);
            case INVENTORY -> removeCaughtItemThen(player, caughtItem, () -> {
                if (!deliverToInventory(player, rewardItem.clone())) {
                    return;
                }
                finalizeRewardDelivery(player, drop, feedbackLocation, rewardItem);
            });
            case DROP_AT_PLAYER -> removeCaughtItemThen(player, caughtItem, () -> {
                if (!dropAtPlayerLocation(player, rewardItem.clone())) {
                    return;
                }
                finalizeRewardDelivery(player, drop, feedbackLocation, rewardItem);
            });
        }
    }

    private void deliverViaVanillaRetrieve(
        Player player,
        Item caughtItem,
        ItemStack rewardItem,
        CustomDrop drop,
        Location feedbackLocation
    ) {
        Runnable playerDropFallback = () -> {
            Location fallbackLocation = player.getLocation();
            if (!dropAtPlayerLocation(player, rewardItem.clone())) {
                return;
            }
            finalizeRewardDelivery(player, drop, fallbackLocation, rewardItem);
        };

        if (caughtItem == null || caughtItem.isDead()) {
            playerDropFallback.run();
            return;
        }

        if (!plugin.isFoliaRuntime()) {
            try {
                caughtItem.setItemStack(rewardItem.clone());
                caughtItem.setOwner(player.getUniqueId());
                caughtItem.setThrower(player.getUniqueId());
                caughtItem.setPickupDelay(0);
                finalizeRewardDelivery(player, drop, feedbackLocation, rewardItem);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to convert caught item into custom reward", e);
                playerDropFallback.run();
            }
            return;
        }

        if (caughtItem.getScheduler().run(plugin, foliaTask(() -> {
            try {
                if (caughtItem.isDead()) {
                    dispatchToPlayerOrGlobal(player, playerDropFallback);
                    return;
                }

                caughtItem.setItemStack(rewardItem.clone());
                caughtItem.setOwner(player.getUniqueId());
                caughtItem.setThrower(player.getUniqueId());
                caughtItem.setPickupDelay(0);

                dispatchToPlayerOrGlobal(player, () -> finalizeRewardDelivery(player, drop, feedbackLocation, rewardItem));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to convert caught item into custom reward", e);
                dispatchToPlayerOrGlobal(player, playerDropFallback);
            }
        }), () -> dispatchToPlayerOrGlobal(player, playerDropFallback)) == null) {
            playerDropFallback.run();
        }
    }

    private void removeCaughtItemThen(Player player, Item caughtItem, Runnable continuation) {
        if (caughtItem == null || caughtItem.isDead()) {
            continuation.run();
            return;
        }

        if (!plugin.isFoliaRuntime()) {
            try {
                caughtItem.remove();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove vanilla caught item before custom delivery", e);
            }
            continuation.run();
            return;
        }

        if (caughtItem.getScheduler().run(plugin, foliaTask(() -> {
            try {
                if (!caughtItem.isDead()) {
                    caughtItem.remove();
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove vanilla caught item before custom delivery", e);
            }
            dispatchToPlayerOrGlobal(player, continuation);
        }), () -> dispatchToPlayerOrGlobal(player, continuation)) == null) {
            continuation.run();
        }
    }

    @SuppressWarnings("unused")
    private Consumer<ScheduledTask> foliaTask(Runnable action) {
        return task -> action.run();
    }

    private void dispatchToPlayerOrGlobal(Player player, Runnable action) {
        if (player != null && player.isOnline()) {
            plugin.getPlatformScheduler().runForPlayer(new PaperPlayer(player), action);
            return;
        }

        plugin.getPlatformScheduler().runGlobal(action);
    }

    private boolean deliverToInventory(Player player, ItemStack rewardItem) {
        try {
            Map<Integer, ItemStack> excess = player.getInventory().addItem(rewardItem);
            if (excess.isEmpty()) {
                return true;
            }

            excess.values().forEach(leftover -> {
                if (!dropAtPlayerLocation(player, leftover)) {
                    warning(() -> "Failed to deliver excess reward item for " + player.getName());
                }
            });
            return true;
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Error delivering drop to inventory", e);
            return false;
        }
    }

    private boolean dropAtPlayerLocation(Player player, ItemStack rewardItem) {
        try {
            Item droppedItem = player.getWorld().dropItemNaturally(player.getLocation(), rewardItem);
            droppedItem.setOwner(player.getUniqueId());
            droppedItem.setThrower(player.getUniqueId());
            droppedItem.setPickupDelay(0);
            return true;
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to drop reward at player location", e);
            return false;
        }
    }

    private void finalizeRewardDelivery(Player player, CustomDrop drop, Location rewardLocation, ItemStack deliveredItem) {
        recordCatchStatistics(player, drop);

        if (player == null || !player.isOnline()) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Reward delivered after player disconnected; skipping online-only feedback");
            }
            return;
        }

        sendCatchMessage(player, drop, deliveredItem);
        spawnCatchEffects(player, rewardLocation, drop);

        giveExperience(player, drop);

        if (plugin.getConfigManager().isDebugMode()) {
            info(() -> "SUCCESS: Custom drop given to " + player.getName());
        }
    }

    private void recordCatchStatistics(Player player, CustomDrop drop) {
        if (player == null || drop == null) {
            return;
        }
        if (!plugin.getConfigManager().trackStatistics()) {
            return;
        }
        try {
            plugin.getStatisticsManager().recordCatch(player.getUniqueId(), drop.getTier());
            plugin.getStatisticsManager().recordRodUse(player.getUniqueId(), resolveRodTierForStats(player));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to record catch statistics", e);
            return;
        }
        fireStatsUpdateEvent(player, drop);
    }

    private String resolveRodTierForStats(Player player) {
        String mainHandTier = resolveEffectiveRodTier(player, EquipmentSlot.HAND);
        if (mainHandTier != null && !mainHandTier.isBlank()) {
            return mainHandTier;
        }
        return MythicRodKeys.DEFAULT_ROD_TIER;
    }

    private void fireStatsUpdateEvent(Player player, CustomDrop drop) {
        try {
            PlayerStatSnapshot snapshot = plugin.getApiFacade().snapshotFor(player.getUniqueId());
            if (snapshot == null) {
                return;
            }
            plugin.getServer().getPluginManager().callEvent(
                new MythicRodStatsUpdateEvent(player.getUniqueId(), drop.getTier(), snapshot));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to fire MythicRodStatsUpdateEvent", e);
        }
    }

    private Location resolveFeedbackLocation(Player player, Location hookLocation, RewardDeliveryMode deliveryMode) {
        if (deliveryMode == RewardDeliveryMode.VANILLA_RETRIEVE && hookLocation != null && hookLocation.getWorld() != null) {
            return hookLocation.clone();
        }
        return player.getLocation();
    }

    private ItemStack sanitizeRewardItem(ItemStack rewardItem, String rewardId) {
        if (rewardItem == null || rewardItem.getType().isAir()) {
            return null;
        }

        ItemStack sanitized = rewardItem.clone();
        int maxStackSize = Math.max(1, sanitized.getMaxStackSize());
        int amount = sanitized.getAmount();
        int safeAmount = Math.clamp(amount, 1, maxStackSize);
        if (safeAmount != amount) {
            warning(() -> "Reward item amount " + amount
                + " out of bounds for '" + rewardId + "', clamped to " + safeAmount);
            sanitized.setAmount(safeAmount);
        }
        return sanitized;
    }

    private ItemStack createItemStack(CustomDrop drop) {
        if (drop == null) {
            plugin.getLogger().warning("Drop was null while building a reward item; using COD");
            ItemStack fallback = ItemStack.of(Material.COD);
            fallback.setAmount(1);
            return fallback;
        }

        String identifier = drop.getIdentifier();

        if (identifier == null || identifier.isEmpty()) {
            plugin.getLogger().warning("Drop identifier was empty while building a reward item; using COD");
            ItemStack fallback = ItemStack.of(Material.COD);
            fallback.setAmount(1);
            return fallback;
        }

        ItemStack baseItem = createBaseItem(identifier);
        int maxStackSize = Math.max(1, baseItem.getMaxStackSize());
        int dropAmount = drop.getAmount();
        int validAmount = Math.clamp(dropAmount, 1, maxStackSize);
        if (dropAmount != validAmount) {
            warning(() -> "Drop amount " + dropAmount
                + " out of bounds for " + identifier + ", clamped to " + validAmount);
        }

        ItemBuilder itemBuilder = ItemBuilder.from(baseItem).amount(validAmount);

        if (drop.getCustomName() != null && !drop.getCustomName().isEmpty()) {
            try {
                itemBuilder.name(drop.getCustomName());
            } catch (Exception e) {
                warning(() -> "Failed to parse custom reward name for " + identifier + ": " + e.getMessage());
            }
        }

        if (drop.getLore() != null && !drop.getLore().isEmpty()) {
            try {
                itemBuilder.lore(drop.getLore());
            } catch (Exception e) {
                warning(() -> "Failed to parse reward lore for " + identifier + ": " + e.getMessage());
            }
        }

        if (drop.getCustomModelData() > 0) {
            itemBuilder.customModelData(drop.getCustomModelData());
        }

        if (!drop.getEnchantments().isEmpty()) {
            itemBuilder.enchantments(drop.getEnchantments());
        }

        if (drop.isGlowing()) {
            itemBuilder.glow();
        }

        ItemStack item = itemBuilder.build();
        applyItemFlags(item, drop.getItemFlags(), identifier);
        return item;
    }

    private ItemStack createBaseItem(String identifier) {
        Result<PlatformItem> createResult = plugin.getPlatformServer().getItemFactory().createItem(identifier, 1);
        if (!createResult.isSuccess()) {
            warning(() -> "Failed to create drop item '" + identifier
                + "': " + createResult.getError() + ". Falling back to COD.");
            return ItemStack.of(Material.COD);
        }

        ItemStack baseItem = unwrapPlatformItem(createResult.getValue(), identifier);
        if (baseItem != null) {
            return baseItem;
        }

        warning(() -> "Unsupported platform item implementation for '"
            + identifier + "'. Falling back to COD.");
        return ItemStack.of(Material.COD);
    }

    private ItemStack unwrapPlatformItem(PlatformItem platformItem, String identifier) {
        if (platformItem == null) {
            return null;
        }

        if (platformItem instanceof PaperPlatformItem paperPlatformItem) {
            return paperPlatformItem.getItemStack().clone();
        }
        if (platformItem instanceof PaperItem paperItem) {
            return paperItem.getBukkitItem();
        }

        warning(() -> "Unsupported platform item implementation for '"
            + identifier + "': " + platformItem.getClass().getName());
        return null;
    }

    private void applyItemFlags(ItemStack item, List<String> itemFlags, String identifier) {
        if (itemFlags == null || itemFlags.isEmpty()) {
            return;
        }

        item.editMeta(meta -> {
            for (String flagName : itemFlags) {
                if (flagName == null || flagName.isBlank()) {
                    continue;
                }

                try {
                    meta.addItemFlags(ItemFlag.valueOf(flagName.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException _) {
                    warning(() -> "Unknown item flag '" + flagName
                        + "' for drop '" + identifier + "'");
                }
            }
        });
    }

    private void spawnCatchEffects(Player player, Location hookLocation, CustomDrop drop) {
        Location effectLocation = hookLocation != null && hookLocation.getWorld() != null
            ? hookLocation
            : player.getLocation();
        boolean debugMode = plugin.getConfigManager().isDebugMode();
        int weight = drop.getWeight();

        if (shouldShowParticles(player)) {
            spawnRarityParticles(player, effectLocation, weight);
        }

        if (plugin.getConfigManager().useSounds()) {
            playRaritySounds(player, effectLocation, weight);
        }

        if (debugMode) {
            info(() -> "Catch effects spawned for " + player.getName() + " (weight " + weight + ")");
        }
    }

    private boolean shouldShowParticles(Player player) {
        return plugin.getConfigManager().useParticles()
            && player != null
            && player.isOnline()
            && (plugin.getPlayerDataService() == null
                || !plugin.getPlayerDataService().hasReducedEffects(player));
    }

    private void spawnRarityParticles(Player player, Location hookLocation, int weight) {
        spawnParticle(player, hookLocation, plugin.getConfigManager().getCatchParticle(), Particle.SPLASH, 30, 0.3D, 0.15D);
        spawnParticle(player, hookLocation.clone().add(0.0D, 0.3D, 0.0D), plugin.getConfigManager().getBubbleParticle(), Particle.BUBBLE_POP, 15, 0.2D, 0.05D);

        if (weight <= 1) {
            Location playerLoc = player.getLocation().add(0.0D, 2.0D, 0.0D);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, playerLoc, 50, 1.0D, 0.5D, 1.0D, 0.3D);
            player.spawnParticle(Particle.END_ROD, playerLoc, 30, 0.8D, 0.3D, 0.8D, 0.1D);
            player.spawnParticle(Particle.HAPPY_VILLAGER, playerLoc, 20, 0.5D, 0.3D, 0.5D, 0.1D);
        } else if (weight <= 5) {
            Location playerLoc = player.getLocation().add(0.0D, 1.5D, 0.0D);
            player.spawnParticle(Particle.HAPPY_VILLAGER, playerLoc, 15, 0.4D, 0.3D, 0.4D, 0.05D);
            player.spawnParticle(Particle.END_ROD, hookLocation, 10, 0.3D, 0.3D, 0.3D, 0.05D);
        } else if (weight <= 15) {
            spawnParticle(player, player.getLocation().add(0.0D, 1.5D, 0.0D), plugin.getConfigManager().getSuccessParticle(), Particle.HAPPY_VILLAGER, 10, 0.35D, 0.04D);
        } else {
            spawnParticle(player, player.getLocation().add(0.0D, 1.5D, 0.0D), plugin.getConfigManager().getSuccessParticle(), Particle.HAPPY_VILLAGER, 5, 0.3D, 0.02D);
        }
    }

    private void playRaritySounds(Player player, Location hookLocation, int weight) {
        player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 0.8F, 1.0F);
        player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 0.6F, 1.1F);

        if (weight <= 1) {
            playLegendarySounds(player, hookLocation);
        } else if (weight <= 5) {
            playRareSounds(player, hookLocation);
        } else if (weight <= 15) {
            playUncommonSounds(player, hookLocation);
        }
    }

    private void playLegendarySounds(Player player, Location hookLocation) {
        player.playSound(hookLocation, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.7F, 1.5F);
        player.playSound(hookLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.3F, 1.8F);

        plugin.getPlatformScheduler().runForPlayerDelayed(
            new PaperPlayer(player),
            () -> {
                if (player.isOnline()) {
                    player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8F, 1.0F);
                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6F, 1.2F);
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.5F, 2.0F);
                }
            },
            5L
        );
    }

    private void playRareSounds(Player player, Location hookLocation) {
        player.playSound(hookLocation, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.6F, 2.0F);

        plugin.getPlatformScheduler().runForPlayerDelayed(
            new PaperPlayer(player),
            () -> {
                if (player.isOnline()) {
                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5F, 1.5F);
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.4F, 1.5F);
                }
            },
            4L
        );
    }

    private void playUncommonSounds(Player player, Location hookLocation) {
        player.playSound(hookLocation, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.4F, 1.8F);

        plugin.getPlatformScheduler().runForPlayerDelayed(
            new PaperPlayer(player),
            () -> {
                if (player.isOnline()) {
                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.4F, 1.2F);
                }
            },
            3L
        );
    }

    private void spawnParticle(Player player, Location location, String particleName, Particle fallback, int count, double offset, double extra) {
        try {
            Particle particle = Particle.valueOf(particleName);
            Object data = ParticleOptions.defaultDataFor(particle, location);
            if (data == null) {
                player.spawnParticle(particle, location, count, offset, offset, offset, extra);
            } else {
                player.spawnParticle(particle, location, count, offset, offset, offset, extra, data);
            }
        } catch (IllegalArgumentException | IllegalStateException _) {
            player.spawnParticle(fallback, location, count, offset, offset, offset, extra);
        }
    }

    private void sendCatchMessage(Player player, CustomDrop drop, ItemStack deliveredItem) {
        try {
            if (player == null || drop == null) {
                plugin.getLogger().warning("Cannot send catch message without a player and drop");
                return;
            }

            int deliveredAmount = deliveredItem != null && !deliveredItem.getType().isAir() && deliveredItem.getAmount() > 0
                ? deliveredItem.getAmount()
                : drop.getAmount();
            String amount = String.valueOf(deliveredAmount);
            String deliveredIdentifier = deliveredItem != null && !deliveredItem.getType().isAir()
                ? deliveredItem.getType().name()
                : drop.getIdentifier();
            int weight = drop.getWeight();

            // Drop names come from plugin-controlled config/provider data, so they should
            // preserve formatting in catch messages instead of leaking raw MiniMessage tags.
            TagResolver resolver = TagResolver.resolver(
                Placeholder.component(
                    "item",
                    resolveCatchItemNameComponent(resolveDeliveredItemDisplayName(deliveredItem), drop.getCustomName(), deliveredIdentifier)
                ),
                Placeholder.unparsed("amount", amount)
            );

            String template;
            if (weight <= 1) {
                template = plugin.getConfigManager().getMsgLegendary();
            } else if (weight <= 5) {
                template = plugin.getConfigManager().getMsgRare();
            } else if (weight <= 15) {
                template = plugin.getConfigManager().getMsgUncommon();
            } else {
                template = plugin.getConfigManager().getMsgCommon();
            }

            if (template == null || template.isEmpty()) {
                plugin.getLogger().warning("Catch message template was empty; using fallback text");
                template = "You caught <yellow><bold>{amount}x {item}</bold></yellow>!";
            }

            Component message = MiniMessage.miniMessage().deserialize(
                normalizeCatchTemplate(MiniMessageMigrator.migrateWithSerializer(template)),
                resolver
            );
            player.sendMessage(message);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send catch message", e);
        }
    }

    private void info(Supplier<String> messageSupplier) {
        plugin.getLogger().log(Level.INFO, messageSupplier);
    }

    private void warning(Supplier<String> messageSupplier) {
        plugin.getLogger().log(Level.WARNING, messageSupplier);
    }

    private static Component resolveDeliveredItemDisplayName(ItemStack deliveredItem) {
        if (deliveredItem == null || deliveredItem.getType().isAir()) {
            return null;
        }

        ItemMeta itemMeta = deliveredItem.getItemMeta();
        if (itemMeta == null) {
            return null;
        }

        return itemMeta.displayName();
    }

    static Component resolveCatchItemNameComponent(String customName, String identifier) {
        return resolveCatchItemNameComponent(null, customName, identifier);
    }

    static Component resolveCatchItemNameComponent(Component deliveredDisplayName, String customName, String identifier) {
        if (deliveredDisplayName != null) {
            return deliveredDisplayName;
        }

        if (customName == null || customName.isBlank()) {
            return Component.text(StringFormatting.formatMaterialName(identifier));
        }

        try {
            String migratedName = MiniMessageMigrator.migrateWithSerializer(customName);
            return MiniMessage.miniMessage().deserialize(migratedName);
        } catch (Exception _) {
            return Component.text(stripMiniMessageTags(customName));
        }
    }

    private String normalizeCatchTemplate(String template) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        return template
            .replace("{item}", "<item>")
            .replace("{amount}", "<amount>")
            .replace("%item%", "<item>")
            .replace("%amount%", "<amount>");
    }

    private static String stripMiniMessageTags(String input) {
        if (input == null || input.isEmpty()) {
            return "Unknown";
        }
        return input.replaceAll("<[^>]+>", "").trim();
    }

    private void giveExperience(Player player, CustomDrop drop) {
        try {
            if (player == null || !player.isOnline()) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Cannot give XP: player is null or offline");
                }
                return;
            }

            byte xpAmount;
            int weight = drop.getWeight();
            if (weight <= 1) {
                xpAmount = 6;
            } else if (weight <= 5) {
                xpAmount = 5;
            } else if (weight <= 15) {
                xpAmount = 3;
            } else if (weight <= 30) {
                xpAmount = 2;
            } else {
                xpAmount = 1;
            }

            player.giveExp(xpAmount);
            if (shouldShowParticles(player)) {
                spawnParticle(
                    player,
                    player.getLocation().add(0.0D, 1.2D, 0.0D),
                    plugin.getConfigManager().getXpParticle(),
                    Particle.HAPPY_VILLAGER,
                    xpAmount * 2,
                    0.2D,
                    0.05D
                );
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error giving experience", e);
        }
    }
}
