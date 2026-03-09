package io.xcutiboo.mythicrod.drops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.xcutiboo.mythicrod.api.platform.PlatformDrop;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;

/**
 * Represents a custom fishing drop with all its properties.
 * Platform-agnostic - platforms handle display name/lore formatting.
 */
public class CustomDrop implements PlatformDrop {
    private final String identifier;
    private final int chance;
    private final int amount;
    private String customName;
    private List<String> lore;
    private final Map<String, Integer> enchantments;
    private final List<String> itemFlags;
    private boolean glowing;
    private final String permission;
    private final List<String> biomes;
    private final String nexoItemId;

    public CustomDrop(String identifier, int chance, int amount) {
        this(identifier, chance, amount, null, new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), false, null, new ArrayList<>(), null);
    }

    public CustomDrop(String identifier, int chance, int amount, String customName,
            List<String> lore, Map<String, Integer> enchantments,
            List<String> itemFlags, boolean glowing, String permission,
            List<String> biomes, String nexoItemId) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        if (chance <= 0) {
            throw new IllegalArgumentException("Chance must be positive");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.identifier = identifier;
        this.chance = chance;
        this.amount = amount;
        this.customName = customName;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.enchantments = enchantments != null ? enchantments : new HashMap<>();
        this.itemFlags = itemFlags != null ? itemFlags : new ArrayList<>();
        this.glowing = glowing;
        this.permission = permission;
        this.biomes = biomes != null ? biomes : new ArrayList<>();
        this.nexoItemId = nexoItemId;
    }

    public static CustomDrop createNexoDrop(String nexoItemId, int chance, int amount) {
        return new CustomDrop("nexo:" + nexoItemId, chance, amount, null, new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), false, null, new ArrayList<>(), nexoItemId);
    }

    @Override
    public String getIdentifier() { return isNexoItem() ? "nexo:" + nexoItemId : identifier; }
    
    @Override
    public int getChance() { return chance; }
    
    @Override
    public int getAmount() { return amount; }
    
    public String getCustomName() { return customName; }
    
    public List<String> getLore() { return lore; }
    
    public Map<String, Integer> getEnchantments() { return enchantments; }
    
    public List<String> getItemFlags() { return itemFlags; }
    
    public boolean isGlowing() { return glowing; }
    
    @Override
    public String getPermission() { return permission; }
    
    @Override
    public List<String> getBiomes() { return biomes; }
    
    public String getNexoItemId() { return nexoItemId; }
    
    @Override
    public boolean isNexoItem() { return nexoItemId != null && !nexoItemId.isEmpty(); }

    @Override
    public PlatformItem createItem() {
        // Implementation provided by platform module or an item factory
        throw new UnsupportedOperationException("createItem should be handled by a PlatformItemFactory");
    }

    // Setters
    public void setCustomName(String customName) { this.customName = customName; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    public void addEnchantment(String enchantment, int level) {
        this.enchantments.put(enchantment, level);
    }
    public void addItemFlag(String flag) {
        this.itemFlags.add(flag);
    }
}
