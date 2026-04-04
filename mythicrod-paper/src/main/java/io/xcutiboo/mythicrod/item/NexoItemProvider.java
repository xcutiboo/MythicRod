package io.xcutiboo.mythicrod.item;

import io.xcutiboo.mythicrod.api.Result;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NexoItemProvider {
    
    private final Logger logger;
    private final java.lang.reflect.Method itemFromIdMethod;
    private final java.lang.reflect.Method existsMethod;
    private final boolean isAvailable;

    public NexoItemProvider(Logger logger) {
        this.logger = logger;
        
        Class<?> tempClass = null;
        java.lang.reflect.Method tempItemMethod = null;
        java.lang.reflect.Method tempExistsMethod = null;
        boolean tempAvailable = false;

        if (Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            try {
                tempClass = Class.forName("com.nexomc.nexo.api.NexoItems");
                tempItemMethod = tempClass.getMethod("itemFromId", String.class);
                tempExistsMethod = tempClass.getMethod("exists", String.class);
                tempAvailable = true;
                logger.info("Nexo integration initialized via reflection.");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to initialize Nexo integration via reflection", e);
            }
        }

        this.itemFromIdMethod = tempItemMethod;
        this.existsMethod = tempExistsMethod;
        this.isAvailable = tempAvailable;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean exists(String itemId) {
        if (!isAvailable || existsMethod == null) return false;
        try {
            return (Boolean) existsMethod.invoke(null, itemId);
        } catch (Exception e) {
            return false;
        }
    }

    public Result<ItemStack> createItem(String itemId) {
        if (!isAvailable || itemFromIdMethod == null) {
            return Result.failure("Nexo integration is not available");
        }

        try {
            Object itemBuilder = itemFromIdMethod.invoke(null, itemId);
            if (itemBuilder == null) {
                return Result.failure("Nexo item not found: " + itemId);
            }

            java.lang.reflect.Method buildMethod = itemBuilder.getClass().getMethod("build");
            ItemStack item = (ItemStack) buildMethod.invoke(itemBuilder);
            
            if (item == null || item.getType().isAir()) {
                 return Result.failure("Nexo item built as AIR: " + itemId);
            }
            
            return Result.success(item);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error creating Nexo item: " + itemId, e);
            return Result.failure("Exception creating Nexo item: " + e.getMessage());
        }
    }
}