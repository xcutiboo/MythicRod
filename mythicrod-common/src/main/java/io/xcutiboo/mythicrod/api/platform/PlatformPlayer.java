package io.xcutiboo.mythicrod.api.platform;

import java.util.UUID;

/**
 * Platform-agnostic representation of a Player.
 * Used to avoid Bukkit Player dependencies in the common module.
 */
public interface PlatformPlayer extends PlatformCommandSender {
    
    UUID getUniqueId();
    
    String getName();
    
    boolean isOnline();
    
    boolean isOp();
    
    /**
     * Close any currently open inventory/GUI
     */
    void closeInventory();
}