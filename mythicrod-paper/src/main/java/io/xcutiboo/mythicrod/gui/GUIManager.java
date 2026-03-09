package io.xcutiboo.mythicrod.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.gui.menus.BaseMenu;

/**
 * Modern Paper GUI Manager using Adventure Components.
 */
public class GUIManager implements Listener {
    private final MythicRod plugin;
    private final Map<UUID, BaseMenu> openMenus = new HashMap<>();
    private final Map<String, MenuFactory> menuFactories = new HashMap<>();

    public GUIManager(MythicRod plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("GUI Manager initialized");
    }

    public void registerMenu(String menuId, MenuFactory factory) {
        menuFactories.put(menuId.toLowerCase(), factory);
    }

    public boolean openMenu(Player player, String menuId) {
        return openMenu(player, menuId, null);
    }

    public boolean openMenu(Player player, String menuId, Map<String, Object> context) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        MenuFactory factory = menuFactories.get(menuId.toLowerCase());
        if (factory == null) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Unknown menu: " + menuId);
            return false;
        }

        try {
            closeMenu(player);
            BaseMenu menu = factory.create(plugin, player);
            if (menu == null) {
                return false;
            }
            if (context != null) {
                menu.setContext(context);
            }
            menu.open();
            openMenus.put(player.getUniqueId(), menu);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to open menu: " + menuId, e);
            openMenus.remove(player.getUniqueId());
            return false;
        }
    }

    public void openMainHub(Player player) {
        openMenu(player, "main");
    }

    public void closeMenu(Player player) {
        if (player == null) return;
        BaseMenu menu = openMenus.remove(player.getUniqueId());
        if (menu != null) {
            try {
                menu.onClose();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing menu", e);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        // Check for specific exploit vectors like offhand swap, number key swap, and drop
        switch (event.getClick()) {
            case NUMBER_KEY:
            case SWAP_OFFHAND:
            case DROP:
            case CONTROL_DROP:
                event.setCancelled(true);
                break;
            default:
                break;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        if (!menu.isMenuInventory(clicked)) {
            // Prevent shift-clicking items from bottom inventory into the menu
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
        
        // Security Check: Ensure only admins can interact with GUIs
        if (!player.hasPermission("mythicrod.admin") && !player.isOp()) {
            player.closeInventory();
            return;
        }

        try {
            menu.handleClick(event);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MythicRod-GUIManager] Error handling menu interaction. Player action may not have been processed.", e);
            closeMenu(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        if (event.getRawSlots().stream().anyMatch(slot ->
                slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
            
            // Security Check: Ensure only admins can interact with GUIs
            if (!player.hasPermission("mythicrod.admin") && !player.isOp()) {
                player.closeInventory();
                plugin.getLogger().log(Level.WARNING, "Error validating drag permission for player: " + player.getName());
                closeMenu(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        if (menu.isMenuInventory(event.getInventory())) {
            if (menu.shouldReopenOnClose()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        BaseMenu current = openMenus.get(player.getUniqueId());
                        if (current == menu) {
                            try {
                                menu.open();
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.WARNING, "Error reopening menu", e);
                                closeMenu(player);
                            }
                        }
                    }
                });
            } else {
                openMenus.remove(player.getUniqueId());
                try {
                    menu.onClose();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error on menu close", e);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BaseMenu menu = openMenus.remove(event.getPlayer().getUniqueId());
        if (menu != null) {
            try {
                menu.onClose();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing menu on quit", e);
            }
        }
    }

    public void shutdown() {
        for (Map.Entry<UUID, BaseMenu> entry : openMenus.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.closeInventory();
            }
            entry.getValue().onClose();
        }
        openMenus.clear();
        menuFactories.clear();
    }

    @FunctionalInterface
    public interface MenuFactory {
        BaseMenu create(MythicRod plugin, Player player);
    }
}
