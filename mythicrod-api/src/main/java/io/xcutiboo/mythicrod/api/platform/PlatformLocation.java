package io.xcutiboo.mythicrod.api.platform;

/// Immutable world-position snapshot used by MythicRod's platform contracts.
///
/// This type deliberately stores a world name instead of a live world object, so
/// it can cross module and scheduler boundaries without carrying mutable
/// platform state. Resolving the world must happen on the platform side.
public record PlatformLocation(
    String worldName,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
) {
    /// Returns the world identifier for this location snapshot, or `null` when unknown.
    public String getWorldName() { return worldName; }

    /// Returns the x coordinate.
    public double getX() { return x; }

    /// Returns the y coordinate.
    public double getY() { return y; }

    /// Returns the z coordinate.
    public double getZ() { return z; }

    /// Returns the yaw component.
    public float getYaw() { return yaw; }

    /// Returns the pitch component.
    public float getPitch() { return pitch; }
}
