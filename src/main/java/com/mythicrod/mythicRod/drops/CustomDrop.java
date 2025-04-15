package com.mythicrod.mythicrod.drops;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        this.material = material;
        this.chance = chance;
        this.amount = amount;
        this.customName = customName;
        this.lore = lore;
        this.enchantments = enchantments;
        this.itemFlags = itemFlags;
        this.glowing = glowing;
        this.permission = permission;
        this.biomes = biomes;
    }

    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (customName != null && !customName.isEmpty()) {
                meta.setDisplayName(customName);
            }

            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }

            for (ItemFlag flag : itemFlags) {
                meta.addItemFlags(flag);
            }

            if (glowing && enchantments.isEmpty()) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        for (Map.Entry<Enchantment, Integer> enchant : enchantments.entrySet()) {
            item.addUnsafeEnchantment(enchant.getKey(), enchant.getValue());
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
