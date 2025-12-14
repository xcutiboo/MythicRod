package io.xcutiboo.mythicrod.drops;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
public class CustomDrop {
    private final Material material;
    private final int chance;
    private final int amount;
    private String customName;
    private List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final List<ItemFlag> itemFlags;
    private boolean glowing;
    private final String permission;
    private final List<String> biomes;
    public CustomDrop(Material material, int chance, int amount) {
        this(material, chance, amount, null, new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), false, null, new ArrayList<>());
    }
    public CustomDrop(Material material, int chance, int amount, String customName,
            List<String> lore, Map<Enchantment, Integer> enchantments,
            List<ItemFlag> itemFlags, boolean glowing, String permission,
            List<String> biomes) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        if (chance <= 0) {
            throw new IllegalArgumentException("Chance must be positive");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.material = material;
        this.chance = chance;
        this.amount = amount;
        this.customName = customName;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.enchantments = enchantments != null ? enchantments : new HashMap<>();
        this.itemFlags = itemFlags != null ? itemFlags : new ArrayList<>();
        this.glowing = glowing;
        this.permission = permission;
        this.biomes = biomes != null ? biomes : new ArrayList<>();
    }
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        // Apply custom name
        if (customName != null && !customName.isEmpty()) {
            Component nameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(customName);
            meta.displayName(nameComponent);
        }
        // Apply lore
        if (!lore.isEmpty()) {
            List<Component> loreComponents = lore.stream()
                .map(line -> LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                .collect(Collectors.toList());
            meta.lore(loreComponents);
        }
        // Apply item flags
        for (ItemFlag flag : itemFlags) {
            meta.addItemFlags(flag);
        }
        // Special handling for ENCHANTED_BOOK - use stored enchantments
        if (material == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) meta;
            // Add stored enchantments
            for (Map.Entry<Enchantment, Integer> enchant : enchantments.entrySet()) {
                bookMeta.addStoredEnchant(enchant.getKey(), enchant.getValue(), true);
            }
            // Add glowing effect for books if requested and no enchantments
            // Books need stored enchants to glow, not regular enchants
            if (glowing && enchantments.isEmpty()) {
                bookMeta.addStoredEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                bookMeta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);
            }
            item.setItemMeta(bookMeta);
        } else {
            // Regular items (not enchanted books)
            // Add glowing effect if enabled and no enchantments
            if (glowing && enchantments.isEmpty()) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
            // Apply enchantments to regular items
            for (Map.Entry<Enchantment, Integer> enchant : enchantments.entrySet()) {
                item.addUnsafeEnchantment(enchant.getKey(), enchant.getValue());
            }
        }
        return item;
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
    public String getCustomName() {
        return customName;
    }
    public void setCustomName(String customName) {
        this.customName = customName;
    }
    public List<String> getLore() {
        return lore;
    }
    public void setLore(List<String> lore) {
        this.lore = lore;
    }
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
    public List<ItemFlag> getItemFlags() {
        return itemFlags;
    }
    public boolean isGlowing() {
        return glowing;
    }
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }
    public String getPermission() {
        return permission;
    }
    public List<String> getBiomes() {
        return biomes;
    }
}
