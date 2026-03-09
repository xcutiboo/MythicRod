package io.xcutiboo.mythicrod.api.platform;

/**
 * Platform-agnostic representation of a physical location.
 */
public interface PlatformLocation {
    
    String getWorldName();
    
    double getX();
    
    double getY();
    
    double getZ();
    
    float getYaw();
    
    float getPitch();
}