package io.xcutiboo.mythicrod.api.platform;

import io.xcutiboo.mythicrod.api.Result;

public interface PlatformItemFactory {
    
    Result<PlatformItem> createItem(String identifier, int amount);
    
    boolean canCreate(String identifier);
}