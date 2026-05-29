package io.xcutiboo.mythicrod.paper.platform;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import io.xcutiboo.mythicrod.api.platform.PlatformLocation;

public final class PaperLocation {

    private PaperLocation() {
        throw new AssertionError("Utility class");
    }

    public static PlatformLocation fromBukkit(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new PlatformLocation(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }

    public static Location toBukkit(PlatformLocation platformLocation, Server server) {
        if (platformLocation == null) {
            return null;
        }
        World world = server.getWorld(platformLocation.getWorldName());
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
