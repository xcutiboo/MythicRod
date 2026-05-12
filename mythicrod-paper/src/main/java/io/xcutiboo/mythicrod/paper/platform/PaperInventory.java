package io.xcutiboo.mythicrod.paper.platform;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.api.platform.PlatformInventory;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;

public class PaperInventory implements PlatformInventory {
    private final Inventory inventory;

    public PaperInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public int getSize() {
        return inventory.getSize();
    }

    @Override
    public String getTitle() {
        return "Inventory";
    }

    @Override
    public boolean isFull() {
        return inventory.firstEmpty() == -1;
    }

    @Override
    public Map<Integer, PlatformItem> addItem(PlatformItem item) {
        Map<Integer, PlatformItem> leftover = new HashMap<>();
        if (item instanceof PaperItem paperItem) {
            ItemStack bukkitItem = paperItem.getBukkitItem();
            HashMap<Integer, ItemStack> result = inventory.addItem(bukkitItem);
            
            for (Map.Entry<Integer, ItemStack> entry : result.entrySet()) {
                leftover.put(entry.getKey(), new PaperItem(entry.getValue()));
            }
        }
        return Map.copyOf(leftover);
    }

    @Override
    public PlatformItem getItem(int slot) {
        ItemStack item = inventory.getItem(slot);
        return item != null ? new PaperItem(item) : null;
    }

    public Inventory getBukkitInventory() {
        return inventory;
    }
}
