package io.xcutiboo.mythicrod.api.platform;

import java.util.List;

/**
 * Platform-agnostic representation of a drop item
 */
public interface PlatformDrop {
    
    /**
     * @return Raw material string or nexo ID
     */
    String getIdentifier();
    
    /**
     * @return Drop chance percentage
     */
    int getChance();
    
    /**
     * @return Amount to drop
     */
    int getAmount();
    
    /**
     * @return True if this is a custom Nexo item
     */
    boolean isNexoItem();
    
    /**
     * @return Required permission string, null if none
     */
    String getPermission();
    
    /**
     * @return List of biome names where this can drop
     */
    List<String> getBiomes();
    
    /**
     * Get the final platform item representing this drop
     */
    PlatformItem createItem();
}