package io.xcutiboo.mythicrod.api.gui;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

import java.util.Map;

/**
 * Interface representing a generic GUI Menu.
 */
public interface PlatformMenu {
    
    /**
     * Opens the menu for a specific player.
     * @param player The player to open the menu for.
     */
    void open(PlatformPlayer player);

    /**
     * Called when the menu is closed.
     */
    void onClose();

    /**
     * Checks if this menu should be reopened if it is closed.
     * Useful for preventing accidental closures by users.
     */
    boolean shouldReopenOnClose();

    /**
     * Sets context data for the menu.
     */
    void setContext(Map<String, Object> context);
}
