package io.xcutiboo.mythicrod.paper.platform;

import java.util.UUID;

import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

/**
 * Paper implementation of PlatformPlayer that wraps a native Bukkit Player
 */
public class PaperPlayer implements PlatformPlayer {
    private final Player player;
    
    public PaperPlayer(Player player) {
        this.player = player;
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
    
    /**
     * Get the underlying native Bukkit Player
     */
    public Player getBukkitPlayer() {
        return player;
    }
}
