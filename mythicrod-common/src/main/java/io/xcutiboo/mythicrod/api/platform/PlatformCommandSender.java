package io.xcutiboo.mythicrod.api.platform;

public interface PlatformCommandSender {
    void sendMessage(String message);
    
    boolean hasPermission(String permission);
}