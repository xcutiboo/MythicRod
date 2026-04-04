package io.xcutiboo.mythicrod.paper.integration;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.paper.util.PrettyLogger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Enhanced Nexo integration with proper event handling.
 * Waits for Nexo to fully load before attempting to use its API.
 */
public class NexoIntegrationListener implements Listener {
    
    private final MythicRod plugin;
    private final PrettyLogger logger;
    
    private Method itemFromIdMethod;
    private Method existsMethod;
    private Method idFromItemMethod;
    private boolean isAvailable = false;
    private boolean itemsLoaded = false;

    public NexoIntegrationListener(MythicRod plugin, PrettyLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Try initial initialization
        initializeNexo();
        // Schedule a delayed check (100 ticks ≈ 5 s) to catch the case where Nexo loads
        // items asynchronously after its PluginEnableEvent fires.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!itemsLoaded) {
                checkItemsLoaded();
            }
        }, 100L);
    }

    /**
     * Attempts to initialize Nexo integration via reflection.
     * Called on plugin enable and when Nexo enables.
     */
    private void initializeNexo() {
        if (isAvailable) return;
        
        if (!Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            return;
        }

        try {
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            itemFromIdMethod = nexoItemsClass.getMethod("itemFromId", String.class);
            existsMethod = nexoItemsClass.getMethod("exists", String.class);
            
            // Try to get idFromItem method if available (newer Nexo versions)
            try {
                idFromItemMethod = nexoItemsClass.getMethod("idFromItem", ItemStack.class);
            } catch (NoSuchMethodException e) {
                // Method may not exist in older versions
                idFromItemMethod = null;
            }
            
            isAvailable = true;
            logger.info("Nexo integration initialized via reflection.");
            
            // Check if items are already loaded
            checkItemsLoaded();
        } catch (Exception e) {
            logger.warning("Failed to initialize Nexo integration: " + e.getMessage());
        }
    }

    /**
     * Checks if Nexo items have been loaded.
     * Nexo loads items asynchronously, so we need to wait for them.
     */
    private void checkItemsLoaded() {
        if (!isAvailable || itemsLoaded) return;
        
        try {
            // Try to access Nexo's item registry
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            Method getEntriesMethod = nexoItemsClass.getMethod("getEntries");
            Object entries = getEntriesMethod.invoke(null);
            
            if (entries != null) {
                itemsLoaded = true;
                logger.success("Nexo items loaded successfully!");
            }
        } catch (Exception e) {
            // Items not loaded yet, will wait for event
            logger.info("Waiting for Nexo items to load...");
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if ("Nexo".equals(event.getPlugin().getName())) {
            logger.info("Nexo plugin enabled, initializing integration...");
            initializeNexo();
        }
    }

    // BUG-006 FIX: @EventHandler on Object is silently ignored by Bukkit's HandlerList
    // introspection — the parameter type must extend org.bukkit.event.Event.
    // We cannot import NexoItemsLoadedEvent at compile time, so the delayed-task
    // approach in register() is the correct fallback instead.

    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean areItemsLoaded() {
        return itemsLoaded;
    }

    public boolean exists(String itemId) {
        if (!isAvailable || existsMethod == null) return false;
        try {
            return (Boolean) existsMethod.invoke(null, itemId);
        } catch (Exception e) {
            return false;
        }
    }

    public ItemStack createItem(String itemId) {
        if (!isAvailable || itemFromIdMethod == null) {
            return null;
        }

        try {
            Object itemBuilder = itemFromIdMethod.invoke(null, itemId);
            if (itemBuilder == null) {
                return null;
            }

            Method buildMethod = itemBuilder.getClass().getMethod("build");
            ItemStack item = (ItemStack) buildMethod.invoke(itemBuilder);
            
            if (item == null || item.getType().isAir()) {
                return null;
            }
            
            return item;
        } catch (Exception e) {
            logger.warning("Error creating Nexo item: " + itemId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the Nexo item ID from an ItemStack.
     * Returns null if not a Nexo item.
     */
    public String getNexoId(ItemStack item) {
        if (!isAvailable || idFromItemMethod == null || item == null) {
            return null;
        }

        try {
            Object result = idFromItemMethod.invoke(null, item);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
