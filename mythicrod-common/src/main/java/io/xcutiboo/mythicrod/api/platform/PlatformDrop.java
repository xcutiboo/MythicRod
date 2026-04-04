package io.xcutiboo.mythicrod.api.platform;

import java.util.List;

public interface PlatformDrop {
    
    String getIdentifier();
    
    int getChance();
    
    int getAmount();
    
    boolean isNexoItem();
    
    String getPermission();
    
    List<String> getBiomes();
    
    PlatformItem createItem();
}