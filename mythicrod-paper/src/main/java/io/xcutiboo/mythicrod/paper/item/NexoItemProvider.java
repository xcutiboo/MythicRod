package io.xcutiboo.mythicrod.paper.item;

import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.api.Result;

public class NexoItemProvider {
    private static final String NEXO_PLUGIN = "Nexo";
    private static final String NEXO_ITEMS_CLASS = "com.nexomc.nexo.api.NexoItems";

    private final String loggerName;
    private final Method itemFromIdMethod;
    private final Method existsMethod;
    private final boolean available;

    public NexoItemProvider(Logger logger) {
        this.loggerName = logger.getName();

        Method tempItemMethod = null;
        Method tempExistsMethod = null;
        boolean tempAvailable = false;

        if (Bukkit.getPluginManager().isPluginEnabled(NEXO_PLUGIN)) {
            try {
                Class<?> nexoItemsClass = Class.forName(NEXO_ITEMS_CLASS);
                tempItemMethod = nexoItemsClass.getMethod("itemFromId", String.class);
                tempExistsMethod = nexoItemsClass.getMethod("exists", String.class);
                tempAvailable = true;
                logger().info("Nexo integration initialized via reflection.");
            } catch (LinkageError | ReflectiveOperationException | SecurityException e) {
                warning(e, () -> "Failed to initialize Nexo integration via reflection");
            }
        }

        this.itemFromIdMethod = tempItemMethod;
        this.existsMethod = tempExistsMethod;
        this.available = tempAvailable;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean exists(String itemId) {
        if (!available || existsMethod == null || itemId == null || itemId.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(existsMethod.invoke(null, itemId));
        } catch (LinkageError | ReflectiveOperationException | SecurityException _) {
            return false;
        }
    }

    public Result<ItemStack> createItem(String itemId) {
        if (!available || itemFromIdMethod == null) {
            return Result.failure("Nexo integration is not available");
        }
        if (itemId == null || itemId.isBlank()) {
            return Result.failure("Nexo item id is empty");
        }
        if (!exists(itemId)) {
            return Result.failure("Nexo item not found: " + itemId);
        }

        try {
            Object itemBuilder = itemFromIdMethod.invoke(null, itemId);
            if (itemBuilder == null) {
                return Result.failure("Nexo item builder unavailable for: " + itemId);
            }

            Method buildMethod = itemBuilder.getClass().getMethod("build");
            ItemStack item = (ItemStack) buildMethod.invoke(itemBuilder);

            if (item == null || item.getType().isAir()) {
                return Result.failure("Nexo item built as AIR: " + itemId);
            }

            return Result.success(item);
        } catch (LinkageError | ReflectiveOperationException | ClassCastException | SecurityException e) {
            warning(e, () -> "Failed to create Nexo item " + itemId);
            return Result.failure("Exception creating Nexo item: " + failureReason(e));
        }
    }

    private Logger logger() {
        return Logger.getLogger(loggerName);
    }

    private void warning(Throwable thrown, Supplier<String> messageSupplier) {
        logger().log(Level.WARNING, thrown, messageSupplier);
    }

    private String failureReason(Throwable thrown) {
        String message = thrown.getMessage();
        return message == null || message.isBlank() ? thrown.getClass().getSimpleName() : message;
    }
}
