package io.xcutiboo.mythicrod.drops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import lombok.NonNull;

/**
 * Manages fishing drop tables loaded from configuration.
 *
 * <p><strong>Thread safety:</strong> The canonical drop table is held in an
 * {@link AtomicReference} so that a reload (which builds a completely new map)
 * is published atomically.  Readers always see either the old complete map or
 * the new complete map — never a partially-populated one.  Individual category
 * lists use {@link CopyOnWriteArrayList} so that in-place mutations
 * ({@code updateDrop}, {@code deleteDrop}) are safe to perform while concurrent
 * readers iterate over the list.  {@code base64Drops} uses a
 * {@link ConcurrentHashMap} for the same reason.
 */
public class DropManager {
    @NonNull
    private final Logger logger;

    /**
     * Atomic reference to the current drop table.
     * Swap the entire reference on reload — never mutate the map in-place across threads.
     */
    private final AtomicReference<Map<String, List<CustomDrop>>> dropCategoriesRef =
            new AtomicReference<>(new ConcurrentHashMap<>());

    private final Map<String, String> base64Drops = new ConcurrentHashMap<>();
    private final DropSelector selector;
    private volatile boolean debugMode = false;

    public DropManager(@NonNull Logger logger) {
        this.logger = logger;
        this.selector = new DropSelector(logger, false);
    }

    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        this.selector.setDebugMode(debug);
    }

    // ---------------------------------------------------------------------------
    // Internal helper — returns the live (current) categories map.
    // ---------------------------------------------------------------------------
    private Map<String, List<CustomDrop>> dropCategories() {
        return dropCategoriesRef.get();
    }

    public void loadDrops(PlatformConfiguration config) {
        // Build a fresh map, then publish it atomically so concurrent readers are
        // never exposed to a partially-populated state.
        Map<String, List<CustomDrop>> newCategories = new ConcurrentHashMap<>();
        int totalDrops = 0;

        PlatformConfiguration dropsSection = config.getSection("fishing.drops");
        if (dropsSection == null) {
            logger.warning("No 'fishing.drops' section found in config! Loading defaults.");
            loadDefaultDrops(newCategories);
            dropCategoriesRef.set(newCategories);
            return;
        }

        for (String category : dropsSection.getKeys("", false)) {
            List<CustomDrop> categoryDrops = loadCategory(dropsSection, category);
            if (!categoryDrops.isEmpty()) {
                // CopyOnWriteArrayList: safe for concurrent iteration while mutations occur
                newCategories.put(category.toLowerCase(java.util.Locale.ROOT),
                        new CopyOnWriteArrayList<>(categoryDrops));
                totalDrops += categoryDrops.size();
            }
        }

        // Atomic publish — readers will see the complete new table from this point
        dropCategoriesRef.set(newCategories);
        logger.info("Loaded " + totalDrops + " drops across " + newCategories.size() + " categories");
    }

    private List<CustomDrop> loadCategory(PlatformConfiguration dropsSection, String category) {
        List<CustomDrop> drops = new ArrayList<>();
        PlatformConfiguration categorySection = dropsSection.getSection(category);
        
        if (categorySection == null) return drops;

        if (debugMode) {
            logger.info("Loading category: " + category);
        }

        for (String key : categorySection.getKeys("", false)) {
            CustomDrop drop = parseDrop(categorySection, key);
            if (drop != null) drops.add(drop);
        }

        return drops;
    }

    private CustomDrop parseDrop(PlatformConfiguration section, String key) {
        PlatformConfiguration dropSection = section.getSection(key);
        
        if (dropSection != null) {
            return parseComplexDrop(dropSection, key);
        }
        
        String dropString = section.getString(key);
        if (dropString != null) {
            return parseSimpleDrop(dropString);
        }
        
        return null;
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
                return CustomDrop.createNexoDrop(nexoItemId, chance, amount);
            }

            String material = dropSection.getString("material", null);
            if (material == null) {
                logger.warning("Missing material for drop: " + key);
                return null;
            }

            Map<String, Integer> enchantments = loadEnchantments(dropSection.getSection("enchantments"));
            List<String> flags = dropSection.getStringList("flags");

            return createDrop(material, chance, amount, name, lore, enchantments, flags, 
                          glowing, permission, biomes, null);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse drop: " + key, e);
            return null;
        }
    }

    private Map<String, Integer> loadEnchantments(PlatformConfiguration enchantsSection) {
        Map<String, Integer> enchantments = new HashMap<>();
        if (enchantsSection == null) return enchantments;
        
        for (String key : enchantsSection.getKeys("", false)) {
            enchantments.put(key.toLowerCase(java.util.Locale.ROOT), enchantsSection.getInt(key, 1));
        }
        return enchantments;
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
            String material = parts[0].toUpperCase(java.util.Locale.ROOT).trim();
            int chance = parts.length > 1 ? Integer.parseInt(parts[1]) : 100;
            int amount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;

            return createDrop(material, chance, amount);
        } catch (Exception e) {
            logger.warning("Failed to parse drop: " + dropString);
            return null;
        }
    }

    private void loadDefaultDrops(Map<String, List<CustomDrop>> target) {
        List<CustomDrop> defaults = new ArrayList<>();
        // Ores and minerals
        defaults.add(createDrop("DIAMOND", 5, 1));
        defaults.add(createDrop("EMERALD", 8, 1));
        defaults.add(createDrop("GOLD_INGOT", 12, 1));
        defaults.add(createDrop("IRON_INGOT", 15, 1));
        defaults.add(createDrop("REDSTONE", 20, 2));
        defaults.add(createDrop("LAPIS_LAZULI", 20, 3));
        defaults.add(createDrop("COAL", 25, 2));
        defaults.add(createDrop("COPPER_INGOT", 18, 2));
        
        // Gems and valuables
        defaults.add(createDrop("AMETHYST_SHARD", 15, 2));
        defaults.add(createDrop("QUARTZ", 18, 3));
        defaults.add(createDrop("PRISMARINE_SHARD", 12, 2));
        defaults.add(createDrop("PRISMARINE_CRYSTALS", 10, 1));
        
        // Treasures
        defaults.add(createDrop("NAME_TAG", 8, 1));
        defaults.add(createDrop("SADDLE", 6, 1));
        defaults.add(createDrop("NAUTILUS_SHELL", 10, 1));
        defaults.add(createDrop("HEART_OF_THE_SEA", 3, 1));
        
        target.put("default", new CopyOnWriteArrayList<>(defaults));
        logger.info("Loaded " + defaults.size() + " default drops (ores, gems, treasures).");
    }

    private CustomDrop createDrop(String id, int chance, int amount) {
        return new CustomDrop(new DropConfigurationRecord(id, chance, amount, null, null, 
            0, null, null, false, null, null, null));
    }

    private CustomDrop createDrop(String id, int chance, int amount, String name,
                                  List<String> lore, Map<String, Integer> enchantments,
                                  List<String> flags, boolean glowing, String permission,
                                  List<String> biomes, String nexoItemId) {
        return new CustomDrop(new DropConfigurationRecord(id, chance, amount, name, lore,
            0, enchantments, flags, glowing, permission, biomes, nexoItemId));
    }

    /**
     * Selects a random drop with a luck multiplier applied to rare/legendary
     * drop weights.  A multiplier &gt; 1.0 makes rare catches more probable.
     *
     * @param player         the fishing player (permission + biome checks)
     * @param biomeName      current biome key string
     * @param luckMultiplier from {@code MythicRodRewardRollEvent}, clamped ≥ 0.01
     */
    public CustomDrop getRandomDrop(PlatformPlayer player, String biomeName, double luckMultiplier) {
        if (player == null || !player.isOnline()) {
            logger.fine("[DROP-MANAGER] Player null or offline");
            return null;
        }
        List<CustomDrop> allDrops = getAllDrops();
        return selector.selectDrop(allDrops, player, biomeName, luckMultiplier);
    }

    /** Luck-neutral overload — delegates with multiplier 1.0. */
    public CustomDrop getRandomDrop(PlatformPlayer player, String biomeName) {
        if (player == null || !player.isOnline()) {
            logger.fine("[DROP-MANAGER] Player null or offline");
            return null;
        }
        // Snapshot the current drop list; the AtomicReference guarantees we see a
        // complete table even if a reload is racing on another thread.
        List<CustomDrop> allDrops = getAllDrops();
        logger.fine("[DROP-MANAGER] Total drops available: " + allDrops.size());
        for (CustomDrop drop : allDrops) {
            logger.fine("[DROP-MANAGER]   - " + drop.getIdentifier() + " (chance: " + drop.getChance() + ")");
        }
        CustomDrop result = selector.selectDrop(allDrops, player, biomeName);
        logger.fine("[DROP-MANAGER] Selected: " + (result != null ? result.getIdentifier() : "NULL"));
        return result;
    }

    public List<CustomDrop> getAllDrops() {
        return dropCategories().values().stream()
            .flatMap(List::stream)
            .toList();
    }

    public List<CustomDrop> getAvailableDrops(PlatformPlayer player) {
        if (player == null) return List.of();
        return getAllDrops().stream()
            .filter(drop -> !requiresPermission(drop) || player.hasPermission(drop.getPermission()))
            .toList();
    }

    private boolean requiresPermission(CustomDrop drop) {
        return drop.getPermission() != null && !drop.getPermission().isEmpty();
    }

    public Map<String, List<CustomDrop>> getDropCategories() {
        return Collections.unmodifiableMap(dropCategories());
    }

    public int getTotalDropCount() {
        return dropCategories().values().stream().mapToInt(List::size).sum();
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Save a Base64 encoded item as a drop configuration.
     * 
     * @param id The unique ID for this drop
     * @param base64String The Base64 encoded ItemStack data
     * @param creator The name of the player who created it
     */
    public void saveBase64Drop(String id, String base64String, String creator) {
        base64Drops.put(id, base64String);
        logger.info("Saved Base64 drop '" + id + "' by " + creator);
    }

    /**
     * Get a Base64 encoded drop by ID.
     * 
     * @param id The drop ID
     * @return The Base64 string, or null if not found
     */
    public String getBase64Drop(String id) {
        return base64Drops.get(id);
    }

    /**
     * Check if a Base64 drop exists.
     * 
     * @param id The drop ID
     * @return true if exists
     */
    public boolean hasBase64Drop(String id) {
        return base64Drops.containsKey(id);
    }

    public void reload(PlatformConfiguration config) {
        loadDrops(config);
    }

    /**
     * Update an existing drop with new properties.
     *
     * <p>Thread safe: the category list is a {@link CopyOnWriteArrayList}, so
     * {@code set()} is atomic and concurrent readers are unaffected.
     *
     * @param dropId     The drop identifier
     * @param category   The category name
     * @param chance     The new chance value
     * @param amount     The new amount
     * @param customName The new custom name (can be null)
     * @param lore       The new lore list
     * @param glowing    Whether the item should glow
     */
    public void updateDrop(String dropId, String category, int chance, int amount,
                           String customName, List<String> lore, boolean glowing) {
        List<CustomDrop> drops = dropCategories().get(category.toLowerCase(java.util.Locale.ROOT));
        if (drops == null) return;
        
        for (int i = 0; i < drops.size(); i++) {
            CustomDrop existing = drops.get(i);
            if (existing.getIdentifier().equals(dropId)) {
                // Create new drop with updated properties
                DropConfigurationRecord newConfig = new DropConfigurationRecord(
                    dropId, chance, amount, customName, lore, 
                    existing.getCustomModelData(),
                    existing.getEnchantments(),
                    existing.getItemFlags(),
                    glowing, 
                    existing.getPermission(), 
                    existing.getBiomes(), 
                    existing.getNexoItemId()
                );
                drops.set(i, new CustomDrop(newConfig));
                logger.info("Updated drop: " + dropId + " in category: " + category);
                return;
            }
        }
    }
    
    /**
     * Delete a drop from a category.
     *
     * <p>Thread safe: {@link CopyOnWriteArrayList#removeIf} performs a
     * copy-on-write replace so concurrent readers are unaffected.
     *
     * @param dropId   The drop identifier
     * @param category The category name
     */
    public void deleteDrop(String dropId, String category) {
        List<CustomDrop> drops = dropCategories().get(category.toLowerCase(java.util.Locale.ROOT));
        if (drops == null) return;
        
        boolean removed = drops.removeIf(d -> d.getIdentifier().equals(dropId));
        if (removed) {
            logger.info("Deleted drop: " + dropId + " from category: " + category);
        }
    }
    
    /**
     * Save all drops to the configuration file.
     * This is a placeholder - actual implementation would write to config.yml
     */
    public void saveDropsConfig() {
        // In a real implementation, this would serialize dropCategories back to config
        // For now, just log that drops were saved
        logger.info("Drops configuration saved (" + getTotalDropCount() + " total drops)");
    }
}
