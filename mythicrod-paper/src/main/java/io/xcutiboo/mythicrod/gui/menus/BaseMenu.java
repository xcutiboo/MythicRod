package io.xcutiboo.mythicrod.gui.menus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.gui.MythicRodMenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Base class for all Paper GUI menus using Adventure API for modern text handling
 */
public abstract class BaseMenu {
    protected final MythicRod plugin;
    protected final UUID playerUuid;
    protected Inventory inventory;
    protected Map<String, Object> context = new HashMap<>();
    protected Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
    protected boolean shouldReopen = false;

    public BaseMenu(MythicRod plugin, Player player) {
        this.plugin = plugin;
        this.playerUuid = player.getUniqueId();
    }

    protected abstract int getSize();
    protected abstract String getTitle();
    protected abstract void build();
    
    protected Player getPlayer() {
        return plugin.getServer().getPlayer(playerUuid);
    }
    
    protected boolean validatePermission() {
        String permission = getRequiredPermission();
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        Player player = getPlayer();
        return player != null && getPlayer().hasPermission(permission);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        Player player = getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            // Use Adventure API for title - parse legacy color codes to Component
            Component titleComponent = MiniMessage.miniMessage().deserialize(getTitle());
            inventory = Bukkit.createInventory(new MythicRodMenuHolder(this), getSize(), titleComponent);
            build();
            player.openInventory(inventory);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[MythicRod-BaseMenu] Error opening menu for " + player.getName(), e);
        }
    }

    public void refresh() {
        Player player = getPlayer();
        if (inventory == null || player == null || !player.isOnline()) {
            return;
        }
        inventory.clear();
        clickHandlers.clear();
        build();
    }

    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> clickHandler) {
        if (inventory == null || item == null || slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
        if (clickHandler != null) {
            clickHandlers.put(slot, clickHandler);
        }
    }

    protected void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    protected void fillEmpty(ItemStack filler) {
        if (inventory == null || filler == null) {
            return;
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    protected void fillRow(int row, ItemStack item) {
        if (inventory == null || item == null || row < 0 || row >= (inventory.getSize() / 9)) {
            return;
        }
        int startSlot = row * 9;
        for (int i = startSlot; i < startSlot + 9; i++) {
            inventory.setItem(i, item);
        }
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        Consumer<InventoryClickEvent> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(event);
        }
    }

    public boolean isMenuInventory(Inventory inv) {
        return inventory != null && inv != null && inventory.equals(inv);
    }

    public void setContext(Map<String, Object> context) {
        if (context != null) {
            this.context = new HashMap<>(context);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T getContext(String key) {
        return (T) context.get(key);
    }

    protected void putContext(String key, Object value) {
        context.put(key, value);
    }

    protected void setShouldReopen(boolean shouldReopen) {
        this.shouldReopen = shouldReopen;
    }

    public boolean shouldReopenOnClose() {
        return shouldReopen;
    }

    public void onClose() {
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public MythicRod getPlugin() {
        return plugin;
    }
    
    public String getRequiredPermission() {
        return null;
    }
    
    /**
     * Send a message to the player viewing this menu.
     * Supports legacy color codes with & prefix.
     */
    protected void sendMessage(String message) {
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(message));
        }
    }
}
