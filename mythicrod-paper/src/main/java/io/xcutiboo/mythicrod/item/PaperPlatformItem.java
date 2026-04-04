package io.xcutiboo.mythicrod.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.api.platform.PlatformItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class PaperPlatformItem implements PlatformItem {

    private final String identifier;
    private final ItemStack itemStack;
    private final boolean isCustom;

    public PaperPlatformItem(String identifier, ItemStack itemStack, boolean isCustom) {
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
        Component displayName = itemStack.getData(io.papermc.paper.datacomponent.DataComponentTypes.ITEM_NAME);
        if (displayName != null) {
            return PlainTextComponentSerializer.plainText().serialize(displayName);
        }
        return itemStack.getType().name();
    }

    @Override
    public List<String> getLore() {
        List<Component> lore = itemStack.lore();
        if (lore != null) {
            return lore.stream()
                .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                .toList();
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
        return new ArrayList<>();
    }

    @Override
    public boolean isGlowing() {
        Boolean glint = itemStack.getData(io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        return glint != null && glint;
    }

    @Override
    public boolean isCustom() {
        return isCustom;
    }
}