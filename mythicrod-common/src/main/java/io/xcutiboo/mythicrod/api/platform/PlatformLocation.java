package io.xcutiboo.mythicrod.api.platform;

/**
 * Platform-agnostic representation of a physical location.
 */
public class PlatformLocation {
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;

    public PlatformLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
