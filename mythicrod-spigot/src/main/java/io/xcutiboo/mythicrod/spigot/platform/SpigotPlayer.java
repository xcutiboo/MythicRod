package io.xcutiboo.mythicrod.spigot.platform;

import java.util.UUID;

import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

public class SpigotPlayer extends SpigotCommandSender implements PlatformPlayer {

    private final Player bukkitPlayer;

    public SpigotPlayer(Player bukkitPlayer) {
        super(bukkitPlayer);
        this.bukkitPlayer = bukkitPlayer;
    }

    @Override
    public UUID getUniqueId() {
        return bukkitPlayer.getUniqueId();
    }

    @Override
    public String getName() {
        return bukkitPlayer.getName();
    }

    @Override
    public boolean isOnline() {
        return bukkitPlayer.isOnline();
    }

    @Override
    public boolean isOp() {
        return bukkitPlayer.isOp();
    }

    @Override
    public void closeInventory() {
        bukkitPlayer.closeInventory();
    }
    
    public Player getBukkitPlayer() {
        return bukkitPlayer;
    }
}
