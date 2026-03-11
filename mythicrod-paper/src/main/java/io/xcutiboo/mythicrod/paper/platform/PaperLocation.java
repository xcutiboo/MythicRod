package io.xcutiboo.mythicrod.paper.platform;

import org.bukkit.Location;

import io.xcutiboo.mythicrod.api.platform.PlatformLocation;

/**
 * Factory for converting between Bukkit Location and PlatformLocation
 */
public class PaperLocation {
    
    /**
     * Convert a Bukkit Location to a PlatformLocation
     */
    public static PlatformLocation fromBukkit(Location location) {
        if (location == null) {
            return null;
        }
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
        return new PlatformLocation(
            worldName,
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }
    
    /**
     * Convert a PlatformLocation to a Bukkit Location
     */
    public static Location toBukkit(PlatformLocation platformLocation, org.bukkit.Server server) {
        if (platformLocation == null) {
            return null;
        }
        org.bukkit.World world = server.getWorld(platformLocation.getWorldName());
        if (world == null) {
            return null;
        }
        return new Location(
            world,
            platformLocation.getX(),
            platformLocation.getY(),
            platformLocation.getZ(),
            platformLocation.getYaw(),
            platformLocation.getPitch()
        );
    }
}
