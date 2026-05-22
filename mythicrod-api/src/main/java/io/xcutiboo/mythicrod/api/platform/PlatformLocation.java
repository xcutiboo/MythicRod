package io.xcutiboo.mythicrod.api.platform;

/// Immutable world-position snapshot used by MythicRod's platform contracts.
///
/// This type deliberately stores a world name instead of a live world object, so
/// it can cross module and scheduler boundaries without carrying mutable
/// platform state. Resolving the world must happen on the platform side.
public final class PlatformLocation {
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    /// Creates a platform-neutral location snapshot.
    ///
    /// @param worldName world identifier, or `null` when the world is unavailable
    /// @param x x coordinate
    /// @param y y coordinate
    /// @param z z coordinate
    /// @param yaw view yaw
    /// @param pitch view pitch
    public PlatformLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /// Returns the world identifier for this location snapshot.
    ///
    /// @return world identifier, or `null` when unknown
    public String getWorldName() { return worldName; }

    /// Returns the x coordinate.
    ///
    /// @return x coordinate
    public double getX() { return x; }

    /// Returns the y coordinate.
    ///
    /// @return y coordinate
    public double getY() { return y; }

    /// Returns the z coordinate.
    ///
    /// @return z coordinate
    public double getZ() { return z; }

    /// Returns the yaw component.
    ///
    /// @return view yaw
    public float getYaw() { return yaw; }

    /// Returns the pitch component.
    ///
    /// @return view pitch
    public float getPitch() { return pitch; }
}
