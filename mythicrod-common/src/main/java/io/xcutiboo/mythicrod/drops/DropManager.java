package io.xcutiboo.mythicrod.drops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

public class DropManager {
    private final MythicRodPlugin plugin;
    private final Map<String, List<CustomDrop>> dropCategories = new HashMap<>();
    private DropSelector selector;
    private boolean debugMode = false;

    public DropManager(MythicRodPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin instance cannot be null");
        }
        this.plugin = plugin;
        this.selector = new DropSelector(plugin.getLogger(), false, false);
    }

    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        this.selector = new DropSelector(plugin.getLogger(), plugin.getConfigManager().usePermissions(), debug);
    }

    public void loadDrops(PlatformConfiguration config) {
        dropCategories.clear();
        int totalDrops = 0;

        PlatformConfiguration dropsSection = config.getSection("fishing.drops");
        if (dropsSection == null) {
            plugin.getLogger().warning("No 'fishing.drops' section found in config.yml! Loading default drops.");
            loadDefaultDrops();
            return;
        }

        for (String category : dropsSection.getKeys("", false)) {
            List<CustomDrop> categoryDrops = new ArrayList<>();
            PlatformConfiguration categorySection = dropsSection.getSection(category);

            if (categorySection != null) {
                if (debugMode) {
                    plugin.getLogger().info("Loading drop category: " + category);
                }

                for (String key : categorySection.getKeys("", false)) {
                    PlatformConfiguration dropSection = categorySection.getSection(key);
                    if (dropSection != null) {
                        CustomDrop drop = parseComplexDrop(dropSection, key);
                        if (drop != null) {
                            categoryDrops.add(drop);
                            totalDrops++;
                        }
                    } else if (categorySection.getString(key) != null) {
                        String dropString = categorySection.getString(key);
                        CustomDrop drop = parseSimpleDrop(dropString);
                        if (drop != null) {
                            categoryDrops.add(drop);
                            totalDrops++;
                        }
                    }
                }
            }

            if (!categoryDrops.isEmpty()) {
                dropCategories.put(category.toLowerCase(), categoryDrops);
            } else {
                plugin.getLogger().warning("No valid drops loaded for category: " + category);
            }
        }

        plugin.getLogger().info("Loaded " + totalDrops + " total drops across " + dropCategories.size() + " categories");
    }

    private CustomDrop parseComplexDrop(PlatformConfiguration dropSection, String key) {
        try {
            int chance = dropSection.getInt("chance", 100);
            int amount = dropSection.getInt("amount", 1);
            String name = dropSection.getString("name", null);
            List<String> lore = dropSection.getStringList("lore");
            boolean glowing = dropSection.getBoolean("glowing", false);
            String permission = dropSection.getString("permission", null);
            List<String> biomes = dropSection.getStringList("biomes");

            String nexoItemId = dropSection.getString("nexo-item", null);
            if (nexoItemId != null && !nexoItemId.isEmpty()) {
                if (debugMode) {
                    plugin.getLogger().info("  Found Nexo item drop: " + nexoItemId + " (chance: " + chance + ")");
                }
                return CustomDrop.createNexoDrop(nexoItemId, chance, amount);
            }

            String materialName = dropSection.getString("material", null);
            if (materialName == null) {
                plugin.getLogger().warning("  ! Missing material for drop: " + key);
                return null;
            }

            Map<String, Integer> enchantments = new HashMap<>();
            PlatformConfiguration enchantsSection = dropSection.getSection("enchantments");
            if (enchantsSection != null) {
                for (String enchantKey : enchantsSection.getKeys("", false)) {
                    enchantments.put(enchantKey.toLowerCase(), enchantsSection.getInt(enchantKey, 1));
                }
            }

            List<String> itemFlags = dropSection.getStringList("flags");

            CustomDrop drop = createCustomDrop(materialName, chance, amount, name, lore,
                    enchantments, itemFlags, glowing, permission, biomes, null);

            if (debugMode) {
                plugin.getLogger().info("  Loaded " + key + 
                    " (material=" + materialName + ", chance=" + chance + ", amount=" + amount + ")");
            }

            return drop;
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to parse complex drop: " + key, e);
            return null;
        }
    }

    private CustomDrop parseSimpleDrop(String dropString) {
        if (dropString == null || dropString.isEmpty()) return null;

        try {
            if (dropString.startsWith("nexo:")) {
                String[] parts = dropString.substring(5).split(":");
                String nexoId = parts[0].trim();
                int chance = parts.length > 1 ? Integer.parseInt(parts[1]) : 100;
                int amount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                return CustomDrop.createNexoDrop(nexoId, chance, amount);
            }

            String[] parts = dropString.split(":");
            if (parts.length > 0) {
                String materialName = parts[0].toUpperCase().trim();
                int chance = parts.length > 1 ? Integer.parseInt(parts[1]) : 100;
                int amount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;

                return createCustomDrop(materialName, chance, amount);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse simple drop string: " + dropString + " - " + e.getMessage());
        }
        return null;
    }

    private void loadDefaultDrops() {
        List<CustomDrop> defaults = new ArrayList<>();
        defaults.add(new CustomDrop(new DropConfigurationRecord("COD", 40, 1, null, null, 0, null, null, false, null, null, null)));
        defaults.add(new CustomDrop(new DropConfigurationRecord("SALMON", 30, 1, null, null, 0, null, null, false, null, null, null)));
        defaults.add(new CustomDrop(new DropConfigurationRecord("TROPICAL_FISH", 20, 1, null, null, 0, null, null, false, null, null, null)));
        defaults.add(new CustomDrop(new DropConfigurationRecord("PUFFERFISH", 15, 1, null, null, 0, null, null, false, null, null, null)));
        defaults.add(new CustomDrop(new DropConfigurationRecord("IRON_INGOT", 10, 1, null, null, 0, null, null, false, null, null, null)));
        defaults.add(new CustomDrop(new DropConfigurationRecord("GOLD_INGOT", 8, 1, null, null, 0, null, null, false, null, null, null)));
        defaults.add(new CustomDrop(new DropConfigurationRecord("DIAMOND", 3, 1, null, null, 0, null, null, false, null, null, null)));
        defaults.add(new CustomDrop(new DropConfigurationRecord("EMERALD", 5, 1, null, null, 0, null, null, false, null, null, null)));
        
        dropCategories.put("default", defaults);
        plugin.getLogger().info("Loaded default fishing drops.");
    }

    private CustomDrop createCustomDrop(String identifier, int chance, int amount) {
        return new CustomDrop(new DropConfigurationRecord(identifier, chance, amount, null, null, 0, null, null, false, null, null, null));
    }

    private CustomDrop createCustomDrop(String identifier, int chance, int amount, String name,
                                       List<String> lore, Map<String, Integer> enchantments,
                                       List<String> itemFlags, boolean glowing, String permission,
                                       List<String> biomes, String nexoItemId) {
        return new CustomDrop(new DropConfigurationRecord(identifier, chance, amount, name, lore, 0, enchantments, itemFlags, glowing, permission, biomes, nexoItemId));
    }
    public CustomDrop getRandomDrop(PlatformPlayer player, String biomeName) {
        if (player == null) {
            plugin.getLogger().warning("Cannot get drop for null player");
            return null;
        }
        if (!player.isOnline()) {
            plugin.getLogger().warning("Cannot get drop for offline player: " + player.getName());
            return null;
        }
        
        List<CustomDrop> allDrops = getAllDrops();
        CustomDrop selected = selector.selectDrop(allDrops, player, biomeName);
        
        if (selected == null && debugMode) {
            plugin.getLogger().warning("No eligible drops for player " + player.getName() +
                " in biome " + (biomeName != null ? biomeName : "unknown"));
            plugin.getLogger().warning("  Available categories: " + dropCategories.keySet());
            plugin.getLogger().warning("  Permissions enabled: " + plugin.getConfigManager().usePermissions());
        }
        
        return selected;
    }
    public Map<String, List<CustomDrop>> getDropCategories() {
        return Collections.unmodifiableMap(dropCategories);
    }
    public int getTotalDropCount() {
        return dropCategories.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    public boolean isDebugMode() {
        return debugMode;
    }
    public void reload(PlatformConfiguration config) {
        loadDrops(config);
    }

    public List<CustomDrop> getAllDrops() {
        return dropCategories.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public List<CustomDrop> getAvailableDrops(PlatformPlayer player) {
        return getAllDrops().stream()
                .filter(drop -> drop.getPermission() == null ||
                              drop.getPermission().isEmpty() ||
                              player.hasPermission(drop.getPermission()))
                .toList();
    }
}
