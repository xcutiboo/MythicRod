package io.xcutiboo.mythicrod.spigot.platform;

import org.bukkit.command.CommandSender;

import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;

public class SpigotCommandSender implements PlatformCommandSender {
    protected final CommandSender sender;

    public SpigotCommandSender(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public void sendMessage(String message) {
        // Not used directly here since MythicRod uses Adventure in Spigot implementation
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }
    
    public CommandSender getBukkitSender() {
        return sender;
    }
}
