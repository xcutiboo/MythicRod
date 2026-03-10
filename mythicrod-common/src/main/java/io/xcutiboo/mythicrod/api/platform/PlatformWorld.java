package io.xcutiboo.mythicrod.api.platform;

public interface PlatformWorld {
    String getName();
    
    /**
     * Drop an item naturally in the world
     */
    void dropItem(PlatformLocation location, PlatformItem item);
}