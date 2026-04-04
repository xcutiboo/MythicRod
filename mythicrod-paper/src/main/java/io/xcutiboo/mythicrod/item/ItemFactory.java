package io.xcutiboo.mythicrod.item;

import io.xcutiboo.mythicrod.api.Result;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformItemFactory;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

@RequiredArgsConstructor
public class ItemFactory implements PlatformItemFactory {
    private final NexoItemProvider nexoProvider;

    public ItemFactory(Logger logger) {
        this.nexoProvider = new NexoItemProvider(logger);
    }

    @Override
    public boolean canCreate(String identifier) {
        if (identifier == null || identifier.isEmpty()) return false;
        
        if (identifier.startsWith("nexo:")) {
            return nexoProvider.isAvailable() && nexoProvider.exists(identifier.substring(5));
        }
        
        return Material.matchMaterial(identifier) != null;
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

            // Try exact match first
            Material material = Material.matchMaterial(identifier);
            
            // If that fails, try with minecraft: prefix
            if (material == null) {
                material = Material.matchMaterial("minecraft:" + identifier.toLowerCase(java.util.Locale.ROOT));
            }
            
            // If still null, log what we tried
            if (material == null) {
                return Result.failure("Invalid material: " + identifier + " (tried: " + identifier + ", minecraft:" + identifier.toLowerCase(java.util.Locale.ROOT) + ")");
            }

            ItemStack item = new ItemStack(material, amount);
            return Result.success(new PaperPlatformItem(identifier, item, false));

        } catch (Exception e) {
            return Result.failure("Error creating item " + identifier + ": " + e.getMessage());
        }
    }
}