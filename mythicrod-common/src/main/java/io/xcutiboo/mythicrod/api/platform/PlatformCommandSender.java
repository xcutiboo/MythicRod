package io.xcutiboo.mythicrod.api.platform;

/**
 * Base interface for anything that can receive a message or execute a command.
 * Extended by PlatformPlayer.
 */
public interface PlatformCommandSender {
    /**
     * Send a plain text or mini-message formatted string.
     */
    void sendMessage(String message);
    
    /**
     * @return true if the sender has the permission
     */
    boolean hasPermission(String permission);
}