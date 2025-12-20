package io.xcutiboo.mythicrod.paper.platform;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;

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
    
    public CommandSender getBukkitSender() {
        return sender;
    }
    
    public static PlatformCommandSender wrap(CommandSender sender) {
        if (sender instanceof Player player) {
            return new PaperPlayer(player);
        }
        return new PaperCommandSender(sender);
    }
}
