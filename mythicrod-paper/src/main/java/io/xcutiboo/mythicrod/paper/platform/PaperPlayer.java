package io.xcutiboo.mythicrod.paper.platform;

import java.util.UUID;

import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.api.platform.PlatformInventory;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

public class PaperPlayer implements PlatformPlayer {
    private final Player player;

    public PaperPlayer(Player player) {
        this.player = player;
    }

    public Player getBukkitPlayer() {
        return player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    @Override
    public boolean isOnline() {
        return player.isOnline();
    }

    @Override
    public boolean isOp() {
        return player.isOp();
    }

    @Override
    public void closeInventory() {
        player.closeInventory();
    }

    @Override
    public PlatformInventory getInventory() {
        return new PaperInventory(player.getInventory());
    }
}
