package io.xcutiboo.mythicrod.paper.platform;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformLocation;
import io.xcutiboo.mythicrod.api.platform.PlatformWorld;

public class PaperWorld implements PlatformWorld {
    private final World world;

    public PaperWorld(World world) {
        this.world = world;
    }

    @Override
    public String getName() {
        return world.getName();
    }

    @Override
    public void dropItem(PlatformLocation location, PlatformItem item) {
        if (location == null || item == null) return;
        
        Location bukkitLoc = new Location(
            world,
            location.getX(), location.getY(), location.getZ(),
            location.getYaw(), location.getPitch()
        );
        
        if (item instanceof PaperItem paperItem) {
            ItemStack bukkitItem = paperItem.getBukkitItem();
            world.dropItemNaturally(bukkitLoc, bukkitItem);
        }
    }

    public World getBukkitWorld() {
        return world;
    }
}
