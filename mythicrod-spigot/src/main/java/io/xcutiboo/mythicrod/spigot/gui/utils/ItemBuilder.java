package io.xcutiboo.mythicrod.spigot.gui.utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        this.item = new ItemStack(material, Math.max(1, Math.min(amount, 64)));
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        if (item == null) {
            throw new IllegalArgumentException("ItemStack cannot be null");
        }
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null && name != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta != null && lines != null && !lines.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lines) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
        }
        return this;
    }

    public ItemBuilder addLore(String... lines) {
        if (meta != null && lines != null && lines.length > 0) {
            List<String> existingLore = meta.getLore();
            if (existingLore == null) {
                existingLore = new ArrayList<>();
            }
            for (String line : lines) {
                existingLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(existingLore);
        }
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (meta != null && enchantment != null) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    public ItemBuilder enchant(Map<Enchantment, Integer> enchantments) {
        if (meta != null && enchantments != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }
        return this;
    }

    public ItemBuilder removeEnchant(Enchantment enchantment) {
        if (meta != null && enchantment != null) {
            meta.removeEnchant(enchantment);
        }
        return this;
    }

    public ItemBuilder flag(ItemFlag flag) {
        if (meta != null && flag != null) {
            meta.addItemFlags(flag);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null && flags != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (meta != null && glow) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder modelData(int data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(amount, 64)));
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
