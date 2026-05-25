package io.xcutiboo.mythicrod.paper.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import io.xcutiboo.mythicrod.paper.gui.menus.BaseMenu;

public record MythicRodMenuHolder(BaseMenu menu) implements InventoryHolder {

    public BaseMenu getMenu() {
        return menu;
    }

    @Override
    public Inventory getInventory() {
        return menu.getInventory();
    }
}
