package io.xcutiboo.mythicrod.api.platform;

public interface PlatformWorld {
    String getName();
    
    void dropItem(PlatformLocation location, PlatformItem item);
}