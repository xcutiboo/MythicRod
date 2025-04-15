package com.mythicrod.mythicrod.drops;

import com.mythicrod.mythicrod.MythicRod;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DropManager {

    private final MythicRod plugin;
    private final Map<String, List<CustomDrop>> dropCategories = new HashMap<>();
    private final Random random = ThreadLocalRandom.current();

    public DropManager(MythicRod plugin) {
        this.plugin = plugin;
        loadDrops();
    }

    public void loadDrops() {
        dropCategories.clear();
        FileConfiguration config = plugin.getConfigManager().getDropsConfig();

        if (config.contains("drops.global")) {
            List<CustomDrop> globalDrops = parseDrops(config.getStringList("drops.global"));
            dropCategories.put("global", globalDrops);
        } else {
            List<CustomDrop> defaultDrops = createDefaultDrops();
            dropCategories.put("global", defaultDrops);

            List<String> defaultDropsAsStrings = defaultDrops.stream()
                    .map(drop -> drop.getMaterial().name() + "," + drop.getChance() + "," + drop.getAmount())
                    .collect(Collectors.toList());

            config.set("drops.global", defaultDropsAsStrings);
            try {
                config.save(plugin.getDataFolder() + "/drops.yml");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save default drops", e);
            }
        }

        ConfigurationSection categories = config.getConfigurationSection("drops");
        if (categories != null) {
            for (String category : categories.getKeys(false)) {
                if (category.equals("global")) {
                    continue;
                }

                if (categories.isList(category)) {
                    dropCategories.put(category, parseDrops(categories.getStringList(category)));
                } else if (categories.isConfigurationSection(category)) {
                    loadAdvancedDrops(category, categories.getConfigurationSection(category));
                }
            }
        }

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
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + getTotalDropCount() + " custom drops across "
                + dropCategories.size() + " categories");
    }

    private void loadAdvancedDrops(String category, ConfigurationSection section) {
        List<CustomDrop> advancedDrops = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection dropSection = section.getConfigurationSection(key);
            if (dropSection == null) {
                continue;
            }

            try {
                String materialName = dropSection.getString("material");
                if (materialName == null) {
                    plugin.getLogger().warning("Missing material for drop " + key + " in category " + category);
                    continue;
                }

                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("Invalid material " + materialName + " for drop " + key);
                    continue;
                }

                int chance = dropSection.getInt("chance", 10);
                int amount = dropSection.getInt("amount", 1);

                String name = null;
                if (dropSection.contains("name")) {
                    name = ChatColor.translateAlternateColorCodes('&', dropSection.getString("name"));
                }

                List<String> lore = new ArrayList<>();
                if (dropSection.contains("lore")) {
                    lore = dropSection.getStringList("lore").stream()
                            .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                            .collect(Collectors.toList());
                }

                Map<Enchantment, Integer> enchantments = new HashMap<>();
                if (dropSection.contains("enchantments")) {
                    ConfigurationSection enchantSection = dropSection.getConfigurationSection("enchantments");
                    if (enchantSection != null) {
                        for (String enchantName : enchantSection.getKeys(false)) {
                            Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                            if (enchant != null) {
                                int level = enchantSection.getInt(enchantName);
                                enchantments.put(enchant, level);
                            } else {
                                plugin.getLogger().warning("Invalid enchantment: " + enchantName);
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
                            plugin.getLogger().warning("Invalid item flag: " + flagName);
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

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading advanced drop " + key, e);
            }
        }

        if (!advancedDrops.isEmpty()) {
            dropCategories.put(category, advancedDrops);
        }
    }

    private List<CustomDrop> parseDrops(List<String> dropStrings) {
        List<CustomDrop> drops = new ArrayList<>();

        for (String dropString : dropStrings) {
            String[] parts = dropString.split(",");
            if (parts.length < 2) {
                plugin.getLogger().warning("Invalid drop format: " + dropString);
                continue;
            }

            try {
                Material material = Material.matchMaterial(parts[0].toUpperCase());
                if (material == null) {
                    plugin.getLogger().warning("Invalid material: " + parts[0]);
                    continue;
                }

                int chance = Integer.parseInt(parts[1]);
                int amount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;

                drops.add(new CustomDrop(material, chance, amount));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid number in drop: " + dropString);
            }
        }

        return drops;
    }

    private List<CustomDrop> createDefaultDrops() {
        List<CustomDrop> defaults = new ArrayList<>();
        defaults.add(new CustomDrop(Material.DIAMOND, 5, 1));
        defaults.add(new CustomDrop(Material.IRON_INGOT, 30, 1));
        defaults.add(new CustomDrop(Material.GOLD_INGOT, 20, 1));
        defaults.add(new CustomDrop(Material.EMERALD, 15, 1));
        defaults.add(new CustomDrop(Material.NETHERITE_INGOT, 1, 1));
        return defaults;
    }

    public CustomDrop getRandomDrop(Player player, String biomeName) {
        List<List<CustomDrop>> eligibleCategories = new ArrayList<>();

        if (dropCategories.containsKey("global")) {
            eligibleCategories.add(dropCategories.get("global"));
        }

        if (plugin.getConfigManager().usePermissions()) {
            for (String category : dropCategories.keySet()) {
                if (category.equals("global") || category.startsWith("biome_")) {
                    continue;
                }

                if (player.hasPermission("mythicrod.drops." + category)) {
                    eligibleCategories.add(dropCategories.get(category));
                }
            }
        }

        if (plugin.getConfigManager().enableBiomeSpecificDrops() && biomeName != null) {
            String biomeCategory = "biome_" + biomeName.toLowerCase();
            if (dropCategories.containsKey(biomeCategory)) {
                eligibleCategories.add(dropCategories.get(biomeCategory));
            }
        }

        List<CustomDrop> eligibleDrops = new ArrayList<>();
        for (List<CustomDrop> category : eligibleCategories) {
            for (CustomDrop drop : category) {
                if (drop.getPermission() != null && !player.hasPermission(drop.getPermission())) {
                    continue;
                }

                if (!drop.getBiomes().isEmpty()
                        && (biomeName == null || !drop.getBiomes().contains(biomeName.toUpperCase()))) {
                    continue;
                }

                eligibleDrops.add(drop);
            }
        }

        if (eligibleDrops.isEmpty()) {
            return null;
        }

        int totalChance = 0;
        for (CustomDrop drop : eligibleDrops) {
            totalChance += drop.getChance();
        }

        if (totalChance <= 0) {
            return null;
        }

        int randomValue = random.nextInt(totalChance);
        int currentChance = 0;

        for (CustomDrop drop : eligibleDrops) {
            currentChance += drop.getChance();
            if (randomValue < currentChance) {
                return drop;
            }
        }

        return eligibleDrops.get(random.nextInt(eligibleDrops.size()));
    }

    public Map<String, List<CustomDrop>> getDropCategories() {
        return dropCategories;
    }

    public int getTotalDropCount() {
        return dropCategories.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public void reload() {
        loadDrops();
    }
}
