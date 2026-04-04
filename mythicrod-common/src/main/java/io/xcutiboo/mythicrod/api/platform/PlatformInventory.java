package io.xcutiboo.mythicrod.api.platform;

import java.util.Map;

public interface PlatformInventory {
    
    int getSize();
    
    String getTitle();
    
    boolean isFull();
    
    Map<Integer, PlatformItem> addItem(PlatformItem item);
    
    PlatformItem getItem(int slot);
}