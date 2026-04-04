package io.xcutiboo.mythicrod.gui.menus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.gui.MythicRodMenuHolder;
import io.xcutiboo.mythicrod.item.ItemBuilder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

@Getter
public abstract class BaseMenu {
    protected final MythicRod plugin;
    protected final UUID playerUuid;
    protected Inventory inventory;
    protected Map<String, Object> context = new HashMap<>();
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
    protected boolean shouldReopen = false;

    protected BaseMenu(MythicRod plugin, Player player) {
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

    public void open() {
        Player player = getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // Check permission before opening menu
        String permission = getRequiredPermission();
        if (permission != null && !permission.isEmpty()) {
            if (!player.hasPermission(permission)) {
                sendMessage("<red>You don't have permission to open this menu!</red>");
                playErrorSound();
                return;
            }
        }
        
        try {
            Component titleComponent = MiniMessage.miniMessage().deserialize(getTitle());
            inventory = Bukkit.createInventory(
                new MythicRodMenuHolder(this), 
                getSize(), 
                titleComponent
            );
            build();
            player.openInventory(inventory);
            playOpenSound();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error opening menu for " + player.getName(), e);
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

    protected void playClickSound() {
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
        }
    }

    protected void playOpenSound() {
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.6f, 1.0f);
        }
    }

    protected void playCloseSound() {
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.MASTER, 0.5f, 1.0f);
        }
    }

    protected void playSuccessSound() {
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 1.2f);
        }
    }

    protected void playErrorSound() {
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 0.5f, 1.0f);
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

    protected void fillBorder(Material material) {
        if (inventory == null || material == null) {
            return;
        }
        ItemStack borderItem = new ItemBuilder(material)
                .name(" ")
                .build();
        int size = inventory.getSize();
        int rows = size / 9;
        
        // Top row
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem);
        }
        // Bottom row
        for (int i = size - 9; i < size; i++) {
            inventory.setItem(i, borderItem);
        }
        // Left and right columns
        for (int row = 0; row < rows; row++) {
            inventory.setItem(row * 9, borderItem);
            inventory.setItem(row * 9 + 8, borderItem);
        }
    }

    protected void setConfigurableItem(int slot, ItemStack item, Runnable onClick) {
        setItem(slot, item, event -> {
            if (!validatePermission()) {
                playErrorSound();
                return;
            }
            playClickSound();
            onClick.run();
        });
    }

    protected void setConfigurableToggle(int slot, ItemStack item, Runnable onToggle) {
        setConfigurableItem(slot, item, () -> {
            onToggle.run();
            refresh();
        });
    }

    protected void setNavigationItem(int slot, ItemStack item, String menuId) {
        setItem(slot, item, event -> {
            playClickSound();
            plugin.getGUIManager().openMenu(getPlayer(), menuId);
        });
    }

    protected void setNavigationItem(int slot, ItemStack item, String menuId, Map<String, Object> context) {
        setItem(slot, item, event -> {
            playClickSound();
            plugin.getGUIManager().openMenu(getPlayer(), menuId, context);
        });
    }

    protected void setCloseButton(int slot, ItemStack item) {
        setItem(slot, item, event -> {
            playClickSound();
            getPlayer().closeInventory();
        });
    }

    protected void setActionItem(int slot, ItemStack item, Runnable action) {
        setItem(slot, item, event -> {
            playClickSound();
            action.run();
        });
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

    /**
     * Retrieves a typed value from the context map using type-safe Class token.
     * 
     * <p>This method uses a Class token to perform a safe cast, avoiding unchecked cast warnings.
     * The type parameter ensures compile-time type safety.
     *
     * @param key The context key
     * @param type The expected type class
     * @return The typed value, or null if not found or type mismatch
     */
    protected <T> T getContext(String key, Class<T> type) {
        Object value = context.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
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
        playCloseSound();
    }
    
    public String getRequiredPermission() {
        return null; // Override in subclasses if permission is required
    }
    
    protected void sendMessage(String message) {
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            Component component = MiniMessage.miniMessage().deserialize(message);
            player.sendMessage(component);
        }
    }
    
    /**
     * Translates a language key to a string using the plugin's LanguageManager.
     * 
     * @param key The translation key (e.g., "menu.stats.title")
     * @return The translated string, or the key itself if not found
     */
    protected String tr(String key) {
        return plugin.getLanguageManager().tr(key);
    }
    
    /**
     * Translates a language key with placeholders.
     * 
     * @param key The translation key
     * @param placeholders Map of placeholder keys to values (e.g., {"%count%", "5"})
     * @return The translated string with placeholders replaced
     */
    protected String tr(String key, Map<String, String> placeholders) {
        return plugin.getLanguageManager().tr(key, placeholders);
    }
    
    /**
     * Translates a language key and deserializes it as a MiniMessage Component.
     * Use this for text that may contain MiniMessage formatting tags.
     * 
     * @param key The translation key
     * @return The translated text as a Component
     */
    protected Component trComponent(String key) {
        String text = plugin.getLanguageManager().tr(key);
        return MiniMessage.miniMessage().deserialize(text);
    }
}
