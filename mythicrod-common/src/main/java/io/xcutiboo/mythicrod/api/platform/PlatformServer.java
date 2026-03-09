package io.xcutiboo.mythicrod.api.platform;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Core platform abstraction representing the server environment.
 */
public interface PlatformServer {
    
    /**
     * @return The platform logger
     */
    Logger getLogger();
    
    /**
     * @return The platform-specific scheduler
     */
    PlatformScheduler getScheduler();
    
    /**
     * Get an online player by UUID
     */
    PlatformPlayer getPlayer(UUID uuid);
    
    /**
     * Check if an entity exists and is valid/alive
     */
    boolean isEntityValid(UUID entityId);
    
    /**
     * @return True if Nexo is installed and enabled
     */
    boolean isNexoEnabled();
    
    /**
     * Load a configuration file
     */
    PlatformConfiguration loadConfiguration(java.io.File file);
    
    /**
     * Load a configuration from an input stream
     */
    PlatformConfiguration loadConfiguration(java.io.InputStream stream);
    
    /**
     * Create an empty configuration
     */
    PlatformConfiguration createEmptyConfiguration();
}