package io.xcutiboo.mythicrod.paper.util;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class ItemStackFactory {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    public static ItemStack createItemStack(CustomDrop drop) {
        if (drop == null) {
            return null;
        }
        
        String identifier = drop.getIdentifier();
        Material material = Material.matchMaterial(identifier);
        
        if (material == null) {
            material = Material.matchMaterial("minecraft:" + identifier.toLowerCase(java.util.Locale.ROOT));
        }
        
        if (material == null) {
            material = Material.COD;
        }
        
        ItemStack item = new ItemStack(material, drop.getAmount());
        
        if (drop.getCustomName() != null && !drop.getCustomName().isEmpty()) {
            Component nameComponent = MINI_MESSAGE.deserialize(drop.getCustomName());
            item.setData(DataComponentTypes.ITEM_NAME, nameComponent);
        }
        
        if (drop.getLore() != null && !drop.getLore().isEmpty()) {
            List<Component> loreComponents = drop.getLore().stream()
                .map(line -> MINI_MESSAGE.deserialize(line))
                .collect(Collectors.toList());
            item.lore(loreComponents);
        }
        
        return item;
    }
}
