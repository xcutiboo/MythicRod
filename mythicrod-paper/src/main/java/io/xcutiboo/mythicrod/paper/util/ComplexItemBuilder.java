package io.xcutiboo.mythicrod.paper.util;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility class for creating complex ItemStacks from CustomDrops.
 * Uses modern Bukkit APIs compatible with Paper 1.21.11.
 */
public class ComplexItemBuilder {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final MythicRod plugin;
    
    public ComplexItemBuilder(MythicRod plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createItemStack(CustomDrop drop) {
        String identifier = drop.getIdentifier();
        
        if (drop.isNexoItem()) {
            return createNexoItem(drop);
        }
        
        String formattedKey = identifier.toLowerCase(java.util.Locale.ROOT);
        if (!formattedKey.contains(":")) {
            formattedKey = "minecraft:" + formattedKey;
        }
        
        Material material = Registry.MATERIAL.get(NamespacedKey.fromString(formattedKey));
        if (material == null) {
            plugin.getLogger().warning("Invalid material: " + identifier + ", using COD");
            material = Material.COD;
        }
        
        ItemStack item = ItemStack.of(material);
        item.setAmount(drop.getAmount());
        
        if (drop.getCustomName() != null && !drop.getCustomName().isEmpty()) {
            Component nameComponent = MINI_MESSAGE.deserialize(drop.getCustomName());
            item.setData(DataComponentTypes.ITEM_NAME, nameComponent);
        }
        
        if (drop.getLore() != null && !drop.getLore().isEmpty()) {
            List<Component> loreComponents = drop.getLore().stream()
                .map(line -> MINI_MESSAGE.deserialize(line))
                .toList();
            item.lore(loreComponents);
        }
        
        Map<String, Integer> enchantments = drop.getEnchantments();
        if (!enchantments.isEmpty()) {
            ItemEnchantments.Builder builder = ItemEnchantments.itemEnchantments();
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                String keyString = entry.getKey();
                if (keyString == null) continue;
                Enchantment enchant = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(NamespacedKey.fromString(
                        keyString.toLowerCase(java.util.Locale.ROOT).startsWith("minecraft:") 
                            ? keyString 
                            : "minecraft:" + keyString.toLowerCase(java.util.Locale.ROOT)
                    ));
                if (enchant != null) {
                    builder.add(enchant, entry.getValue());
                }
            }
            item.setData(DataComponentTypes.ENCHANTMENTS, builder.build());
        }
        
        if (drop.isGlowing()) {
            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        
        return item;
    }
    
    private ItemStack createNexoItem(CustomDrop drop) {
        String nexoId = drop.getNexoItemId();
        ItemStack item = ItemStack.of(Material.PAPER);
        item.setAmount(drop.getAmount());
        
        Component name = MINI_MESSAGE.deserialize("<red>Nexo: " + nexoId + "</red>");
        item.setData(DataComponentTypes.ITEM_NAME, name);
        
        return item;
    }
    
    public ItemStack createFromBase64(String base64String) {
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64String);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize Base64 item", e);
            return null;
        }
    }
    
    public String serializeToBase64(ItemStack item) {
        byte[] bytes = item.serializeAsBytes();
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }
}
