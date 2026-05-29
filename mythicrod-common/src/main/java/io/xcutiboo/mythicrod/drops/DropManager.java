package io.xcutiboo.mythicrod.drops;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.drop.DropCatalog;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.constants.PermissionNodes;
import lombok.NonNull;

/// Manages fishing drop tables loaded from configuration.
///
/// **Thread safety:** The canonical drop table is held in an
/// {@link AtomicReference} so that a reload (which builds a completely new map)
/// is published atomically. Readers always see either the old complete map or
/// the new complete map, never a partially-populated one. Individual category
/// lists use {@link CopyOnWriteArrayList} so that in-place mutations
/// (`updateDrop`, `deleteDrop`) are safe to perform while concurrent
/// readers iterate over the list.
public class DropManager implements DropCatalog {
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_CUSTOM_MODEL_DATA = "custom_model_data";
    private static final String KEY_CUSTOM_MODEL_DATA_LEGACY = "customModelData";
    private static final String KEY_CUSTOM_NAME = "custom_name";
    private static final String KEY_NAME = "name";
    private static final String KEY_LORE = "lore";
    private static final String KEY_GLOW = "glow";
    private static final String KEY_GLOWING = "glowing";
    private static final String KEY_PERMISSION = "permission";
    private static final String KEY_BIOMES = "biomes";
    private static final String KEY_NEXO_ITEM = "nexo-item";
    private static final String KEY_NEXO_ITEM_LEGACY = "nexo_item";
    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_MATERIAL = "material";
    private static final String KEY_ENCHANTMENTS = "enchantments";
    private static final String KEY_ITEM_FLAGS = "item_flags";
    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_CHANCE_LEGACY = "chance";
    private static final String NEXO_PREFIX = "nexo:";
    private static final String BIOME_CATEGORY_PREFIX = "biome_";

    @NonNull
    private final String loggerName;

    private final AtomicReference<Map<String, List<CustomDrop>>> dropCategoriesRef =
            new AtomicReference<>(new ConcurrentHashMap<>());

    private final DropSelector selector;
    private final Object asyncPersistenceMonitor = new Object();
    private final Object persistenceFileLock = new Object();
    private volatile boolean debugMode = false;
    private volatile int pendingAsyncPersistenceOperations = 0;
    // Reload swaps these references whole. Readers see either the old or new
    // PlatformConfiguration, never a half-built one, atomic publication is the
    // intended invariant. Using AtomicReference adds no value here.
    @SuppressWarnings("java:S3077")
    private volatile PlatformConfiguration dropsConfig;
    @SuppressWarnings("java:S3077")
    private volatile File dropsFile;

