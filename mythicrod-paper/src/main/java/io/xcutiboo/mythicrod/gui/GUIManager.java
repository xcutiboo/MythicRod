package io.xcutiboo.mythicrod.gui;

import java.util.HashMap;
import java.util.Map;
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
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.gui.menus.BaseMenu;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GUIManager implements Listener {
    private final MythicRod plugin;
    private final PlatformScheduler scheduler;
    private final Map<String, MenuFactory> menuFactories = new HashMap<>();

    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("GUI Manager initialized");
    }

    public void registerMenu(String menuId, MenuFactory factory) {
        menuFactories.put(menuId.toLowerCase(java.util.Locale.ROOT), factory);
    }

    public boolean openMenu(Player player, String menuId) {
        return openMenu(player, menuId, null);
    }

    public boolean openMenu(Player player, String menuId, Map<String, Object> context) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        MenuFactory factory = menuFactories.get(menuId.toLowerCase(java.util.Locale.ROOT));
        if (factory == null) {
            plugin.getLogger().log(Level.WARNING, "Unknown menu: " + menuId);
            return false;
        }

        try {
            // Close any existing inventory first
            player.closeInventory();
            
            BaseMenu menu = factory.create(plugin, player);
            if (menu == null) {
                return false;
            }
            if (context != null) {
                menu.setContext(context);
            }
            menu.open();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to open menu: " + menuId, e);
            return false;
        }
    }

    public void openMainHub(Player player) {
        openMenu(player, "main");
    }

    public void closeMenu(Player player) {
        if (player == null) return;
        // Simply close the inventory - InventoryHolder pattern handles the rest
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Use InventoryHolder pattern - no need for ConcurrentHashMap lookup
        if (!(event.getInventory().getHolder() instanceof MythicRodMenuHolder holder)) {
            return;
        }

        BaseMenu menu = holder.getMenu();
        if (menu == null) return;

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
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        try {
            menu.handleClick(event);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MythicRod-GUIManager] Error handling menu interaction. Player action may not have been processed.", e);
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        // Use InventoryHolder pattern
        if (!(event.getInventory().getHolder() instanceof MythicRodMenuHolder)) {
            return;
        }

        if (event.getRawSlots().stream().anyMatch(slot ->
                slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Use InventoryHolder pattern
        if (!(event.getInventory().getHolder() instanceof MythicRodMenuHolder holder)) {
            return;
        }

        BaseMenu menu = holder.getMenu();
        if (menu == null) return;

        if (menu.shouldReopenOnClose()) {
            scheduler.runForPlayer(new PaperPlayer(player), () -> {
                if (player.isOnline()) {
                    try {
                        menu.open();
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error reopening menu", e);
                    }
                }
            });
        } else {
            try {
                menu.onClose();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error on menu close", e);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Close inventory on quit - InventoryHolder pattern will handle cleanup
        if (player.getOpenInventory() != null && 
            player.getOpenInventory().getTopInventory().getHolder() instanceof MythicRodMenuHolder holder) {
            try {
                holder.getMenu().onClose();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing menu on quit", e);
            }
        }
    }

    public void shutdown() {
        // Close all open menus using InventoryHolder pattern
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory() != null && 
                player.getOpenInventory().getTopInventory().getHolder() instanceof MythicRodMenuHolder holder) {
                player.closeInventory();
                try {
                    holder.getMenu().onClose();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error during menu shutdown", e);
                }
            }
        }
        menuFactories.clear();
    }

    @FunctionalInterface
    public interface MenuFactory {
        BaseMenu create(MythicRod plugin, Player player);
    }
}
