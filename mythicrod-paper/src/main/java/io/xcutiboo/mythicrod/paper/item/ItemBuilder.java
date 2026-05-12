package io.xcutiboo.mythicrod.paper.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.xcutiboo.mythicrod.text.ConfiguredText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Builder for creating ItemStacks through Paper's modern DataComponent API.
 * ItemMeta remains reserved for data that Paper still exposes there, such as PDC.
 */
public class ItemBuilder {
    private final ItemStack item;

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
            Component nameComponent = deserializeConfiguredText(name);
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
                Component component = deserializeConfiguredText(line)
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
            Component component = deserializeConfiguredText(loreLine)
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

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (enchantment != null) {
            ItemEnchantments current = item.getData(enchantmentComponentType());
            ItemEnchantments.Builder builder = ItemEnchantments.itemEnchantments();
            if (current != null) {
                builder.addAll(current.enchantments());
            }
            builder.add(enchantment, level);
            item.setData(enchantmentComponentType(), builder.build());
        }
        return this;
    }

    public ItemBuilder enchantments(Map<String, Integer> enchantments) {
        if (enchantments != null && !enchantments.isEmpty()) {
            Registry<Enchantment> registry = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT);

            ItemEnchantments current = item.getData(enchantmentComponentType());
            ItemEnchantments.Builder builder = ItemEnchantments.itemEnchantments();
            if (current != null) {
                builder.addAll(current.enchantments());
            }

            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String enchantName = entry.getKey().toLowerCase(java.util.Locale.ROOT);
                NamespacedKey key = enchantName.contains(":")
                    ? NamespacedKey.fromString(enchantName)
                    : NamespacedKey.minecraft(enchantName);
                Enchantment enchant = key != null ? registry.get(key) : null;
                if (enchant != null) {
                    builder.add(enchant, entry.getValue());
                }
            }
            item.setData(enchantmentComponentType(), builder.build());
        }
        return this;
    }

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

    public ItemBuilder unbreakable() {
        item.setData(DataComponentTypes.UNBREAKABLE);
        return this;
    }

    public ItemStack build() {
        return item.clone();
    }

    static Component deserializeConfiguredText(String text) {
        return ConfiguredText.parse(text);
    }

    private DataComponentType.Valued<ItemEnchantments> enchantmentComponentType() {
        return item.getType() == Material.ENCHANTED_BOOK
            ? DataComponentTypes.STORED_ENCHANTMENTS
            : DataComponentTypes.ENCHANTMENTS;
    }
}
