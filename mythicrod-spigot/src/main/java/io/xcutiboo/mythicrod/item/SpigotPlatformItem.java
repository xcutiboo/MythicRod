package io.xcutiboo.mythicrod.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.api.platform.PlatformItem;

/**
 * Spigot implementation of PlatformItem wrapping a Bukkit ItemStack
 */
public class SpigotPlatformItem implements PlatformItem {

    private final String identifier;
    private final ItemStack itemStack;
    private final boolean isCustom;

    public SpigotPlatformItem(String identifier, ItemStack itemStack, boolean isCustom) {
        this.identifier = identifier;
        this.itemStack = itemStack.clone();
        this.isCustom = isCustom;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int getAmount() {
        return itemStack.getAmount();
    }

    @Override
    public String getDisplayName() {
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            return itemStack.getItemMeta().getDisplayName();
        }
        return itemStack.getType().name(); // Fallback
    }

    @Override
    public List<String> getLore() {
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
            return itemStack.getItemMeta().getLore();
        }
        return new ArrayList<>();
    }

    @Override
    public Map<String, Integer> getEnchantments() {
        Map<String, Integer> enchants = new HashMap<>();
        itemStack.getEnchantments().forEach((enchant, level) -> 
            enchants.put(enchant.getKey().getKey(), level)
        );
        return enchants;
    }

    @Override
    public List<String> getItemFlags() {
        List<String> flags = new ArrayList<>();
        if (itemStack.hasItemMeta()) {
            itemStack.getItemMeta().getItemFlags().forEach(flag -> flags.add(flag.name()));
        }
        return flags;
    }

    @Override
    public boolean isGlowing() {
        if (!itemStack.hasItemMeta()) return false;
        return itemStack.getItemMeta().hasEnchants() && itemStack.getItemMeta().hasItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
    }

    @Override
    public boolean isCustom() {
        return isCustom;
    }
}