package com.mythicrod.mythicRod;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * MythicRod - Custom fishing plugin that replaces vanilla drops with configurable items
 *
 * Features:
 * - Fully customizable drop table with configurable chances and amounts
 * - Prevents exploit of getting multiple items from the same fishing hook
 * - Easy to set up and use
 */
public final class MythicRod extends JavaPlugin implements Listener {

    // Core variables
    private List<CustomDrop> customDrops = new ArrayList<>();
    private Random random = new Random();
    private File configFile;
    private FileConfiguration config;

    // Anti-abuse system: Track which hooks have already given rewards
    private Map<FishHook, Boolean> rewardedHooks = new HashMap<>();

    /**
     * Plugin startup logic
     */
    @Override
    public void onEnable() {
        // Initialize config
        configFile = new File(getDataFolder(), "config.yml");
        saveDefaultConfig();
        onLoad();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Load config and drops
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadCustomDrops();

        getLogger().info("✨ MythicRod has been enabled! ✨");
    }

    /**
     * Plugin shutdown logic
     */
    @Override
    public void onDisable() {
        getLogger().info("⚡ MythicRod has been disabled! ⚡");
    }

    /**
     * Loads custom drops from config.yml
     * Creates default drops if none exist
     */
    public void loadCustomDrops() {
        customDrops.clear();

        // Set default drops if none exist
        if (!config.contains("drops")) {
            List<String> defaultDrops = new ArrayList<>();
            defaultDrops.add("DIAMOND,5,10");       // Rare, but valuable
            defaultDrops.add("IRON_INGOT,30,1");    // Common
            defaultDrops.add("GOLD_INGOT,20,1");    // Uncommon
            defaultDrops.add("EMERALD,15,1");       // Uncommon
            defaultDrops.add("NETHERITE_INGOT,1,1"); // Very rare

            config.set("drops", defaultDrops);
            saveConfig();
        }

        // Load drops from config
        List<String> drops = config.getStringList("drops");

        for (String drop : drops) {
            String[] parts = drop.split(",");
            if (parts.length >= 3) {
                try {
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int chance = Integer.parseInt(parts[1]);
                    int amount = Integer.parseInt(parts[2]);

                    customDrops.add(new CustomDrop(material, chance, amount));
                } catch (Exception e) {
                    getLogger().warning("⚠️ Invalid Drop Configuration: " + drop);
                }
            }
        }

        getLogger().info("🎣 Loaded " + customDrops.size() + " custom fishing drops!");
    }

    /**
     * Handles all fishing events
     * Prevents multiple rewards from the same hook
     */
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        FishHook hook = event.getHook();

        // CASE 1: Player casts fishing rod
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            rewardedHooks.put(hook, false);
            return;
        }

        // CASE 2: Player reels back with no catch or failed attempt
        if (event.getState() == PlayerFishEvent.State.REEL_IN ||
                event.getState() == PlayerFishEvent.State.IN_GROUND ||
                event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT) {
            rewardedHooks.remove(hook);
            return;
        }

        // CASE 3: Player caught a fish
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // Anti-abuse: Check if this hook already gave a reward
            if (rewardedHooks.containsKey(hook) && rewardedHooks.get(hook)) {
                event.setCancelled(true);
                return;
            }

            // Mark this hook as having given a reward
            rewardedHooks.put(hook, true);

            // Cancel vanilla fish and give custom drop
            event.setCancelled(true);
            ItemStack drop = getRandomDrop();

            if (drop != null) {
                // Create item at hook location
                Item item = event.getPlayer().getWorld().dropItem(
                        event.getHook().getLocation(),
                        drop
                );

                // Make item fly toward player like vanilla fishing
                item.setVelocity(event.getPlayer().getLocation().subtract(
                        event.getHook().getLocation()).toVector().multiply(0.1));
            }
        }

        // CASE 4: Fishing is complete (caught entity or fish)
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY ||
                event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // Clean up tracking for non-fish catches
            if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
                rewardedHooks.remove(hook);
            }
        }
    }

    /**
     * Selects a random drop based on configured chances
     * @return ItemStack with the selected drop or null if no drops available
     */
    private ItemStack getRandomDrop() {
        // Calculate total chance weight
        int totalChance = 0;
        for (CustomDrop drop : customDrops) {
            totalChance += drop.getChance();
        }

        if (totalChance <= 0) {
            return null;
        }

        // Select a random drop based on weights
        int randomValue = random.nextInt(totalChance);
        int currentChance = 0;

        for (CustomDrop drop : customDrops) {
            currentChance += drop.getChance();
            if (randomValue < currentChance) {
                return new ItemStack(drop.getMaterial(), drop.getAmount());
            }
        }

        return null;
    }

    /**
     * Creates default config if none exists
     */
    @Override
    public void saveDefaultConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
    }

    /**
     * Represents a custom item that can be fished
     */
    private class CustomDrop {
        private final Material material;
        private final int chance;
        private final int amount;

        /**
         * Creates a new custom drop
         *
         * @param material The Minecraft material to drop
         * @param chance The relative chance of this item dropping
         * @param amount How many of this item to give
         */
        public CustomDrop(Material material, int chance, int amount) {
            this.material = material;
            this.chance = chance;
            this.amount = amount;
        }

        public Material getMaterial() {
            return material;
        }

        public int getChance() {
            return chance;
        }

        public int getAmount() {
            return amount;
        }
    }
}