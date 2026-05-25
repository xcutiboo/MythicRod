package io.xcutiboo.mythicrod.api.platform;

import java.util.UUID;

/// Player abstraction exposed to MythicRod integrations.
///
/// Provider hooks receive this type so they can inspect player identity,
/// permissions, and inventory state without depending directly on Paper types.
public interface PlatformPlayer extends PlatformCommandSender {

    /// Returns the stable UUID for this player.
    ///
    /// @return stable UUID for this player
    UUID getUniqueId();

    /// Returns the player's current name.
    ///
    /// @return current player name
    String getName();

    /// Returns whether the player is still online.
    ///
    /// @return `true` while the player is still online
    boolean isOnline();

    /// Returns whether the player currently has operator status.
    ///
    /// @return `true` when the player has operator status
    boolean isOp();

    /// Closes the player's currently open inventory, if any.
    void closeInventory();

    /// Returns the player's current inventory view.
    ///
    /// @return inventory view for this player
    PlatformInventory getInventory();
}
