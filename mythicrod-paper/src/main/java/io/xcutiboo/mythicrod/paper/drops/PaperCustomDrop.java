package io.xcutiboo.mythicrod.paper.drops;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import io.xcutiboo.mythicrod.drops.CustomDrop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Paper-specific CustomDrop implementation using native Adventure API.
 * This ensures proper text rendering on Paper servers.
 */
public class PaperCustomDrop extends CustomDrop {

    public PaperCustomDrop(Material material, int chance, int amount) {
        super(material, chance, amount);
    }

    public PaperCustomDrop(Material material, int chance, int amount, String customName,
            List<String> lore, Map<Enchantment, Integer> enchantments,
            List<ItemFlag> itemFlags, boolean glowing, String permission,
            List<String> biomes) {
        super(material, chance, amount, customName, lore, enchantments, itemFlags, glowing, permission, biomes);
    }

    /**
     * Create ItemStack using Paper's native Adventure API for text components.
     * This prevents text corruption and ensures consistent rendering.
     */
    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(getMaterial(), getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Apply custom name using Adventure Component (Paper native)
        if (getCustomName() != null && !getCustomName().isEmpty()) {
            Component nameComponent = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(getCustomName());
            meta.displayName(nameComponent);
        }

        // Apply lore using Adventure Components (Paper native)
        if (getLore() != null && !getLore().isEmpty()) {
            List<Component> loreComponents = getLore().stream()
                .map(line -> LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                .toList();
            meta.lore(loreComponents);
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
                bookMeta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);
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
