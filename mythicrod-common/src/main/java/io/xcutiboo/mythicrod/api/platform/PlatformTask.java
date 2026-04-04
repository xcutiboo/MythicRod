package io.xcutiboo.mythicrod.api.platform;

public interface PlatformTask {
    
    void cancel();
    
    boolean isCancelled();
}
