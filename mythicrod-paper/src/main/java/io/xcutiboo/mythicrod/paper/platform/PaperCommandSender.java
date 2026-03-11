package io.xcutiboo.mythicrod.paper.platform;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;

/**
 * Paper implementation of PlatformCommandSender that wraps a native Bukkit CommandSender
 */
public class PaperCommandSender implements PlatformCommandSender {
    private final CommandSender sender;
    
    public PaperCommandSender(CommandSender sender) {
        this.sender = sender;
    }
    
    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }
    
    @Override
    public void sendMessage(String message) {
        sender.sendMessage(message);
    }
    
    /**
     * Get the underlying native Bukkit CommandSender
     */
    public CommandSender getBukkitSender() {
        return sender;
    }
    
    /**
     * Factory method to create appropriate wrapper based on sender type
     */
    public static PlatformCommandSender wrap(CommandSender sender) {
        if (sender instanceof Player player) {
            return new PaperPlayer(player);
        }
        return new PaperCommandSender(sender);
    }
}
