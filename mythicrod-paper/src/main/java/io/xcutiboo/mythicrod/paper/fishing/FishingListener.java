package io.xcutiboo.mythicrod.paper.fishing;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.paper.events.MythicRodFishCatchEvent;
import io.xcutiboo.mythicrod.paper.events.MythicRodRewardRollEvent;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
import org.bukkit.inventory.ItemStack;

/**
 * FishingListener using modern Paper 1.21.11 best practices.
 * 
 * CRITICAL: Following NotebookLM guidance:
 * - NO global ConcurrentHashMap for hook tracking (hides threading issues, memory leaks)
 * - NO hook.remove() (breaks vanilla mechanics: rod durability, XP)
 * - Use EntityScheduler for thread-safe item modifications
 * - Remove caught item entity, let vanilla handle hook naturally
 */
public class FishingListener implements Listener {
    private final MythicRod plugin;

    public FishingListener(MythicRod plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle fishing events with HIGHEST priority for final say on drops.
     * Thread-safe: Uses EntityScheduler for all entity modifications.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        // Only process successful catches
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        Player player = event.getPlayer();
        Entity caughtEntity = event.getCaught();
        
        // Safety: Ensure they caught an item, not a mob/player
        if (!(caughtEntity instanceof Item caughtItem)) {
            return;
        }

        boolean debugMode = plugin.getConfigManager().isDebugMode();
        if (debugMode) {
            plugin.getLogger().info("CAUGHT_FISH: " + player.getName() + " caught " + caughtItem.getItemStack().getType());
        }

        // Get biome for biome-specific drops
        Location hookLoc = event.getHook().getLocation();
        String biomeName = hookLoc.getWorld().getComputedBiome(
            hookLoc.getBlockX(), 
            hookLoc.getBlockY(), 
            hookLoc.getBlockZ()
        ).getKey().asString();

        // ── Cycle-2 event: MythicRodRewardRollEvent ──────────────────────────
        // Fired before the weighted roll so external plugins can:
        //   • Adjust luck via setLuckMultiplier()
        //   • Force a specific drop via forceDrop(CustomDrop)
        // Category passed as biomeName for now; the luck multiplier is a future
        // DropManager hook — stored in the event for API consumers.
        MythicRodRewardRollEvent rollEvent = new MythicRodRewardRollEvent(player, biomeName, 100.0);
        plugin.getServer().getPluginManager().callEvent(rollEvent);

        // Honour any forced drop from external plugins; otherwise run the
        // luck-modified weighted roll using the multiplier from the event.
        final double luckMultiplier = rollEvent.getLuckMultiplier();
        CustomDrop drop;
        if (rollEvent.hasForcedDrop()) {
            drop = rollEvent.getForcedDrop();
            if (debugMode) {
                plugin.getLogger().info("Drop forced by external plugin: "
                        + (drop != null ? drop.getIdentifier() : "null"));
            }
        } else {
            drop = plugin.getDropManager().getRandomDrop(
                    new PaperPlayer(player), biomeName, luckMultiplier);
        }

        if (drop == null) {
            if (debugMode) {
                plugin.getLogger().info("No custom drop - letting vanilla handle");
            }
            return;
        }

        if (debugMode) {
            plugin.getLogger().info("Selected drop: " + drop.getIdentifier() + " x" + drop.getAmount());
        }

        // Create the custom item
        ItemStack customItem = createItemStack(drop);

        // Thread-Safe: Schedule on the caught item's EntityScheduler
        // This ensures atomic execution on the correct region thread
        caughtItem.getScheduler().run(plugin, (task) -> {
            // CRITICAL: Remove the vanilla item entity so it can't be picked up
            // This prevents double-drops without needing flags or maps
            caughtItem.remove();

            // Now give the custom item to the player
            giveCustomDrop(player, customItem, drop, hookLoc);
        }, null);
    }

