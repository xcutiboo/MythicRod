package io.xcutiboo.mythicrod.api.platform;

import java.util.UUID;
import java.util.logging.Logger;

public interface PlatformServer {
    
    Logger getLogger();
    
    PlatformScheduler getScheduler();
    
    PlatformPlayer getPlayer(UUID uuid);
    
    PlatformCommandSender getCommandSender(String name);
    
    boolean isEntityValid(UUID entityId);
    
    boolean isNexoEnabled();
    
    PlatformConfiguration loadConfiguration(java.io.File file);
    
    PlatformConfiguration loadConfiguration(java.io.InputStream stream);
    
    PlatformConfiguration createEmptyConfiguration();
    
    PlatformWorld getWorld(String name);
    
    PlatformItemFactory getItemFactory();

    void dispatchCommandConsole(String command);

    void broadcastMessage(String message);
}
