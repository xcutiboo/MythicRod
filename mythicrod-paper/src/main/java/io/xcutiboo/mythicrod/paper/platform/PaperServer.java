package io.xcutiboo.mythicrod.paper.platform;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import java.io.InputStreamReader;

import org.bukkit.World;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.api.platform.PlatformWorld;
import io.xcutiboo.mythicrod.api.platform.PlatformItemFactory;
import io.xcutiboo.mythicrod.paper.scheduler.FoliaSchedulerService;
import io.xcutiboo.mythicrod.item.ItemFactory;

public class PaperServer implements PlatformServer {
    private final Server server;
    private final PlatformScheduler scheduler;
    // HIGH-008 FIX: ItemFactory (and its NexoItemProvider Class.forName reflection) was
    // instantiated on every getItemFactory() call.  Cache it once at construction time.
    private final PlatformItemFactory itemFactory;

    public PaperServer(Server server, Plugin plugin) {
        this.server = server;
        this.scheduler = new FoliaSchedulerService(plugin);
        this.itemFactory = new ItemFactory(server.getLogger());
    }
    
    @Override
    public Logger getLogger() {
        return server.getLogger();
    }
    
    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }
    
    @Override
    public PlatformPlayer getPlayer(UUID uuid) {
        Player player = server.getPlayer(uuid);
        return player != null ? new PaperPlayer(player) : null;
    }
    
    @Override
    public PlatformCommandSender getCommandSender(String name) {
        Player player = server.getPlayer(name);
        if (player != null) {
            return new PaperPlayer(player);
        }
        return new PaperCommandSender(server.getConsoleSender());
    }
    
    @Override
    public boolean isEntityValid(UUID entityId) {
        return server.getEntity(entityId) != null;
    }
    
    @Override
    public boolean isNexoEnabled() {
        return server.getPluginManager().getPlugin("Nexo") != null;
    }
    
    @Override
    public PlatformConfiguration loadConfiguration(File file) {
        return new PaperConfiguration(YamlConfiguration.loadConfiguration(file));
    }
    
    @Override
    public PlatformConfiguration loadConfiguration(InputStream stream) {
        return new PaperConfiguration(YamlConfiguration.loadConfiguration(new InputStreamReader(stream)));
    }
    
    @Override
    public PlatformConfiguration createEmptyConfiguration() {
        return new PaperConfiguration(new YamlConfiguration());
    }
    
    @Override
    public void dispatchCommandConsole(String command) {
        server.dispatchCommand(server.getConsoleSender(), command);
    }
    
    /**
     * Broadcasts a message to all players on the server using Adventure Component API.
     * 
     * @param message The message to broadcast (will be deserialized from MiniMessage format)
     */
    @Override
    public void broadcastMessage(String message) {
        Component component = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message);
        server.broadcast(component);
    }
    
    @Override
    public PlatformWorld getWorld(String name) {
        World world = server.getWorld(name);
        return world != null ? new PaperWorld(world) : null;
    }
    
    @Override
    public PlatformItemFactory getItemFactory() {
        return itemFactory;
    }
    
    public Server getBukkitServer() {
        return server;
    }
    
    private static class PaperConfiguration implements PlatformConfiguration {
        private final YamlConfiguration config;
        
        PaperConfiguration(YamlConfiguration config) {
            this.config = config;
        }
        
        @Override
        public boolean contains(String path) {
            return config.contains(path);
        }
        
        @Override
        public String getString(String path) {
            return config.getString(path);
        }
        
        @Override
        public String getString(String path, String def) {
            return config.getString(path, def);
        }
        
        @Override
        public int getInt(String path) {
            return config.getInt(path);
        }
        
        @Override
        public int getInt(String path, int def) {
            return config.getInt(path, def);
        }
        
        @Override
        public boolean getBoolean(String path) {
            return config.getBoolean(path);
        }
        
        @Override
        public boolean getBoolean(String path, boolean def) {
            return config.getBoolean(path, def);
        }
        
        @Override
        public double getDouble(String path) {
            return config.getDouble(path);
        }
        
        @Override
        public double getDouble(String path, double def) {
            return config.getDouble(path, def);
        }
        
        @Override
        public List<String> getStringList(String path) {
            return config.getStringList(path);
        }
        
        @Override
        public void set(String path, Object value) {
            config.set(path, value);
        }
        
        @Override
        public Set<String> getKeys(String path, boolean deep) {
            ConfigurationSection section = config.getConfigurationSection(path);
            return section != null ? section.getKeys(deep) : Collections.emptySet();
        }
        
        @Override
        public PlatformConfiguration getSection(String path) {
            ConfigurationSection section = config.getConfigurationSection(path);
            if (section == null) return null;
            YamlConfiguration newConfig = new YamlConfiguration();
            for (String key : section.getKeys(false)) {
                newConfig.set(key, section.get(key));
            }
            return new PaperConfiguration(newConfig);
        }
        
        @Override
        public void save(File file) throws Exception {
            config.save(file);
        }
    }
}
