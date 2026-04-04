package io.xcutiboo.mythicrod.api.platform;

import java.util.UUID;

public interface PlatformPlayer extends PlatformCommandSender {
    
    UUID getUniqueId();
    
    String getName();
    
    boolean isOnline();
    
    boolean isOp();
    
    void closeInventory();
    
    PlatformInventory getInventory();
}