    /**
     * Give custom drop to player - scheduled on player's EntityScheduler
     * for thread safety in Folia environments.
     */
    private void giveCustomDrop(Player player, ItemStack customItem, CustomDrop drop, Location hookLoc) {
        player.getScheduler().run(plugin, (task) -> {
            // ── Cycle-2 event: MythicRodFishCatchEvent ──────────────────────
            // Fired on the player's region thread (Folia-safe).
            // • Cancelling prevents MythicRod from applying the custom drop.
            // • External plugins may replace the reward item via setRewardItem().
            MythicRodFishCatchEvent catchEvent =
                    new MythicRodFishCatchEvent(player, drop, customItem);
            plugin.getServer().getPluginManager().callEvent(catchEvent);
            if (catchEvent.isCancelled()) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("MythicRodFishCatchEvent cancelled for " + player.getName());
                }
                return; // Plugin rejected this reward
            }
            // Use the item as (possibly modified) by event listeners
            final ItemStack finalItem = catchEvent.getRewardItem();

            // Check if drop-to-inventory is enabled
            if (plugin.getConfigManager().dropToInventory()) {
                // Add directly to inventory, drop excess on ground
                Map<Integer, ItemStack> excess = player.getInventory().addItem(finalItem);
                if (!excess.isEmpty()) {
                    excess.values().forEach(leftover ->
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover)
                    );
                }
            } else {
                // Physical drop at hook location (traditional fly-to-player)
                player.getWorld().dropItemNaturally(hookLoc, finalItem);
            }

            // Send catch message
            sendCatchMessage(player, drop);

            // Spawn effects with rarity context
            spawnCatchEffects(player, hookLoc, drop);

            // Track statistics
            if (plugin.getConfigManager().trackStatistics()) {
                plugin.getStatisticsManager().recordCatch(player.getUniqueId(), getCategoryFromDrop(drop));
            }

