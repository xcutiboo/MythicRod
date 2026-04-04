package io.xcutiboo.mythicrod.api.gui;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

import java.util.Map;

public interface PlatformMenu {
    
    void open(PlatformPlayer player);

    void onClose();

    boolean shouldReopenOnClose();

    void setContext(Map<String, Object> context);
}
