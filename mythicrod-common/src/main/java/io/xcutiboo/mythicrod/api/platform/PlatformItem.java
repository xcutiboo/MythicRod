package io.xcutiboo.mythicrod.api.platform;

import java.util.List;
import java.util.Map;

/**
 * Platform-agnostic representation of an item.
 * Used to avoid Bukkit ItemStack dependencies in the common module.
 */
public interface PlatformItem {
    
    /**
     * @return The material or item identifier (e.g., "DIAMOND_SWORD" or "nexo:custom_sword")
     */
    String getIdentifier();
    
    int getAmount();
    
    String getDisplayName();
    
    List<String> getLore();
    
    Map<String, Integer> getEnchantments();
    
    List<String> getItemFlags();
    
    boolean isGlowing();
    
    /**
     * @return True if this is a custom item (like Nexo)
     */
    boolean isCustom();
}