package io.xcutiboo.mythicrod.drops;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import io.xcutiboo.mythicrod.MythicRod;
public class DropManager {
    private final MythicRod plugin;
    private final Map<String, List<CustomDrop>> dropCategories = new HashMap<>();
    private final Random random = ThreadLocalRandom.current();
    private boolean debugMode = false;
    public DropManager(MythicRod plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin instance cannot be null");
        }
        this.plugin = plugin;
    }
    public void initialize() {
        loadDrops();
        this.debugMode = plugin.getConfigManager().isDebugMode();
    }
    public void loadDrops() {
        dropCategories.clear();
        FileConfiguration config = plugin.getConfigManager().getDropsConfig();
        plugin.getLogger().info("========================================");
        plugin.getLogger().info("Loading MythicRod drops configuration...");
        plugin.getLogger().info("========================================");
        // Load global drops
        if (config.contains("drops.global")) {
            List<CustomDrop> globalDrops = parseDrops(config.getStringList("drops.global"));
            if (!globalDrops.isEmpty()) {
                dropCategories.put("global", globalDrops);
                plugin.getLogger().info("✓ Loaded " + globalDrops.size() + " global drops");
            } else {
                plugin.getLogger().warning("! Global drops list is empty");
            }
        } else {
            plugin.getLogger().warning("! No global drops found - creating defaults");
            List<CustomDrop> defaultDrops = createDefaultDrops();
            dropCategories.put("global", defaultDrops);
        }
        // Load drop categories
        ConfigurationSection categories = config.getConfigurationSection("drops");
        if (categories != null) {
            for (String category : categories.getKeys(false)) {
                if (category.equals("global")) {
                    continue;
                }
                if (categories.isList(category)) {
                    List<CustomDrop> drops = parseDrops(categories.getStringList(category));
                    if (!drops.isEmpty()) {
                        dropCategories.put(category, drops);
                        plugin.getLogger().info("✓ Loaded " + drops.size() + " drops for category: " + category);
                    }
                } else if (categories.isConfigurationSection(category)) {
                    loadAdvancedDrops(category, categories.getConfigurationSection(category));
                }
            }
        }
        // Load biome-specific drops
        if (config.contains("biome-drops") && plugin.getConfigManager().enableBiomeSpecificDrops()) {
            ConfigurationSection biomeSection = config.getConfigurationSection("biome-drops");
            if (biomeSection != null) {
                for (String biome : biomeSection.getKeys(false)) {
                    if (biomeSection.isList(biome)) {
                        List<CustomDrop> biomeDrops = parseDrops(biomeSection.getStringList(biome));
                        for (CustomDrop drop : biomeDrops) {
                            drop.getBiomes().add(biome.toUpperCase());
                        }
                        String category = "biome_" + biome;
                        dropCategories.put(category, biomeDrops);
                        plugin.getLogger().info("✓ Loaded " + biomeDrops.size() + " drops for biome: " + biome);
                    }
                }
            }
        }
        plugin.getLogger().info("========================================");
        plugin.getLogger().info("Total: " + getTotalDropCount() + " drops across " +
            dropCategories.size() + " categories");
        plugin.getLogger().info("Permission mode: " +
            (plugin.getConfigManager().usePermissions() ? "ENABLED" : "DISABLED"));
        plugin.getLogger().info("========================================");
        // Print detailed category information
        for (Map.Entry<String, List<CustomDrop>> entry : dropCategories.entrySet()) {
            plugin.getLogger().info("  [" + entry.getKey() + "]: " + entry.getValue().size() + " drops");
        }
    }
    private void loadAdvancedDrops(String category, ConfigurationSection section) {
        List<CustomDrop> advancedDrops = new ArrayList<>();
        int loaded = 0;
        int failed = 0;
        for (String key : section.getKeys(false)) {
            ConfigurationSection dropSection = section.getConfigurationSection(key);
            if (dropSection == null) {
                plugin.getLogger().warning("  ! Invalid drop configuration: " + key);
                failed++;
                continue;
            }
            try {
                String materialName = dropSection.getString("material");
                if (materialName == null) {
                    plugin.getLogger().warning("  ! Missing material for drop: " + key);
                    failed++;
                    continue;
                }
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("  ! Invalid material '" + materialName + "' for drop: " + key);
                    failed++;
                    continue;
                }
                int chance = dropSection.getInt("chance", 10);
                int amount = dropSection.getInt("amount", 1);
                // Validate values
                if (chance <= 0) {
                    plugin.getLogger().warning("  ! Invalid chance (" + chance + ") for drop: " + key + " - using 10");
                    chance = 10;
                }
                if (amount <= 0) {
                    plugin.getLogger().warning("  ! Invalid amount (" + amount + ") for drop: " + key + " - using 1");
                    amount = 1;
                }
                String name = dropSection.getString("name");
                List<String> lore = dropSection.getStringList("lore");
                Map<Enchantment, Integer> enchantments = new HashMap<>();
                if (dropSection.contains("enchantments")) {
                    ConfigurationSection enchantSection = dropSection.getConfigurationSection("enchantments");
                    if (enchantSection != null) {
                        for (String enchantName : enchantSection.getKeys(false)) {
                            try {
                                @SuppressWarnings("deprecation")
                                Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                                if (enchant != null) {
                                    int level = enchantSection.getInt(enchantName);
                                    enchantments.put(enchant, level);
                                } else {
                                    plugin.getLogger().warning("  ! Invalid enchantment: " + enchantName);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("  ! Failed to load enchantment " + enchantName);
                            }
                        }
                    }
                }
                List<ItemFlag> itemFlags = new ArrayList<>();
                if (dropSection.contains("item-flags")) {
                    for (String flagName : dropSection.getStringList("item-flags")) {
                        try {
                            ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                            itemFlags.add(flag);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("  ! Invalid item flag: " + flagName);
                        }
                    }
                }
                boolean glowing = dropSection.getBoolean("glowing", false);
                String permission = dropSection.getString("permission");
                List<String> biomes = new ArrayList<>();
                if (dropSection.contains("biomes")) {
                    biomes = dropSection.getStringList("biomes").stream()
                            .map(String::toUpperCase)
                            .collect(Collectors.toList());
                }
                CustomDrop drop = new CustomDrop(material, chance, amount, name, lore,
                        enchantments, itemFlags, glowing, permission, biomes);
                advancedDrops.add(drop);
                loaded++;
                plugin.getLogger().fine("  ✓ Loaded advanced drop: " + key +
                    " (material=" + material + ", chance=" + chance + ", amount=" + amount + ")");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "  ! Error loading drop: {}", new Object[]{key, e});
                failed++;
            }
        }
        if (!advancedDrops.isEmpty()) {
            dropCategories.put(category, advancedDrops);
            plugin.getLogger().info("✓ Loaded " + loaded + " advanced drops for category: " + category +
                (failed > 0 ? " (" + failed + " failed)" : ""));
        } else if (failed > 0) {
            plugin.getLogger().warning("! Failed to load any drops for category: " + category +
                " (" + failed + " errors)");
        }
    }
    private List<CustomDrop> parseDrops(List<String> dropStrings) {
        List<CustomDrop> drops = new ArrayList<>();
        for (String dropString : dropStrings) {
            String[] parts = dropString.split(",");
            if (parts.length < 2) {
                plugin.getLogger().warning("  ! Invalid drop format: " + dropString);
                continue;
            }
            try {
                Material material = Material.matchMaterial(parts[0].toUpperCase().trim());
                if (material == null) {
                    plugin.getLogger().warning("  ! Invalid material: " + parts[0]);
                    continue;
                }
                int chance = Integer.parseInt(parts[1].trim());
                int amount = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 1;
                if (chance <= 0 || amount <= 0) {
                    plugin.getLogger().warning("  ! Invalid values for drop: " + dropString);
                    continue;
                }
                drops.add(new CustomDrop(material, chance, amount));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("  ! Invalid number in drop: " + dropString);
            }
        }
        return drops;
    }
    private List<CustomDrop> createDefaultDrops() {
        List<CustomDrop> defaults = new ArrayList<>();
        defaults.add(new CustomDrop(Material.COD, 40, 1));
        defaults.add(new CustomDrop(Material.SALMON, 30, 1));
        defaults.add(new CustomDrop(Material.TROPICAL_FISH, 20, 1));
        defaults.add(new CustomDrop(Material.PUFFERFISH, 15, 1));
        defaults.add(new CustomDrop(Material.IRON_INGOT, 10, 1));
        defaults.add(new CustomDrop(Material.GOLD_INGOT, 8, 1));
        defaults.add(new CustomDrop(Material.DIAMOND, 3, 1));
        defaults.add(new CustomDrop(Material.EMERALD, 5, 1));
        return defaults;
    }
    public CustomDrop getRandomDrop(Player player, String biomeName) {
        if (player == null) {
            plugin.getLogger().warning("Cannot get drop for null player");
            return null;
        }
        List<CustomDrop> eligibleDrops = collectEligibleDrops(player, biomeName);
        if (debugMode || eligibleDrops.isEmpty()) {
            plugin.getLogger().info("Drop selection for " + player.getName() + ":");
            plugin.getLogger().info("  Biome: " + (biomeName != null ? biomeName : "unknown"));
            plugin.getLogger().info("  Eligible drops: " + eligibleDrops.size());
            plugin.getLogger().info("  Permission mode: " + plugin.getConfigManager().usePermissions());
        }
        if (eligibleDrops.isEmpty()) {
            plugin.getLogger().warning("No eligible drops for player " + player.getName() +
                " in biome " + (biomeName != null ? biomeName : "unknown"));
            plugin.getLogger().warning("  Available categories: " + dropCategories.keySet());
            plugin.getLogger().warning("  Permissions enabled: " + plugin.getConfigManager().usePermissions());
            if (plugin.getConfigManager().usePermissions()) {
                plugin.getLogger().warning("  Player may need permission: mythicrod.drops.global");
            }
            return null;
        }
        CustomDrop selected = selectWeightedRandom(eligibleDrops);
        if (debugMode) {
            plugin.getLogger().info("  Selected: " + selected.getMaterial() +
                " (chance=" + selected.getChance() + ")");
        }
        return selected;
    }
    private List<CustomDrop> collectEligibleDrops(Player player, String biomeName) {
        List<CustomDrop> eligibleDrops = new ArrayList<>();
        // Always try to include global drops if player has permission (or permissions disabled)
        if (dropCategories.containsKey("global")) {
            if (!plugin.getConfigManager().usePermissions() ||
                player.hasPermission("mythicrod.drops.global") ||
                player.hasPermission("mythicrod.drops.*") ||
                player.hasPermission("mythicrod.*")) {
                eligibleDrops.addAll(dropCategories.get("global"));
                if (debugMode) {
                    plugin.getLogger().info("  Added " + dropCategories.get("global").size() + " global drops");
                }
            } else if (plugin.getConfigManager().usePermissions()) {
                plugin.getLogger().fine("Player " + player.getName() + " lacks permission: mythicrod.drops.global");
            }
        }
        // Add category-based drops
        for (Map.Entry<String, List<CustomDrop>> entry : dropCategories.entrySet()) {
            String category = entry.getKey();
            // Skip global (already added) and biome categories (handled separately)
            if (category.equals("global") || category.startsWith("biome_")) {
                continue;
            }
            // Check if player has permission for this category
            if (!plugin.getConfigManager().usePermissions() ||
                player.hasPermission("mythicrod.drops." + category) ||
                player.hasPermission("mythicrod.drops.*") ||
                player.hasPermission("mythicrod.*")) {
                for (CustomDrop drop : entry.getValue()) {
                    // Check individual drop permission
                    if (drop.getPermission() == null || player.hasPermission(drop.getPermission())) {
                        // Check biome restriction
                        if (drop.getBiomes().isEmpty() ||
                            (biomeName != null && drop.getBiomes().contains(biomeName.toUpperCase()))) {
                            eligibleDrops.add(drop);
                        }
                    }
                }
                if (debugMode) {
                    plugin.getLogger().info("  Added drops from category: " + category);
                }
            }
        }
        // Add biome-specific drops if enabled
        if (plugin.getConfigManager().enableBiomeSpecificDrops() && biomeName != null) {
            String biomeCategory = "biome_" + biomeName.toLowerCase();
            if (dropCategories.containsKey(biomeCategory)) {
                eligibleDrops.addAll(dropCategories.get(biomeCategory));
                if (debugMode) {
                    plugin.getLogger().info("  Added biome-specific drops for: " + biomeName);
                }
            }
        }
        return eligibleDrops;
    }
    private CustomDrop selectWeightedRandom(List<CustomDrop> drops) {
        if (drops.isEmpty()) {
            return null;
        }
        int totalChance = drops.stream()
            .mapToInt(CustomDrop::getChance)
            .sum();
        if (totalChance <= 0) {
            plugin.getLogger().warning("Total drop chance is zero or negative - selecting random drop");
            return drops.get(random.nextInt(drops.size()));
        }
        int randomValue = random.nextInt(totalChance);
        int currentChance = 0;
        for (CustomDrop drop : drops) {
            currentChance += drop.getChance();
            if (randomValue < currentChance) {
                return drop;
            }
        }
        // Fallback (should never reach here)
        return drops.get(drops.size() - 1);
    }
    public Map<String, List<CustomDrop>> getDropCategories() {
        return Collections.unmodifiableMap(dropCategories);
    }
    public int getTotalDropCount() {
        return dropCategories.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    public boolean isDebugMode() {
        return debugMode;
    }
    public void reload() {
        loadDrops();
    }
}
