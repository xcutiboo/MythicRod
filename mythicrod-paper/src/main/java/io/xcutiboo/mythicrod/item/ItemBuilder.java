package io.xcutiboo.mythicrod.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Builder for creating ItemStacks with fluent API using modern DataComponent API.
 * All operations use Paper 1.21.11 DataComponent API - no deprecated ItemMeta usage.
 */
public class ItemBuilder {
    private final ItemStack item;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    public ItemBuilder(Material material) {
        this(material, 1);
    }
    
    public ItemBuilder(Material material, int amount) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        this.item = new ItemStack(material, Math.max(1, Math.min(amount, material.getMaxStackSize())));
    }
    
    private ItemBuilder(ItemStack item) {
        this.item = item;
    }
    
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }
    
    public static ItemBuilder from(ItemStack item) {
        return new ItemBuilder(item.clone());
    }
    
    public ItemBuilder name(String name) {
        if (name != null && !name.isEmpty()) {
            Component nameComponent = MINI_MESSAGE.deserialize(name);
            item.setData(DataComponentTypes.ITEM_NAME, nameComponent);
        }
        return this;
    }
    
    public ItemBuilder name(Component name) {
        if (name != null) {
            item.setData(DataComponentTypes.ITEM_NAME, name);
        }
        return this;
    }
    
    public ItemBuilder lore(List<String> loreLines) {
        if (loreLines != null && !loreLines.isEmpty()) {
            List<Component> components = new ArrayList<>();
            for (String line : loreLines) {
                Component component = MINI_MESSAGE.deserialize(line)
                    .decoration(TextDecoration.ITALIC, false);
                components.add(component);
            }
            item.lore(components);
        }
        return this;
    }
    
    public ItemBuilder lore(String... loreLines) {
        if (loreLines != null && loreLines.length > 0) {
            return lore(List.of(loreLines));
        }
        return this;
    }
    
    public ItemBuilder addLore(String loreLine) {
        if (loreLine != null && !loreLine.isEmpty()) {
            List<Component> currentLore = item.lore();
            List<Component> components = new ArrayList<>();
            if (currentLore != null) {
                components.addAll(currentLore);
            }
            Component component = MINI_MESSAGE.deserialize(loreLine)
                .decoration(TextDecoration.ITALIC, false);
            components.add(component);
            item.lore(components);
        }
        return this;
    }
    
    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(amount, item.getMaxStackSize())));
        return this;
    }
    
    /**
     * Adds an enchantment using DataComponent API.
     * 
     * @param enchantment The enchantment to add
     * @param level The level of the enchantment
     * @return This builder for chaining
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (enchantment != null) {
            ItemEnchantments current = item.getData(DataComponentTypes.ENCHANTMENTS);
            ItemEnchantments.Builder builder = ItemEnchantments.itemEnchantments();
            if (current != null) {
                builder.addAll(current.enchantments());
            }
            builder.add(enchantment, level);
            item.setData(DataComponentTypes.ENCHANTMENTS, builder.build());
        }
        return this;
    }
    
    /**
     * Adds multiple enchantments using DataComponent API.
     * 
     * @param enchantments Map of enchantment names to levels
     * @return This builder for chaining
     */
    public ItemBuilder enchantments(Map<String, Integer> enchantments) {
        if (enchantments != null && !enchantments.isEmpty()) {
            Registry<Enchantment> registry = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT);
            
            ItemEnchantments current = item.getData(DataComponentTypes.ENCHANTMENTS);
            ItemEnchantments.Builder builder = ItemEnchantments.itemEnchantments();
            if (current != null) {
                builder.addAll(current.enchantments());
            }
            
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                String enchantName = entry.getKey().toLowerCase(java.util.Locale.ROOT);
                NamespacedKey key = NamespacedKey.minecraft(enchantName);
                Enchantment enchant = registry.get(key);
                if (enchant != null) {
                    builder.add(enchant, entry.getValue());
                }
            }
            item.setData(DataComponentTypes.ENCHANTMENTS, builder.build());
        }
        return this;
    }
    
    /**
     * Sets custom model data using DataComponent API.
     * 
     * @param data The custom model data value
     * @return This builder for chaining
     */
    public ItemBuilder customModelData(int data) {
        if (data > 0) {
            CustomModelData cmd = CustomModelData.customModelData()
                .addFloat(data)
                .build();
            item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, cmd);
        }
        return this;
    }
    
    public ItemBuilder glow() {
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return this;
    }
    
    public ItemBuilder glow(boolean glow) {
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glow);
        return this;
    }
    
    /**
     * Makes item unbreakable using DataComponent API.
     * 
     * @return This builder for chaining
     */
    public ItemBuilder unbreakable() {
        item.setData(DataComponentTypes.UNBREAKABLE);
        return this;
    }
    
    public ItemStack build() {
        return item.clone();
    }
}