            // Give experience
            giveExperience(player, drop);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("SUCCESS: Custom drop given to " + player.getName());
            }
        }, null);
    }

    /**
     * Create ItemStack from CustomDrop using modern DataComponent API.
     */
    private ItemStack createItemStack(CustomDrop drop) {
        String identifier = drop.getIdentifier();
        
        // Modern Material resolution using Registry
        String formattedKey = identifier.toLowerCase(java.util.Locale.ROOT);
        if (!formattedKey.contains(":")) {
            formattedKey = "minecraft:" + formattedKey;
        }
        
        Material material = org.bukkit.Registry.MATERIAL.get(org.bukkit.NamespacedKey.fromString(formattedKey));
        if (material == null) {
            plugin.getLogger().warning("Invalid material: " + identifier + ", using COD");
            material = Material.COD;
        }
        
        ItemStack item = ItemStack.of(material);
        item.setAmount(drop.getAmount());
        
        // Use DataComponent API for custom name
        if (drop.getCustomName() != null && !drop.getCustomName().isEmpty()) {
            Component nameComponent = MiniMessage.miniMessage().deserialize(drop.getCustomName());
            item.setData(DataComponentTypes.ITEM_NAME, nameComponent);
        }
        
        // Use DataComponent API for lore
        if (drop.getLore() != null && !drop.getLore().isEmpty()) {
            List<Component> loreComponents = drop.getLore().stream()
                .map(line -> MiniMessage.miniMessage().deserialize(line))
                .collect(Collectors.toList());
            item.lore(loreComponents);
        }
        
        return item;
    }

    private void spawnCatchEffects(Player player, Location hookLocation, CustomDrop drop) {
        boolean debugMode = plugin.getConfigManager().isDebugMode();
        
        // Spawn particles based on rarity
        if (plugin.getConfigManager().useParticles()) {
            spawnRarityParticles(player, hookLocation, drop.getChance());
        }

        // Play contextual sounds based on drop rarity
        if (plugin.getConfigManager().useSounds()) {
            playRaritySounds(player, hookLocation, drop.getChance());
        }
        
        if (debugMode) {
            plugin.getLogger().info("Effects spawned for " + player.getName() + " (rarity: " + drop.getChance() + ")");
        }
    }
    
    /**
     * Spawn particles appropriate for the drop rarity.
     * Common: Basic splash
     * Rare: Enhanced with totem particles
     * Legendary: Celebration with multiple particle types
     */
    private void spawnRarityParticles(Player player, Location hookLocation, int chance) {
        // Base water splash at hook location
        spawnParticle(player, hookLocation, plugin.getConfigManager().getCatchParticle(), Particle.SPLASH, 30, 0.3D, 0.15D);
        spawnParticle(player, hookLocation.clone().add(0.0D, 0.3D, 0.0D), plugin.getConfigManager().getBubbleParticle(), Particle.BUBBLE_POP, 15, 0.2D, 0.05D);
        
        // Rarity-based enhancements
        if (chance <= 1) {
            // Legendary - dramatic celebration
            Location playerLoc = player.getLocation().add(0.0D, 2.0D, 0.0D);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, playerLoc, 50, 1.0D, 0.5D, 1.0D, 0.3D);
            player.spawnParticle(Particle.END_ROD, playerLoc, 30, 0.8D, 0.3D, 0.8D, 0.1D);
            player.spawnParticle(Particle.HAPPY_VILLAGER, playerLoc, 20, 0.5D, 0.3D, 0.5D, 0.1D);
        } else if (chance <= 5) {
            // Rare - enhanced success particles
            Location playerLoc = player.getLocation().add(0.0D, 1.5D, 0.0D);
            player.spawnParticle(Particle.HAPPY_VILLAGER, playerLoc, 15, 0.4D, 0.3D, 0.4D, 0.05D);
            player.spawnParticle(Particle.END_ROD, hookLocation, 10, 0.3D, 0.3D, 0.3D, 0.05D);
        } else {
            // Common/Uncommon - basic success particle
            spawnParticle(player, player.getLocation().add(0.0D, 1.5D, 0.0D), plugin.getConfigManager().getSuccessParticle(), Particle.HAPPY_VILLAGER, 5, 0.3D, 0.02D);
        }
    }
    
    /**
     * Play sounds appropriate for the drop rarity.
     * Uses SoundCategory.PLAYERS for proper audio mixing.
     */
    private void playRaritySounds(Player player, Location hookLocation, int chance) {
        // Base fishing sounds at hook location (spatial audio)
        player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 0.8F, 1.0F);
        player.playSound(hookLocation, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 0.6F, 1.1F);
        
        if (chance <= 1) {
            // Legendary catch - epic celebration sequence
            playLegendarySounds(player, hookLocation);
        } else if (chance <= 5) {
            // Rare catch - enhanced rewarding sounds
            playRareSounds(player, hookLocation);
        } else if (chance <= 15) {
            // Uncommon - satisfying catch sound
            playUncommonSounds(player, hookLocation);
        } else {
            // Common - simple catch sound
            playCommonSounds(player, hookLocation);
        }
    }
    
    private void playLegendarySounds(Player player, Location hookLocation) {
        // Immediate impact sounds at hook
        player.playSound(hookLocation, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.7F, 1.5F);
        player.playSound(hookLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.3F, 1.8F);
        
        // Delayed epic celebration at player location
        plugin.getPlatformScheduler().runAtLocationDelayed(
            io.xcutiboo.mythicrod.paper.platform.PaperLocation.fromBukkit(player.getLocation()),
            () -> {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8F, 1.0F);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6F, 1.2F);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.5F, 2.0F);
                }
            },
            5L
        );
    }
    
    private void playRareSounds(Player player, Location hookLocation) {
        // Impact sound at hook
        player.playSound(hookLocation, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.6F, 2.0F);
        
        // Rewarding chime at player location
        plugin.getPlatformScheduler().runAtLocationDelayed(
            io.xcutiboo.mythicrod.paper.platform.PaperLocation.fromBukkit(player.getLocation()),
            () -> {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5F, 1.5F);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.4F, 1.5F);
                }
            },
            4L
        );
    }
    
    private void playUncommonSounds(Player player, Location hookLocation) {
        // Satisfying click at hook
        player.playSound(hookLocation, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.4F, 1.8F);
        
        // Subtle reward at player location
        plugin.getPlatformScheduler().runAtLocationDelayed(
            io.xcutiboo.mythicrod.paper.platform.PaperLocation.fromBukkit(player.getLocation()),
            () -> {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.4F, 1.2F);
                }
            },
            3L
        );
    }
    
    private void playCommonSounds(Player player, Location hookLocation) {
        // Simple water splash only - no additional sounds
        // This keeps common catches subtle and non-intrusive
    }
    
    private void spawnParticle(Player player, Location location, String particleName, Particle fallback, int count, double offset, double extra) {
        try {
            Particle particle = Particle.valueOf(particleName);
            player.spawnParticle(particle, location, count, offset, offset, offset, extra);
        } catch (IllegalArgumentException e) {
            player.spawnParticle(fallback, location, count, offset, offset, offset, extra);
        }
    }

    /**
     * Sends the per-rarity catch message to the player.
     *
     * <p>HIGH-007 FIX: Previously used hardcoded English MiniMessage strings, completely
     * bypassing the user-configurable templates in {@code config.yml} and breaking
     * non-English locales.  Now delegates to
     * {@link io.xcutiboo.mythicrod.config.ConfigManager#getMsgLegendary()} etc.,
     * which are populated from {@code messages.catch.*} at load/reload time.
     */
    private void sendCatchMessage(Player player, CustomDrop drop) {
        String itemName = drop.getCustomName() != null ? drop.getCustomName() : formatMaterialName(drop.getIdentifier());
        String amount = String.valueOf(drop.getAmount());

        // Use Placeholder.unparsed() to prevent MiniMessage injection from item names
        TagResolver resolver = TagResolver.resolver(
            Placeholder.unparsed("item",   itemName),
            Placeholder.unparsed("amount", amount)
        );

        // Resolve template from ConfigManager (user-configurable, reload-safe)
        String template;
        if (drop.getChance() <= 1) {
            template = plugin.getConfigManager().getMsgLegendary();
        } else if (drop.getChance() <= 5) {
            template = plugin.getConfigManager().getMsgRare();
        } else if (drop.getChance() <= 15) {
            template = plugin.getConfigManager().getMsgUncommon();
        } else {
            template = plugin.getConfigManager().getMsgCommon();
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize(template, resolver));
    }

    private String formatMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return "Unknown";
        }
        String[] words = materialName.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) continue;
            if (i > 0) result.append(" ");
            result.append(Character.toUpperCase(words[i].charAt(0)));
            if (words[i].length() > 1) {
                result.append(words[i].substring(1));
            }
        }
        return result.toString();
    }

    private void giveExperience(Player player, CustomDrop drop) {
        byte xpAmount;
        if (drop.getChance() <= 1) {
            xpAmount = 6;
        } else if (drop.getChance() <= 5) {
            xpAmount = 5;
        } else if (drop.getChance() <= 15) {
            xpAmount = 3;
        } else if (drop.getChance() <= 30) {
            xpAmount = 2;
        } else {
            xpAmount = 1;
        }

        player.giveExp(xpAmount);
        if (plugin.getConfigManager().useParticles()) {
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0D, 1.2D, 0.0D), xpAmount * 2, 0.2D, 0.3D, 0.2D, 0.05D);
        }
    }

    /**
     * Maps a drop's chance value to a tier category string consumed by
     * {@link io.xcutiboo.mythicrod.metrics.StatisticsManager#recordCatch(java.util.UUID, String)}.
     *
     * <p>Thresholds mirror those used in {@link #sendCatchMessage} and
     * {@link #spawnRarityParticles} so category names are consistent.
     *
     * @param drop The resolved custom drop. Never null.
     * @return A lower-case tier string: {@code "legendary"}, {@code "rare"},
     *         {@code "uncommon"}, or {@code "common"}.
     */
    private String getCategoryFromDrop(CustomDrop drop) {
        int chance = drop.getChance();
        if (chance <= 1)  return "legendary";
        if (chance <= 5)  return "rare";
        if (chance <= 15) return "uncommon";
        return "common";
    }

    /**
     * Returns 0 - active hook tracking removed per NotebookLM guidance.
     * Zero global state prevents memory leaks and threading issues.
     */
    public int getActiveFishingCount() {
        return 0;
    }
}
