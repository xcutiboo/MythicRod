package io.xcutiboo.mythicrod.paper.platform;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;

/**
 * Paper implementation of PlatformServer that wraps a native Bukkit Server
 */
public class PaperServer implements PlatformServer {
    private final Server server;
    
    public PaperServer(Server server) {
        this.server = server;
    }
    
    @Override
    public Logger getLogger() {
        return server.getLogger();
    }
    
    @Override
    public PlatformScheduler getScheduler() {
        // Paper module doesn't use platform scheduler abstraction
        return null;
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
        return new PaperConfiguration(YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(stream)));
    }
    
    @Override
    public PlatformConfiguration createEmptyConfiguration() {
        return new PaperConfiguration(new YamlConfiguration());
    }
    
    @Override
    public void dispatchCommandConsole(String command) {
        server.dispatchCommand(server.getConsoleSender(), command);
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public void broadcastMessage(String message) {
        server.broadcastMessage(message);
    }
    
    /**
     * Get the underlying native Bukkit Server
     */
    public Server getBukkitServer() {
        return server;
    }
    
    /**
     * Paper-specific configuration wrapper
     */
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
        public java.util.List<String> getStringList(String path) {
            return config.getStringList(path);
        }
        
        @Override
        public void set(String path, Object value) {
            config.set(path, value);
        }
        
        @Override
        public java.util.Set<String> getKeys(String path, boolean deep) {
            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(path);
            return section != null ? section.getKeys(deep) : java.util.Collections.emptySet();
        }
        
        @Override
        public PlatformConfiguration getSection(String path) {
            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(path);
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
