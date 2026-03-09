package io.xcutiboo.mythicrod.api.gui;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

import java.util.Map;
import java.util.UUID;

/**
 * Common abstraction for managing Platform Menus.
 */
public interface PlatformGUIManager {

    /**
     * Initializes the GUI Manager.
     */
    void initialize();

    /**
     * Opens a specific menu for a player.
     * @param player The player to open the menu for.
     * @param menuId The ID of the menu to open.
     * @return True if successful, false otherwise.
     */
    boolean openMenu(PlatformPlayer player, String menuId);

    /**
     * Opens a specific menu for a player with context.
     * @param player The player to open the menu for.
     * @param menuId The ID of the menu to open.
     * @param context Additional context for the menu.
     * @return True if successful, false otherwise.
     */
    boolean openMenu(PlatformPlayer player, String menuId, Map<String, Object> context);

    /**
     * Opens the main hub menu for a player.
     */
    void openMainHub(PlatformPlayer player);

    /**
     * Closes the currently open menu for a player.
     */
    void closeMenu(PlatformPlayer player);

    /**
     * Called during plugin shutdown to clean up open menus.
     */
    void shutdown();
}
