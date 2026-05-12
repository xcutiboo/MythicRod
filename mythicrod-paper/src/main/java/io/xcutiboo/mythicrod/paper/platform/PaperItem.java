package io.xcutiboo.mythicrod.paper.platform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class PaperItem implements PlatformItem {
    private final ItemStack itemStack;

    public PaperItem(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    @Override
    public String getIdentifier() {
        return itemStack.getType().name();
    }

    @Override
    public int getAmount() {
        return itemStack.getAmount();
    }

    @Override
    public String getDisplayName() {
        Component displayName = itemStack.getData(DataComponentTypes.ITEM_NAME);
        return displayName != null 
            ? PlainTextComponentSerializer.plainText().serialize(displayName) 
            : null;
    }

    @Override
    public List<String> getLore() {
        List<Component> lore = itemStack.lore();
        if (lore == null) return List.of();
        return lore.stream()
            .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
            .toList();
    }

    @Override
    public Map<String, Integer> getEnchantments() {
        Map<String, Integer> enchants = new HashMap<>();
        itemStack.getEnchantments().forEach((enchant, level) -> 
            enchants.put(enchant.getKey().getKey(), level));
        return Map.copyOf(enchants);
    }

    @Override
    public List<String> getItemFlags() {
        return List.of();
    }

    @Override
    public boolean isGlowing() {
        Boolean glint = itemStack.getData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        return glint != null && glint;
    }

    @Override
    public boolean isCustom() {
        return false;
    }

    public ItemStack getBukkitItem() {
        return itemStack.clone();
    }
}
