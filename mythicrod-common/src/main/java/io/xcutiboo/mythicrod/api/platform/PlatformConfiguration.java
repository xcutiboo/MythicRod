package io.xcutiboo.mythicrod.api.platform;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Platform-agnostic configuration interface to replace FileConfiguration.
 */
public interface PlatformConfiguration {
    
    boolean contains(String path);
    
    String getString(String path);
    
    String getString(String path, String def);
    
    int getInt(String path);
    
    int getInt(String path, int def);
    
    boolean getBoolean(String path);
    
    boolean getBoolean(String path, boolean def);
    
    double getDouble(String path);
    
    double getDouble(String path, double def);
    
    List<String> getStringList(String path);
    
    void set(String path, Object value);
    
    Set<String> getKeys(String path, boolean deep);
    
    PlatformConfiguration getSection(String path);
    
    void save(File file) throws Exception;
}