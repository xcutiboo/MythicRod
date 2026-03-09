package io.xcutiboo.mythicrod.spigot.drops;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import io.xcutiboo.mythicrod.drops.CustomDrop;

/**
 * Spigot-specific CustomDrop implementation using ChatColor for text rendering.
 * Mirrors Paper behavior but uses legacy Bukkit APIs for Spigot compatibility.
 */
public class SpigotCustomDrop extends CustomDrop {

    public SpigotCustomDrop(Material material, int chance, int amount) {
        super(material, chance, amount);
    }

    public SpigotCustomDrop(Material material, int chance, int amount, String customName,
            List<String> lore, Map<Enchantment, Integer> enchantments,
            List<ItemFlag> itemFlags, boolean glowing, String permission,
            List<String> biomes) {
        super(material, chance, amount, customName, lore, enchantments, itemFlags, glowing, permission, biomes, null);
    }

    /**
     * Create ItemStack using legacy ChatColor for Spigot compatibility.
     * Behavior mirrors Paper exactly, just using different text API.
     */
    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(getMaterial(), getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Apply custom name using ChatColor (Spigot legacy)
        if (getCustomName() != null && !getCustomName().isEmpty()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getCustomName()));
        }

        // Apply lore using ChatColor (Spigot legacy)
        if (getLore() != null && !getLore().isEmpty()) {
            List<String> formattedLore = new ArrayList<>();
            for (String line : getLore()) {
                formattedLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(formattedLore);
        }

        // Apply item flags
        for (ItemFlag flag : getItemFlags()) {
            meta.addItemFlags(flag);
        }

        // Special handling for ENCHANTED_BOOK
        if (getMaterial() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) meta;
            for (Map.Entry<Enchantment, Integer> enchant : getEnchantments().entrySet()) {
                bookMeta.addStoredEnchant(enchant.getKey(), enchant.getValue(), true);
            }
            if (isGlowing() && getEnchantments().isEmpty()) {
                bookMeta.addStoredEnchant(Enchantment.UNBREAKING, 1, true);
                bookMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(bookMeta);
        } else {
            // Regular items
            if (isGlowing() && getEnchantments().isEmpty()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
            for (Map.Entry<Enchantment, Integer> enchant : getEnchantments().entrySet()) {
                item.addUnsafeEnchantment(enchant.getKey(), enchant.getValue());
            }
        }

        return item;
    }
}
