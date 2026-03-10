package io.xcutiboo.mythicrod.spigot.item;

import java.util.Optional;

import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.api.platform.CustomItemProvider;
import io.xcutiboo.mythicrod.api.platform.ItemContext;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.item.SpigotPlatformItem;

public class NexoItemProvider implements CustomItemProvider {
    
    private boolean isNexoAvailable() {
        try {
            Class.forName("com.nexomc.nexo.api.NexoItems");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public Optional<PlatformItem> buildItem(String id, ItemContext context) {
        if (!isNexoAvailable()) {
            return Optional.empty();
        }

        try {
            String cleanId = id.toLowerCase().replace("nexo:", "");
            
            // Use reflection to avoid hard dependency compilation errors
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            java.lang.reflect.Method itemFromIdMethod = nexoItemsClass.getMethod("itemFromId", String.class);
            Object nexoItem = itemFromIdMethod.invoke(null, cleanId);
            
            if (nexoItem == null) {
                return Optional.empty();
            }
            
            java.lang.reflect.Method buildMethod = nexoItem.getClass().getMethod("build");
            ItemStack item = (ItemStack) buildMethod.invoke(nexoItem);
            
            if (item != null) {
                item.setAmount(context.getAmount());
                return Optional.of(new SpigotPlatformItem("nexo:" + cleanId, item, true));
            }
            
        } catch (Exception e) {
            // Log warning handled by manager
        }

        return Optional.empty();
    }
}
