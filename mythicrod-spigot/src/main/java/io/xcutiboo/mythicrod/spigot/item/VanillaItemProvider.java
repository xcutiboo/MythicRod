package io.xcutiboo.mythicrod.spigot.item;

import java.util.Optional;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.xcutiboo.mythicrod.api.platform.CustomItemProvider;
import io.xcutiboo.mythicrod.api.platform.ItemContext;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.item.SpigotPlatformItem;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;

public class VanillaItemProvider implements CustomItemProvider {
    @Override
    public Optional<PlatformItem> buildItem(String id, ItemContext context) {
        String cleanId = id.toUpperCase().replace("MINECRAFT:", "");
        Material material;
        try {
            material = Material.valueOf(cleanId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        ItemStack item = new ItemStack(material, context.getAmount());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (context.getName() != null) {
                Component nameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(context.getName());
                meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(nameComponent));
            }

            if (context.getLore() != null && !context.getLore().isEmpty()) {
                java.util.List<String> lore = new ArrayList<>();
                for (String line : context.getLore()) {
                    Component lineComp = LegacyComponentSerializer.legacyAmpersand().deserialize(line);
                    lore.add(LegacyComponentSerializer.legacySection().serialize(lineComp));
                }
                meta.setLore(lore);
            }

            if (context.getCustomModelData() > 0) {
                meta.setCustomModelData(context.getCustomModelData());
            }

            item.setItemMeta(meta);
        }

        return Optional.of(new SpigotPlatformItem(id, item, false));
    }
}
