package io.xcutiboo.mythicrod.api.platform;

/// Minimal world view used by platform-neutral MythicRod contracts.
///
/// Implementations are platform-bound. Calls that mutate world state must be
/// made on the scheduler owner for the target location.
public interface PlatformWorld {

    /// Returns this world's stable name.
    ///
    /// @return world name
    String getName();

    /// Drops an item at a location in this world.
    ///
    /// @param location target location owned by this world
    /// @param item item to drop
    void dropItem(PlatformLocation location, PlatformItem item);
}
