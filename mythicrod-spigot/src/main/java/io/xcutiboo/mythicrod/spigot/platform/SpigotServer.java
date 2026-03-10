package io.xcutiboo.mythicrod.spigot.platform;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;
import io.xcutiboo.mythicrod.spigot.scheduler.BukkitSchedulerService;
import io.xcutiboo.mythicrod.spigot.config.SpigotConfiguration;

public class SpigotServer implements PlatformServer {
    private final MythicRod plugin;
    private final PlatformScheduler scheduler;

    public SpigotServer(MythicRod plugin) {
        this.plugin = plugin;
        this.scheduler = new BukkitSchedulerService(plugin);
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public PlatformPlayer getPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return new SpigotPlayer(player);
        }
        return null;
    }

    @Override
    public boolean isEntityValid(UUID entityId) {
        return Bukkit.getEntity(entityId) != null;
    }

    @Override
    public boolean isNexoEnabled() {
        return Bukkit.getPluginManager().getPlugin("Nexo") != null;
    }

    @Override
    public PlatformConfiguration loadConfiguration(File file) {
        org.bukkit.configuration.file.YamlConfiguration bukkitConfig = 
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        return new SpigotConfiguration(bukkitConfig);
    }

    @Override
    public PlatformConfiguration loadConfiguration(InputStream stream) {
        if (stream == null) return createEmptyConfiguration();
        
        try (java.io.InputStreamReader reader = new java.io.InputStreamReader(stream, com.google.common.base.Charsets.UTF_8)) {
            org.bukkit.configuration.file.YamlConfiguration bukkitConfig = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(reader);
            return new SpigotConfiguration(bukkitConfig);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load configuration from stream", e);
            return createEmptyConfiguration();
        }
    }

    @Override
    public PlatformConfiguration createEmptyConfiguration() {
        return new SpigotConfiguration(new org.bukkit.configuration.file.YamlConfiguration());
    }

    @Override
    public PlatformCommandSender getCommandSender(String name) {
        if ("CONSOLE".equalsIgnoreCase(name)) {
            return new SpigotCommandSender(Bukkit.getConsoleSender());
        }
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            return new SpigotPlayer(player);
        }
        return new SpigotCommandSender(Bukkit.getConsoleSender()); // Fallback
    }

    @Override
    public void dispatchCommandConsole(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @Override
    public void broadcastMessage(String message) {
        Bukkit.broadcastMessage(message);
    }
}