    public DropManager(@NonNull Logger logger) {
        this.loggerName = logger.getName();
        this.selector = new DropSelector(logger, false);
    }

    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        this.selector.setDebugMode(debug);
    }

    public void setUsePermissions(boolean usePermissions) {
        this.selector.setUsePermissions(usePermissions);
    }

    public void setUseBiomeSpecificDrops(boolean useBiomeSpecificDrops) {
        this.selector.setUseBiomeSpecificDrops(useBiomeSpecificDrops);
    }

    private Map<String, List<CustomDrop>> dropCategories() {
        return dropCategoriesRef.get();
    }

    public void loadDrops(PlatformConfiguration config) {
        loadDrops(config, null);
    }

    public void loadDrops(PlatformConfiguration config, File sourceFile) {
        this.dropsConfig = config;
        this.dropsFile = sourceFile;

        // Build a fresh map, then publish it atomically so concurrent readers are
        // never exposed to a partially-populated state.
        Map<String, List<CustomDrop>> newCategories = new ConcurrentHashMap<>();
        DropLoadReport report = new DropLoadReport();
        int totalDrops = 0;

        PlatformConfiguration dropsSection = config.getSection("drops");
        if (dropsSection == null) {
            dropsSection = config.getSection("fishing.drops");
        }
        PlatformConfiguration previousBiomeDropsSection = config.getSection("biome-drops");

        if (dropsSection == null && previousBiomeDropsSection == null) {
            warning(() -> "No 'drops', 'fishing.drops', or 'biome-drops' section found. Loading defaults.");
            loadDefaultDrops(newCategories);
            dropCategoriesRef.set(newCategories);
            return;
        }

        if (dropsSection != null) {
            totalDrops += loadConfiguredCategories(dropsSection, newCategories, report);
        }

        if (previousBiomeDropsSection != null) {
            totalDrops += loadPreviousBiomeDropCategories(previousBiomeDropsSection, newCategories, report);
        }

        // Readers see the complete new table from this point.
        dropCategoriesRef.set(newCategories);
        if (report.migratedWeightAliases > 0) {
            info(() -> "Read " + report.migratedWeightAliases
                + " drop weight value(s) from the previous 'chance' key. Save drops once to rewrite them as 'weight'.");
        }
        info("Loaded " + totalDrops + " drops across " + newCategories.size() + " categories");
    }

    private int loadConfiguredCategories(
        PlatformConfiguration dropsSection,
        Map<String, List<CustomDrop>> target,
        DropLoadReport report
    ) {
        int totalDrops = 0;

        for (String category : dropsSection.getKeys(false)) {
            String categoryKey = category.toLowerCase(Locale.ROOT);
            List<CustomDrop> categoryDrops = applyImplicitCategoryConditions(
                categoryKey,
                loadCategory(dropsSection, category, report)
            );
            totalDrops += publishCategory(
                target,
                categoryKey,
                categoryDrops
            );
        }

        return totalDrops;
    }

    private List<CustomDrop> applyImplicitCategoryConditions(String categoryKey, List<CustomDrop> categoryDrops) {
        if (categoryDrops.isEmpty()) {
            return categoryDrops;
        }

        List<String> implicitBiomes = implicitBiomesForCategory(categoryKey);
        String implicitPermission = implicitPermissionForCategory(categoryKey);

        if (implicitBiomes.isEmpty() && implicitPermission == null) {
            return categoryDrops;
        }

        List<CustomDrop> scopedDrops = new ArrayList<>(categoryDrops.size());
        for (CustomDrop drop : categoryDrops) {
            CustomDrop scopedDrop = drop;
            if (!implicitBiomes.isEmpty() && scopedDrop.getBiomes().isEmpty()) {
                scopedDrop = copyDropWithBiomes(scopedDrop, implicitBiomes);
            }
            if (implicitPermission != null
                    && (scopedDrop.getPermission() == null || scopedDrop.getPermission().isBlank())) {
                scopedDrop = copyDropWithPermission(scopedDrop, implicitPermission);
            }
            scopedDrops.add(scopedDrop);
        }
        return scopedDrops;
    }

    private List<String> implicitBiomesForCategory(String categoryKey) {
        if (!categoryKey.startsWith(BIOME_CATEGORY_PREFIX)) {
            return List.of();
        }

        String biomeKey = categoryKey.substring(BIOME_CATEGORY_PREFIX.length());
        if (biomeKey.isBlank()) {
            return List.of();
        }
        return List.of(normalizeBiomeKey(biomeKey));
    }

    private String implicitPermissionForCategory(String categoryKey) {
        return switch (categoryKey) {
            case "global" -> PermissionNodes.DROPS_GLOBAL;
            case "rare" -> PermissionNodes.DROPS_RARE;
            case "legendary" -> PermissionNodes.DROPS_LEGENDARY;
            default -> null;
        };
    }

    private int loadPreviousBiomeDropCategories(
        PlatformConfiguration biomeDropsSection,
        Map<String, List<CustomDrop>> target,
        DropLoadReport report
    ) {
        int totalDrops = 0;
        List<String> skippedDuplicateCategories = new ArrayList<>();

        for (String biomeKey : biomeDropsSection.getKeys(false)) {
            String categoryKey = toBiomeCategoryKey(biomeKey);
            if (target.containsKey(categoryKey)) {
                skippedDuplicateCategories.add("biome-drops." + biomeKey + " -> drops." + categoryKey);
            } else {
                List<CustomDrop> categoryDrops = loadCategory(biomeDropsSection, biomeKey, report);
                if (!categoryDrops.isEmpty()) {
                    List<String> biomeConstraints = List.of(normalizeBiomeKey(biomeKey));
                    List<CustomDrop> biomeScopedDrops = new ArrayList<>(categoryDrops.size());
                    for (CustomDrop drop : categoryDrops) {
                        biomeScopedDrops.add(copyDropWithBiomes(drop, biomeConstraints));
                    }
                    totalDrops += publishCategory(target, categoryKey, biomeScopedDrops);
                }
            }
        }

        if (!skippedDuplicateCategories.isEmpty()) {
            warning(() -> "Ignored " + skippedDuplicateCategories.size()
                + " previous biome drop section(s) because matching drops.* categories already exist: "
                + String.join(", ", skippedDuplicateCategories)
                + ". Save drops from the GUI once to rewrite the file to the current structure.");
        }

        return totalDrops;
    }

    private int publishCategory(
        Map<String, List<CustomDrop>> target,
        String categoryKey,
        List<CustomDrop> categoryDrops
    ) {
        if (categoryDrops.isEmpty()) {
            return 0;
        }

        target.merge(
            categoryKey,
            new CopyOnWriteArrayList<>(categoryDrops),
            (existing, incoming) -> {
                warning(() -> "Merging duplicate drop category '" + categoryKey
                    + "' from multiple configuration sections.");
                CopyOnWriteArrayList<CustomDrop> mergedDrops = new CopyOnWriteArrayList<>(existing);
                mergedDrops.addAll(incoming);
                return mergedDrops;
            }
        );
        return categoryDrops.size();
    }

    private CustomDrop copyDropWithBiomes(CustomDrop drop, List<String> biomeConstraints) {
        List<String> mergedBiomes = new ArrayList<>(drop.getBiomes());
        for (String biomeConstraint : biomeConstraints) {
            boolean alreadyPresent = mergedBiomes.stream().anyMatch(existing -> existing.equalsIgnoreCase(biomeConstraint));
            if (!alreadyPresent) {
                mergedBiomes.add(biomeConstraint);
            }
        }

        return new CustomDrop(new DropConfigurationRecord(
            drop.getIdentifier(),
            drop.getWeight(),
            drop.getAmount(),
            drop.getCustomName(),
            drop.getLore(),
            drop.getCustomModelData(),
            drop.getEnchantments(),
            drop.getItemFlags(),
            drop.isGlowing(),
            drop.getPermission(),
            mergedBiomes,
            drop.isNexoItem() ? drop.getNexoItemId() : null
        ));
    }

    private CustomDrop copyDropWithPermission(CustomDrop drop, String permission) {
        return new CustomDrop(new DropConfigurationRecord(
            drop.getIdentifier(),
            drop.getWeight(),
            drop.getAmount(),
            drop.getCustomName(),
            drop.getLore(),
            drop.getCustomModelData(),
            drop.getEnchantments(),
            drop.getItemFlags(),
            drop.isGlowing(),
            permission,
            drop.getBiomes(),
            drop.isNexoItem() ? drop.getNexoItemId() : null
        ));
    }

    private String normalizeBiomeKey(String biomeKey) {
        String normalized = biomeKey == null ? "" : biomeKey.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return normalized;
        }
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private String toBiomeCategoryKey(String biomeKey) {
        String normalized = biomeKey == null ? "unknown" : biomeKey.trim().toLowerCase(Locale.ROOT);
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator < normalized.length() - 1) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        return BIOME_CATEGORY_PREFIX + normalized.replace('-', '_');
    }

    private List<CustomDrop> loadCategory(
        PlatformConfiguration dropsSection,
        String category,
        DropLoadReport report
    ) {
        List<Map<?, ?>> mappedDrops = dropsSection.getMapList(category);
        if (!mappedDrops.isEmpty()) {
            return loadMappedDrops(mappedDrops, category, report);
        }

        List<String> simpleDrops = dropsSection.getStringList(category);
        if (!simpleDrops.isEmpty()) {
            return loadSimpleDrops(simpleDrops);
        }

        PlatformConfiguration categorySection = dropsSection.getSection(category);
        if (categorySection == null) return new ArrayList<>();

        if (debugMode) {
            info(() -> "Loading category: " + category);
        }
        return loadComplexDrops(categorySection, report);
    }

    private List<CustomDrop> loadMappedDrops(List<Map<?, ?>> mappedDrops, String category, DropLoadReport report) {
        List<CustomDrop> drops = new ArrayList<>(mappedDrops.size());
        for (Map<?, ?> mappedDrop : mappedDrops) {
            CustomDrop drop = parseMappedDrop(mappedDrop, category, report);
            if (drop != null) drops.add(drop);
        }
        return drops;
    }

    private List<CustomDrop> loadSimpleDrops(List<String> simpleDrops) {
        List<CustomDrop> drops = new ArrayList<>(simpleDrops.size());
        for (String simpleDrop : simpleDrops) {
            CustomDrop drop = parseSimpleDrop(simpleDrop);
            if (drop != null) drops.add(drop);
        }
        return drops;
    }

    private List<CustomDrop> loadComplexDrops(PlatformConfiguration categorySection, DropLoadReport report) {
        List<CustomDrop> drops = new ArrayList<>();
        for (String key : categorySection.getKeys(false)) {
            CustomDrop drop = parseComplexDrop(categorySection, key, report);
            if (drop != null) drops.add(drop);
        }
        return drops;
    }

    private CustomDrop parseComplexDrop(
        PlatformConfiguration categorySection,
        String key,
        DropLoadReport report
    ) {
        PlatformConfiguration dropSection = categorySection.getSection(key);
        if (dropSection == null) {
            return null;
        }

        try {
            int weight = readConfiguredWeight(dropSection, key, 100, report);
            int amount = dropSection.getInt(KEY_AMOUNT, 1);
            int customModelData = dropSection.getInt(KEY_CUSTOM_MODEL_DATA, dropSection.getInt(KEY_CUSTOM_MODEL_DATA_LEGACY, 0));
            String name = firstNonBlank(
                dropSection.getString(KEY_CUSTOM_NAME, null),
                dropSection.getString(KEY_NAME, null)
            );
            List<String> lore = dropSection.getStringList(KEY_LORE);
            boolean glowing = dropSection.getBoolean(KEY_GLOW, dropSection.getBoolean(KEY_GLOWING, false));
            String permission = dropSection.getString(KEY_PERMISSION, null);
            List<String> biomes = dropSection.getStringList(KEY_BIOMES);

            String nexoItemId = firstNonBlank(
                dropSection.getString(KEY_NEXO_ITEM, null),
                dropSection.getString(KEY_NEXO_ITEM_LEGACY, null)
            );
            String material = firstNonBlank(
                dropSection.getString(KEY_IDENTIFIER, null),
                dropSection.getString(KEY_MATERIAL, null)
            );
            if ((material == null || material.isEmpty()) && (nexoItemId == null || nexoItemId.isEmpty())) {
                warning(() -> "Missing identifier/material for drop: " + key);
                return null;
            }

            Map<String, Integer> enchantments = loadEnchantments(dropSection.getSection(KEY_ENCHANTMENTS));
            List<String> flags = readFlagList(dropSection);

            String identifier = (nexoItemId != null && !nexoItemId.isEmpty())
                ? NEXO_PREFIX + nexoItemId
                : material;

            return createDrop(new DropConfigurationRecord(identifier, weight, amount, name, lore,
                customModelData, enchantments, flags, glowing, permission, biomes, null));

        } catch (IllegalArgumentException e) {
            log(Level.SEVERE, e, () -> "Failed to parse drop: " + key);
            return null;
        }
    }

    private CustomDrop parseMappedDrop(Map<?, ?> mappedDrop, String category, DropLoadReport report) {
        try {
            String nexoItemId = firstNonBlank(
                asString(mappedDrop.get(KEY_NEXO_ITEM)),
                asString(mappedDrop.get(KEY_NEXO_ITEM_LEGACY))
            );
            String identifier = firstNonBlank(
                asString(mappedDrop.get(KEY_IDENTIFIER)),
                asString(mappedDrop.get(KEY_MATERIAL))
            );

            if ((identifier == null || identifier.isBlank()) && (nexoItemId == null || nexoItemId.isBlank())) {
                warning(() -> "Skipping drop in category '" + category + "' because it has no identifier/material");
                return null;
            }

            if (nexoItemId != null && !nexoItemId.isBlank()) {
                identifier = NEXO_PREFIX + nexoItemId;
            }

            int weight = readMappedWeight(mappedDrop, category, identifier, 100, report);
            int amount = asInt(mappedDrop.get(KEY_AMOUNT), 1);
            int customModelData = asInt(
                firstMappedValue(mappedDrop, KEY_CUSTOM_MODEL_DATA, KEY_CUSTOM_MODEL_DATA_LEGACY),
                0
            );
            String customName = firstNonBlank(
                asString(mappedDrop.get(KEY_CUSTOM_NAME)),
                asString(mappedDrop.get(KEY_NAME))
            );
            List<String> lore = asStringList(mappedDrop.get(KEY_LORE));
            boolean glowing = asBoolean(
                firstMappedValue(mappedDrop, KEY_GLOW, KEY_GLOWING),
                false
            );
            String permission = asString(mappedDrop.get(KEY_PERMISSION));
            List<String> biomes = asStringList(mappedDrop.get(KEY_BIOMES));
            Map<String, Integer> enchantments = asIntMap(mappedDrop.get(KEY_ENCHANTMENTS));
            List<String> flags = firstNonEmpty(
                asStringList(mappedDrop.get(KEY_ITEM_FLAGS)),
                asStringList(mappedDrop.get("item-flags")),
                asStringList(mappedDrop.get("flags"))
            );

            return createDrop(new DropConfigurationRecord(identifier, weight, amount, customName, lore,
                customModelData, enchantments, flags, glowing, permission, biomes, nexoItemId));
        } catch (IllegalArgumentException e) {
            log(Level.WARNING, e, () -> "Failed to parse mapped drop in category '" + category + "'");
            return null;
        }
    }

    private Map<String, Integer> loadEnchantments(PlatformConfiguration enchantsSection) {
        Map<String, Integer> enchantments = new HashMap<>();
        if (enchantsSection == null) return enchantments;

        for (String key : enchantsSection.getKeys(false)) {
            enchantments.put(key.toLowerCase(Locale.ROOT), enchantsSection.getInt(key, 1));
        }
        return enchantments;
    }

    private CustomDrop parseSimpleDrop(String dropString) {
        if (dropString == null || dropString.isEmpty()) return null;

        try {
            String trimmed = dropString.trim();
            if (trimmed.startsWith(NEXO_PREFIX)) {
                String remainder = trimmed.substring(NEXO_PREFIX.length());
                String[] parts = remainder.contains(",") ? remainder.split(",") : remainder.split(":");
                String nexoId = parts[0].trim();
                int weight = parts.length > 1 ? Integer.parseInt(parts[1]) : 100;
                int amount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                return createDrop(new DropConfigurationRecord(NEXO_PREFIX + nexoId, weight, amount, null, null,
                    0, Map.of(), List.of(), false, null, List.of(), nexoId));
            }

            String[] parts = trimmed.contains(",") ? trimmed.split(",") : trimmed.split(":");
            String material = parts[0].trim();
            if (!material.contains(":")) {
                material = material.toUpperCase(Locale.ROOT);
            }
            int weight = parts.length > 1 ? Integer.parseInt(parts[1]) : 100;
            int amount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;

            return createDrop(material, weight, amount);
        } catch (IllegalArgumentException e) {
            log(Level.WARNING, e, () -> "Failed to parse drop: " + dropString);
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
        info(() -> "Loaded " + defaults.size() + " default drops (ores, gems, treasures).");
    }

    private CustomDrop createDrop(String id, int weight, int amount) {
        return createDrop(new DropConfigurationRecord(id, weight, amount, null, null,
            0, null, null, false, null, null, null));
    }

    private CustomDrop createDrop(DropConfigurationRecord config) {
        int sanitizedWeight = sanitizeWeight(config.identifier(), config.weight());
        int sanitizedAmount = sanitizeAmount(config.identifier(), config.amount());
        int sanitizedModelData = Math.max(0, config.customModelData());
        if (sanitizedWeight == config.weight()
                && sanitizedAmount == config.amount()
                && sanitizedModelData == config.customModelData()) {
            return new CustomDrop(config);
        }
        return new CustomDrop(new DropConfigurationRecord(
            config.identifier(),
            sanitizedWeight,
            sanitizedAmount,
            config.customName(),
            config.lore(),
            sanitizedModelData,
            config.enchantments(),
            config.itemFlags(),
            config.glowing(),
            config.permission(),
            config.biomes(),
            config.nexoItemId()
        ));
    }

    private static final String DROP_LABEL = "Drop '";
    private static final int MIN_DROP_AMOUNT = 1;
    private static final int MAX_DROP_AMOUNT = 64;

    private int sanitizeWeight(String id, int weight) {
        if (weight >= 1) {
            return weight;
        }
        warning(() -> DROP_LABEL + id + "' had non-positive weight " + weight
            + "; clamping to 1. Fix the entry to silence this warning.");
        return 1;
    }

    private int sanitizeAmount(String id, int amount) {
        if (amount >= MIN_DROP_AMOUNT && amount <= MAX_DROP_AMOUNT) {
            return amount;
        }
        int clamped = Math.clamp(amount, MIN_DROP_AMOUNT, MAX_DROP_AMOUNT);
        warning(() -> DROP_LABEL + id + "' had out-of-range amount " + amount
            + "; clamping to " + clamped + ". Fix the entry to silence this warning.");
        return clamped;
    }

    /// Selects a random drop with a luck multiplier applied to rare/legendary
    /// drop weights. A multiplier &gt; 1.0 makes rare catches more probable.
    ///
    /// @param player         the fishing player (permission + biome checks)
    /// @param biomeName      current biome key string
    /// @param luckMultiplier from `MythicRodRewardRollEvent`, clamped to at least 0.01
    public CustomDrop getRandomDrop(PlatformPlayer player, String biomeName, double luckMultiplier) {
        if (player == null || !player.isOnline()) {
            fine(() -> "Skipping reward roll because player is null or offline");
            return null;
        }
        List<CustomDrop> allDrops = getAllDrops();
        return selector.selectDrop(allDrops, player, biomeName, luckMultiplier);
    }

    /// Luck-neutral overload.
    public CustomDrop getRandomDrop(PlatformPlayer player, String biomeName) {
        if (player == null || !player.isOnline()) {
            fine(() -> "Skipping reward roll because player is null or offline");
            return null;
        }
        // Snapshot the current drop list; the AtomicReference guarantees we see a
        // complete table even if a reload is racing on another thread.
        List<CustomDrop> allDrops = getAllDrops();
        fine(() -> "Reward roll has " + allDrops.size() + " configured drops available");
        for (CustomDrop drop : allDrops) {
            fine(() -> "Candidate drop: " + drop.getIdentifier() + " (weight: " + drop.getWeight() + ")");
        }
        CustomDrop result = selector.selectDrop(allDrops, player, biomeName);
        fine(() -> "Selected reward drop: " + (result != null ? result.getIdentifier() : "none"));
        return result;
    }

    @Override
    public List<CustomDrop> getAllDrops() {
        return dropCategories().values().stream()
            .flatMap(List::stream)
            .toList();
    }

    @Override
    public List<CustomDrop> getDrops(String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }

        List<CustomDrop> drops = dropCategories().get(category.toLowerCase(Locale.ROOT));
        if (drops == null || drops.isEmpty()) {
            return List.of();
        }
        return List.copyOf(drops);
    }

    @Override
    public Set<String> getCategories() {
        return Set.copyOf(dropCategories().keySet());
    }

    public List<CustomDrop> getEligibleDrops(PlatformPlayer player, String biomeName) {
        if (player == null || !player.isOnline()) {
            return List.of();
        }
        return selector.getEligibleDrops(getAllDrops(), player, biomeName);
    }

    public int getEffectiveWeight(CustomDrop drop, double luckMultiplier) {
        if (drop == null) {
            return 0;
        }
        return selector.getEffectiveWeight(drop.getWeight(), luckMultiplier);
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

    @Override
    public int getTotalDropCount() {
        return dropCategories().values().stream().mapToInt(List::size).sum();
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void reload(PlatformConfiguration config) {
        awaitAsyncPersistenceOperations();
        loadDrops(config);
    }

    public void reload(PlatformConfiguration config, File sourceFile) {
        awaitAsyncPersistenceOperations();
        loadDrops(config, sourceFile);
    }

    public void beginAsyncPersistenceOperation() {
        synchronized (asyncPersistenceMonitor) {
            pendingAsyncPersistenceOperations++;
        }
    }

    public void endAsyncPersistenceOperation() {
        synchronized (asyncPersistenceMonitor) {
            if (pendingAsyncPersistenceOperations > 0) {
                pendingAsyncPersistenceOperations--;
            }
            if (pendingAsyncPersistenceOperations == 0) {
                asyncPersistenceMonitor.notifyAll();
            }
        }
    }

    public void awaitAsyncPersistenceOperations() {
        synchronized (asyncPersistenceMonitor) {
            while (pendingAsyncPersistenceOperations > 0) {
                try {
                    asyncPersistenceMonitor.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log(Level.WARNING, e, () -> "Interrupted while waiting for pending drop persistence operations");
                    return;
                }
            }
        }
    }

    /// Update an existing drop with new properties.
    ///
    /// Thread safe: the category list is a {@link CopyOnWriteArrayList}, so
    /// `set()` is atomic and concurrent readers are unaffected.
    ///
    /// @param dropId     The drop identifier
    /// @param category   The category name
    /// @param weight     The new relative roll weight
    /// @param amount     The new amount
    /// @param customName The new custom name (can be null)
    /// @param lore       The new lore list
    /// @param glowing    Whether the item should glow
    public void updateDrop(String dropId, String category, int weight, int amount,
                           String customName, List<String> lore, boolean glowing) {
        List<CustomDrop> drops = dropCategories().get(category.toLowerCase(Locale.ROOT));
        if (drops == null) return;

        for (int i = 0; i < drops.size(); i++) {
            CustomDrop existing = drops.get(i);
            if (existing.getIdentifier().equals(dropId)) {
                drops.set(i, copyDropWithEdits(new EditableDropFields(
                    existing.getIdentifier(),
                    weight,
                    amount,
                    customName,
                    lore,
                    existing.getCustomModelData(),
                    existing.getEnchantments(),
                    existing.getItemFlags(),
                    glowing,
                    existing.getPermission(),
                    existing.getBiomes()
                )));
                info(() -> "Updated drop: " + dropId + " in category: " + category);
                return;
            }
        }
    }

    /// Adds a drop to a category from a {@link EditableDropFields} value object.
    ///
    /// Returns `null` when the category or fields are invalid.
    public CustomDrop addDrop(String category, EditableDropFields fields) {
        if (category == null || category.isBlank() || fields == null) {
            return null;
        }
        String normalizedIdentifier = normalizeEditedIdentifier(fields.identifier());
        if (normalizedIdentifier == null) {
            return null;
        }
        if (fields.weight() <= 0 || fields.amount() <= 0 || fields.customModelData() < 0) {
            return null;
        }

        String categoryKey = category.toLowerCase(Locale.ROOT);
        String nexoItemId = null;
        if (normalizedIdentifier.regionMatches(true, 0, NEXO_PREFIX, 0, NEXO_PREFIX.length())) {
            nexoItemId = normalizedIdentifier.substring(NEXO_PREFIX.length());
        }
        String normalizedPermission = fields.permission() == null || fields.permission().isBlank()
            ? null
            : fields.permission().trim();

        CustomDrop drop = new CustomDrop(new DropConfigurationRecord(
            normalizedIdentifier,
            fields.weight(),
            fields.amount(),
            fields.customName(),
            fields.lore(),
            fields.customModelData(),
            fields.enchantments(),
            fields.itemFlags(),
            fields.glowing(),
            normalizedPermission,
            fields.biomes(),
            nexoItemId
        ));
        CustomDrop scopedDrop = applyImplicitCategoryConditions(categoryKey, List.of(drop)).get(0);

        List<CustomDrop> existingDrops = dropCategories().get(categoryKey);
        if (existingDrops == null) {
            CopyOnWriteArrayList<CustomDrop> created = new CopyOnWriteArrayList<>();
            created.add(scopedDrop);
            dropCategories().put(categoryKey, created);
        } else {
            existingDrops.add(scopedDrop);
        }
        info(() -> "Added drop: " + scopedDrop.getIdentifier() + " to category: " + categoryKey);
        return scopedDrop;
    }

    /// Updates the exact drop instance selected by the GUI.
    ///
    /// Identifier-based updates are ambiguous when a category contains two
    /// drops with the same material. The GUI passes the live {@link CustomDrop}
    /// object it opened so only that row is replaced.
    ///
    /// @return `true` when the selected drop was still present
    public boolean updateDrop(CustomDrop targetDrop, String category, int weight, int amount,
                              String customName, List<String> lore, boolean glowing) {
        if (targetDrop == null) return false;
        return updateDrop(targetDrop, category, new EditableDropFields(
            targetDrop.getIdentifier(),
            weight,
            amount,
            customName,
            lore,
            targetDrop.getCustomModelData(),
            targetDrop.getEnchantments(),
            targetDrop.getItemFlags(),
            glowing,
            targetDrop.getPermission(),
            targetDrop.getBiomes()
        ));
    }

    /// Replaces the selected drop instance with the supplied {@link EditableDropFields}.
    ///
    /// @return `true` when the target was still present and the new fields validated
    public boolean updateDrop(CustomDrop targetDrop, String category, EditableDropFields fields) {
        if (!hasUpdatableArguments(targetDrop, category, fields)) return false;
        String normalizedIdentifier = normalizeEditedIdentifier(fields.identifier());
        if (normalizedIdentifier == null) return false;

        List<CustomDrop> drops = dropCategories().get(category.toLowerCase(Locale.ROOT));
        if (drops == null) return false;

        EditableDropFields sanitized = sanitizeFields(fields, normalizedIdentifier);
        return replaceTarget(drops, targetDrop, category, sanitized);
    }

    private boolean hasUpdatableArguments(CustomDrop targetDrop, String category, EditableDropFields fields) {
        if (targetDrop == null || category == null || category.isBlank() || fields == null) return false;
        if (fields.identifier() == null || fields.identifier().isBlank()) return false;
        return fields.weight() > 0 && fields.amount() > 0 && fields.customModelData() >= 0;
    }

    private EditableDropFields sanitizeFields(EditableDropFields fields, String normalizedIdentifier) {
        String normalizedPermission = fields.permission() == null || fields.permission().isBlank()
            ? null
            : fields.permission().trim();
        return new EditableDropFields(
            normalizedIdentifier,
            fields.weight(),
            fields.amount(),
            fields.customName(),
            fields.lore() == null ? List.of() : List.copyOf(fields.lore()),
            fields.customModelData(),
            fields.enchantments() == null ? Map.of() : Map.copyOf(fields.enchantments()),
            fields.itemFlags() == null ? List.of() : List.copyOf(fields.itemFlags()),
            fields.glowing(),
            normalizedPermission,
            fields.biomes() == null ? List.of() : List.copyOf(fields.biomes())
        );
    }

    private boolean replaceTarget(List<CustomDrop> drops, CustomDrop targetDrop, String category, EditableDropFields fields) {
        for (int i = 0; i < drops.size(); i++) {
            CustomDrop existing = drops.get(i);
            if (existing == targetDrop) {
                CustomDrop editedDrop = copyDropWithEdits(fields);
                drops.set(i, editedDrop);
                info(() -> "Updated drop: " + existing.getIdentifier()
                    + " -> " + editedDrop.getIdentifier()
                    + " in category: " + category);
                return true;
            }
        }
        return false;
    }

    /// Delete a drop from a category.
    ///
    /// Thread safe: {@link CopyOnWriteArrayList#removeIf} performs a
    /// copy-on-write replace so concurrent readers are unaffected.
    ///
    /// @param dropId   The drop identifier
    /// @param category The category name
    public void deleteDrop(String dropId, String category) {
        List<CustomDrop> drops = dropCategories().get(category.toLowerCase(Locale.ROOT));
        if (drops == null) return;

        boolean removed = drops.removeIf(d -> d.getIdentifier().equals(dropId));
        if (removed) {
            info(() -> "Deleted drop: " + dropId + " from category: " + category);
        }
    }

    /// Deletes the exact drop instance selected by the GUI.
    ///
    /// @return `true` when the selected drop was still present
    public boolean deleteDrop(CustomDrop targetDrop, String category) {
        if (targetDrop == null || category == null || category.isBlank()) {
            return false;
        }

        List<CustomDrop> drops = dropCategories().get(category.toLowerCase(Locale.ROOT));
        if (drops == null) {
            return false;
        }

        boolean removed = drops.removeIf(drop -> drop == targetDrop);
        if (removed) {
            info(() -> "Deleted drop: " + targetDrop.getIdentifier() + " from category: " + category);
        }
        return removed;
    }

    private CustomDrop copyDropWithEdits(EditableDropFields fields) {
        String trimmedIdentifier = fields.identifier();
        String nexoItemId = null;
        if (trimmedIdentifier.regionMatches(true, 0, NEXO_PREFIX, 0, NEXO_PREFIX.length())) {
            nexoItemId = trimmedIdentifier.substring(NEXO_PREFIX.length());
            trimmedIdentifier = NEXO_PREFIX + nexoItemId;
        }

        DropConfigurationRecord newConfig = new DropConfigurationRecord(
            trimmedIdentifier,
            fields.weight(),
            fields.amount(),
            fields.customName(),
            fields.lore(),
            fields.customModelData(),
            fields.enchantments(),
            fields.itemFlags(),
            fields.glowing(),
            fields.permission(),
            fields.biomes(),
            nexoItemId
        );
        return new CustomDrop(newConfig);
    }

    private String normalizeEditedIdentifier(String identifier) {
        String trimmedIdentifier = identifier == null ? "" : identifier.trim();
        if (trimmedIdentifier.isEmpty()) {
            return null;
        }
        if (trimmedIdentifier.regionMatches(true, 0, NEXO_PREFIX, 0, NEXO_PREFIX.length())) {
            String nexoItemId = trimmedIdentifier.substring(NEXO_PREFIX.length()).trim();
            return nexoItemId.isEmpty() ? null : NEXO_PREFIX + nexoItemId;
        }
        return trimmedIdentifier;
    }

    /// Saves the current in-memory drop table back to the source configuration file.
    public void saveDropsConfig() {
        saveDrops();
    }

    public void saveDrops() {
        synchronized (persistenceFileLock) {
            if (dropsConfig == null || dropsFile == null) {
                throw new IllegalStateException("Drop configuration source file is not available for saving");
            }

            dropsConfig.set("drops", null);
            dropsConfig.set("biome-drops", null);
            dropsConfig.set("fishing.drops", null);
            dropCategories().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> dropsConfig.set("drops." + entry.getKey(), serializeCategory(entry.getValue())));

            try {
                dropsConfig.save(dropsFile);
                info(() -> "Drops configuration saved to " + dropsFile.getName() + " (" + getTotalDropCount() + " total drops)");
            } catch (IOException e) {
                throw new IllegalStateException("Failed to save drops configuration", e);
            }
        }
    }

    private List<Map<String, Object>> serializeCategory(List<CustomDrop> drops) {
        List<Map<String, Object>> serializedDrops = new ArrayList<>(drops.size());
        for (CustomDrop drop : drops) {
            serializedDrops.add(serializeDrop(drop));
        }
        return serializedDrops;
    }

    private Map<String, Object> serializeDrop(CustomDrop drop) {
        Map<String, Object> serializedDrop = new LinkedHashMap<>();

        if (drop.isNexoItem()) {
            serializedDrop.put(KEY_NEXO_ITEM, drop.getNexoItemId());
        } else {
            serializedDrop.put(KEY_IDENTIFIER, drop.getIdentifier());
        }

        serializedDrop.put(KEY_WEIGHT, drop.getWeight());
        serializedDrop.put(KEY_AMOUNT, drop.getAmount());

        if (drop.getCustomName() != null && !drop.getCustomName().isBlank()) {
            serializedDrop.put(KEY_CUSTOM_NAME, drop.getCustomName());
        }
        if (!drop.getLore().isEmpty()) {
            serializedDrop.put(KEY_LORE, new ArrayList<>(drop.getLore()));
        }
        if (drop.getCustomModelData() > 0) {
            serializedDrop.put(KEY_CUSTOM_MODEL_DATA, drop.getCustomModelData());
        }
        if (!drop.getEnchantments().isEmpty()) {
            serializedDrop.put(KEY_ENCHANTMENTS, new LinkedHashMap<>(drop.getEnchantments()));
        }
        if (!drop.getItemFlags().isEmpty()) {
            serializedDrop.put(KEY_ITEM_FLAGS, new ArrayList<>(drop.getItemFlags()));
        }
        if (drop.isGlowing()) {
            serializedDrop.put(KEY_GLOW, true);
        }
        if (drop.getPermission() != null && !drop.getPermission().isBlank()) {
            serializedDrop.put(KEY_PERMISSION, drop.getPermission());
        }
        if (!drop.getBiomes().isEmpty()) {
            serializedDrop.put(KEY_BIOMES, new ArrayList<>(drop.getBiomes()));
        }

        return serializedDrop;
    }

    private List<String> readFlagList(PlatformConfiguration dropSection) {
        List<String> itemFlags = dropSection.getStringList("item-flags");
        if (!itemFlags.isEmpty()) {
            return itemFlags;
        }

        itemFlags = dropSection.getStringList(KEY_ITEM_FLAGS);
        if (!itemFlags.isEmpty()) {
            return itemFlags;
        }

        return dropSection.getStringList("flags");
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int readConfiguredWeight(
        PlatformConfiguration dropSection,
        String dropKey,
        int defaultValue,
        DropLoadReport report
    ) {
        if (dropSection.contains(KEY_WEIGHT)) {
            return dropSection.getInt(KEY_WEIGHT, defaultValue);
        }
        if (dropSection.contains(KEY_CHANCE_LEGACY)) {
            report.migratedWeightAliases++;
            return dropSection.getInt(KEY_CHANCE_LEGACY, defaultValue);
        }
        warning(() -> DROP_LABEL + dropKey + "' is missing 'weight'; using " + defaultValue + ".");
        return defaultValue;
    }

    private int readMappedWeight(
        Map<?, ?> mappedDrop,
        String category,
        String identifier,
        int defaultValue,
        DropLoadReport report
    ) {
        Object configuredWeight = mappedDrop.get(KEY_WEIGHT);
        if (configuredWeight != null) {
            return asInt(configuredWeight, defaultValue);
        }

        Object migratedWeight = mappedDrop.get(KEY_CHANCE_LEGACY);
        if (migratedWeight != null) {
            report.migratedWeightAliases++;
            return asInt(migratedWeight, defaultValue);
        }

        warning(() -> DROP_LABEL + identifier + "' in category '" + category
            + "' is missing 'weight'; using " + defaultValue + ".");
        return defaultValue;
    }

    private Object firstMappedValue(Map<?, ?> map, String primaryKey, String fallbackKey) {
        Object primary = map.get(primaryKey);
        return primary != null ? primary : map.get(fallbackKey);
    }

    private int asInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException _) {
            return defaultValue;
        }
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> listValue)) {
            return List.of();
        }

        List<String> result = new ArrayList<>(listValue.size());
        for (Object entry : listValue) {
            if (entry != null) {
                result.add(String.valueOf(entry));
            }
        }
        return result;
    }

    private Map<String, Integer> asIntMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String key = asString(entry.getKey());
            if (key == null || key.isBlank()) {
                continue;
            }
            result.put(key.toLowerCase(Locale.ROOT), asInt(entry.getValue(), 1));
        }
        return result;
    }

    private Logger logger() {
        return Logger.getLogger(loggerName);
    }

    private void info(String message) {
        logger().info(message);
    }

    private void info(Supplier<String> messageSupplier) {
        logger().log(Level.INFO, messageSupplier);
    }

    private void warning(Supplier<String> messageSupplier) {
        logger().log(Level.WARNING, messageSupplier);
    }

    private void fine(Supplier<String> messageSupplier) {
        logger().log(Level.FINE, messageSupplier);
    }

    private void log(Level level, Throwable thrown, Supplier<String> messageSupplier) {
        logger().log(level, thrown, messageSupplier);
    }

    @SafeVarargs
    private <T> List<T> firstNonEmpty(List<T>... candidates) {
        for (List<T> candidate : candidates) {
            if (candidate != null && !candidate.isEmpty()) {
                return candidate;
            }
        }
        return List.of();
    }

    private static final class DropLoadReport {
        private int migratedWeightAliases;
    }
}
