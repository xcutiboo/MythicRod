package io.xcutiboo.mythicrod.api.platform;

import java.util.Map;

/**
 * Platform-agnostic representation of an inventory.
 */
public interface PlatformInventory {
    
    int getSize();
    
    String getTitle();
    
    /**
     * Returns true if the inventory is full
     */
    boolean isFull();
    
    /**
     * Add an item to the inventory.
     * @return Map of items that could not fit, or empty map if all fit.
     */
    Map<Integer, PlatformItem> addItem(PlatformItem item);
    
    PlatformItem getItem(int slot);
}