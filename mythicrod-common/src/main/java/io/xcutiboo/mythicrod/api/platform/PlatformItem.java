package io.xcutiboo.mythicrod.api.platform;

import java.util.List;
import java.util.Map;

public interface PlatformItem {
    
    String getIdentifier();
    
    int getAmount();
    
    String getDisplayName();
    
    List<String> getLore();
    
    Map<String, Integer> getEnchantments();
    
    List<String> getItemFlags();
    
    boolean isGlowing();
    
    boolean isCustom();
}