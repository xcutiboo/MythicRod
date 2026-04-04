package io.xcutiboo.mythicrod.api.gui;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

import java.util.Map;

public interface PlatformGUIManager {

    void initialize();

    boolean openMenu(PlatformPlayer player, String menuId);

    boolean openMenu(PlatformPlayer player, String menuId, Map<String, Object> context);

    void openMainHub(PlatformPlayer player);

    void closeMenu(PlatformPlayer player);

    void shutdown();
}
