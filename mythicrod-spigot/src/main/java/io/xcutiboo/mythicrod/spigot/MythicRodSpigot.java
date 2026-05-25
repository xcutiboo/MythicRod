package io.xcutiboo.mythicrod.spigot;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Skeleton Spigot runtime. The Paper module remains the supported runtime;
 * this module exists so a future Spigot implementation can be filled in
 * without restructuring the project.
 *
 * <p>No fishing logic is wired here. The plugin enables, logs that it is a
 * stub, and disables cleanly.
 */
public final class MythicRodSpigot extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().warning(
            "MythicRod-Spigot is a skeleton build with no runtime logic wired."
            + " Use MythicRod-Paper for the supported runtime."
        );
    }

    @Override
    public void onDisable() {
        // Nothing to clean up. Skeleton module.
    }
}
