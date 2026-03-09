package io.xcutiboo.mythicrod.api;

import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;

public interface MythicRodAPI {
    
    /**
     * Core platform services integration
     */
    PlatformServer getServer();
    
    PlatformScheduler getScheduler();
}
