package io.xcutiboo.mythicrod.paper.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import io.xcutiboo.mythicrod.paper.gui.menus.BaseMenu;

public final class MythicRodMenuHolder implements InventoryHolder {
    private final BaseMenu menu;

    public MythicRodMenuHolder(BaseMenu menu) {
        this.menu = menu;
    }

    public BaseMenu getMenu() {
        return menu;
    }

    @Override
    public Inventory getInventory() {
        return menu.getInventory();
    }
}
