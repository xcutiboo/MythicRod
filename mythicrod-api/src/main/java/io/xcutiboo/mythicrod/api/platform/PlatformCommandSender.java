package io.xcutiboo.mythicrod.api.platform;

/// Minimal command/message recipient abstraction used by MythicRod's public API.
///
/// This deliberately exposes only the capabilities MythicRod needs for its
/// platform contracts: sending feedback and checking permissions.
public interface PlatformCommandSender {

    /// Sends a plain text or already-formatted message to this sender.
    ///
    /// @param message message content to deliver
    void sendMessage(String message);

    /// Checks whether the sender holds a permission node.
    ///
    /// @param permission permission node to test
    /// @return `true` when the sender has the permission
    boolean hasPermission(String permission);
}
