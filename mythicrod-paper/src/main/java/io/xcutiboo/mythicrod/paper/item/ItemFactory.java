package io.xcutiboo.mythicrod.paper.item;

import java.util.Locale;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.api.Result;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformItemFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemFactory implements PlatformItemFactory {
    private static final String MINECRAFT_PREFIX = "minecraft:";

    private final NexoItemProvider nexoProvider;

    public ItemFactory(Logger logger) {
        this.nexoProvider = new NexoItemProvider(logger);
    }

    @Override
    public boolean canCreate(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }

        if (identifier.startsWith("nexo:")) {
            return nexoProvider.isAvailable() && nexoProvider.exists(identifier.substring(5));
        }

        return resolveMaterial(identifier) != null;
    }

    @Override
    public Result<PlatformItem> createItem(String identifier, int amount) {
        if (identifier == null || identifier.isEmpty()) {
            return Result.failure("Identifier is null or empty");
        }

        try {
            if (identifier.startsWith("nexo:")) {
                String nexoId = identifier.substring(5);
                Result<ItemStack> result = nexoProvider.createItem(nexoId);

                if (result.isSuccess()) {
                    ItemStack item = result.getValue();
                    item.setAmount(amount);
                    return Result.success(new PaperPlatformItem(identifier, item, true));
                } else {
                    return Result.failure(result.getError());
                }
            }

            Material material = resolveMaterial(identifier);
            if (material == null) {
                return Result.failure("Invalid material: " + identifier
                    + " (tried: " + identifier + ", minecraft:" + identifier.toLowerCase(Locale.ROOT) + ")");
            }

            ItemStack item = new ItemStack(material, amount);
            return Result.success(new PaperPlatformItem(identifier, item, false));

        } catch (Exception e) {
            return Result.failure("Error creating item " + identifier + ": " + e.getMessage());
        }
    }

    private Material resolveMaterial(String identifier) {
        Material material = Material.matchMaterial(identifier);
        if (material == null && identifier.regionMatches(true, 0, MINECRAFT_PREFIX, 0, MINECRAFT_PREFIX.length())) {
            material = Material.matchMaterial(identifier.substring(MINECRAFT_PREFIX.length()));
        }
        if (material == null && !identifier.contains(":")) {
            material = Material.matchMaterial(MINECRAFT_PREFIX + identifier.toLowerCase(Locale.ROOT));
        }
        return material != null && material.isItem() && !material.isAir() ? material : null;
    }
}
