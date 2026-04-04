package io.xcutiboo.mythicrod.api.platform;

public interface PlatformScheduler {
    
    
    void runAtLocation(PlatformLocation location, Runnable task);
    
    PlatformTask runAtLocationDelayed(PlatformLocation location, Runnable task, long delayTicks);
    
    
    void runForPlayer(PlatformPlayer player, Runnable task);
    
    PlatformTask runForPlayerDelayed(PlatformPlayer player, Runnable task, long delayTicks);
    
    
    void runGlobal(Runnable task);
    
    PlatformTask runGlobalDelayed(Runnable task, long delayTicks);
    
    
    void runAsync(Runnable task);
    
    PlatformTask runAsyncDelayed(Runnable task, long delayTicks);
    
    
    PlatformTask runGlobalRepeating(Runnable task, long initialDelayTicks, long periodTicks);
    
    PlatformTask runAsyncRepeating(Runnable task, long initialDelayMillis, long periodMillis);
}