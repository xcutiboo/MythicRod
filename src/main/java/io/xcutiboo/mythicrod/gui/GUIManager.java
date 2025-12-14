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
import io.xcutiboo.mythicrod.gui.menus.ConfigMenu;
import io.xcutiboo.mythicrod.gui.menus.DropsMenu;
import io.xcutiboo.mythicrod.gui.menus.LanguageSwitchMenu;
import io.xcutiboo.mythicrod.gui.menus.MainHubMenu;
import io.xcutiboo.mythicrod.gui.menus.StatsMenu;
public class GUIManager implements Listener {
    private final MythicRod plugin;
    private final Map<UUID, BaseMenu> openMenus = new HashMap<>();
    private final Map<String, MenuFactory> menuFactories = new HashMap<>();
    public GUIManager(MythicRod plugin) {
        this.plugin = plugin;
    }
    public void initialize() {
        // Register all menu types
        registerMenu("main", MainHubMenu::new);
        registerMenu("config", ConfigMenu::new);
        registerMenu("drops", DropsMenu::new);
        registerMenu("stats", StatsMenu::new);
        registerMenu("language", LanguageSwitchMenu::new);
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("GUI Manager initialized with " + menuFactories.size() + " menu types");
    }
    public void registerMenu(String menuId, MenuFactory factory) {
        menuFactories.put(menuId.toLowerCase(), factory);
        plugin.getLogger().fine("Registered menu: " + menuId);
    }
    public boolean openMenu(Player player, String menuId) {
        return openMenu(player, menuId, null);
    }
    public boolean openMenu(Player player, String menuId, Map<String, Object> context) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Attempted to open menu for null or offline player");
            return false;
        }
        MenuFactory factory = menuFactories.get(menuId.toLowerCase());
        if (factory == null) {
            plugin.getLogger().warning("Attempted to open unregistered menu: " + menuId);
            return false;
        }
        try {
            // Close any existing menu
            closeMenu(player);
            // Create and open new menu
            BaseMenu menu = factory.create(plugin, player);
            if (menu == null) {
                plugin.getLogger().warning("Menu factory returned null for menu: " + menuId);
                return false;
            }
            if (context != null) {
                menu.setContext(context);
            }
            menu.open();
            openMenus.put(player.getUniqueId(), menu);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to open menu '" + menuId + "' for " + player.getName(), e);
            openMenus.remove(player.getUniqueId());
            return false;
        }
    }
    public void openMainHub(Player player) {
        openMenu(player, "main");
    }
    public void closeMenu(Player player) {
        if (player == null) {
            return;
        }
        BaseMenu menu = openMenus.remove(player.getUniqueId());
        if (menu != null) {
            try {
                menu.onClose();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing menu for " + player.getName(), e);
            }
        }
    }
    public BaseMenu getOpenMenu(Player player) {
        if (player == null) {
            return null;
        }
        return openMenus.get(player.getUniqueId());
    }
    public boolean hasMenuOpen(Player player) {
        if (player == null) {
            return false;
        }
        return openMenus.containsKey(player.getUniqueId());
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !player.isOnline()) {
            return;
        }
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        // Check if this is our menu's inventory
        if (!menu.isMenuInventory(clickedInventory)) {
            return;
        }
        // Cancel the event by default for menu interactions
        event.setCancelled(true);
        // Delegate to the menu's click handler
        try {
            menu.handleClick(event);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling menu click for " + player.getName(), e);
            closeMenu(player);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !player.isOnline()) {
            return;
        }
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }
        // Cancel any drag attempts in menu inventories
        if (event.getRawSlots().stream().anyMatch(slot ->
                slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !player.isOnline()) {
            return;
        }
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }
        // Check if this is our menu being closed
        if (menu.isMenuInventory(event.getInventory())) {
            // Schedule reopening if menu requests it
            if (menu.shouldReopenOnClose()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        BaseMenu currentMenu = openMenus.get(player.getUniqueId());
                        if (currentMenu == menu) {
                            try {
                                menu.open();
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.WARNING, "Error reopening menu for " + player.getName(), e);
                                closeMenu(player);
                            }
                        }
                    }
                });
            } else {
                // Clean up the menu session
                openMenus.remove(player.getUniqueId());
                try {
                    menu.onClose();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error on menu close for " + player.getName(), e);
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
                plugin.getLogger().log(Level.WARNING, "Error closing menu on player quit", e);
            }
        }
    }
    public void shutdown() {
        // Close all open menus
        for (Map.Entry<UUID, BaseMenu> entry : openMenus.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.closeInventory();
            }
            entry.getValue().onClose();
        }
        openMenus.clear();
        menuFactories.clear();
        plugin.getLogger().info("GUI Manager shut down successfully");
    }
    @FunctionalInterface
    public interface MenuFactory {
        BaseMenu create(MythicRod plugin, Player player);
    }
}
