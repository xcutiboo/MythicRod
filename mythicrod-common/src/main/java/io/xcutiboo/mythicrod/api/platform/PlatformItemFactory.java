package io.xcutiboo.mythicrod.api.platform;

import io.xcutiboo.mythicrod.api.Result;

public interface PlatformItemFactory {
    
    /**
     * Create a PlatformItem based on a raw identifier (e.g. "DIAMOND" or "nexo:custom_sword")
     */
    Result<PlatformItem> createItem(String identifier, int amount);
    
    /**
     * Check if the factory can create the item
     */
    boolean canCreate(String identifier);
}