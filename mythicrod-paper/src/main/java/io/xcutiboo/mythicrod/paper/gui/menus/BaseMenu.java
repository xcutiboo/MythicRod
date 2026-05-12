package io.xcutiboo.mythicrod.paper.gui.menus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.paper.gui.MythicRodMenuHolder;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.text.ConfiguredText;
import net.kyori.adventure.text.Component;

public abstract class BaseMenu {
    protected final MythicRod plugin;
    protected final UUID playerUuid;
    protected final long guiSessionVersion;
    protected Inventory inventory;
    protected Map<String, Object> context = new HashMap<>();
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
    protected boolean shouldReopen = false;
    private boolean closeFeedbackSuppressed = false;

    protected BaseMenu(MythicRod plugin, Player player) {
        this.plugin = plugin;
        this.playerUuid = player.getUniqueId();
        this.guiSessionVersion = plugin.getGUIManager() != null
            ? plugin.getGUIManager().getCurrentSessionVersion()
            : 0L;
    }

    protected abstract int getSize();
    protected abstract String getTitle();
    protected abstract void build();

    public long getGuiSessionVersion() {
        return guiSessionVersion;
    }

    public Inventory getInventory() {
        return inventory;
    }

    protected Player getPlayer() {
        return plugin.getServer().getPlayer(playerUuid);
    }

    protected boolean validatePermission() {
        String permission = getRequiredPermission();
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        Player player = getPlayer();
        return player != null && player.hasPermission(permission);
    }

    protected boolean requirePermission() {
        if (validatePermission()) {
            return true;
        }
        sendMessage(tr("general.no_permission"));
        playErrorSound();
        return false;
    }

    public void open() {
        Player player = getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        String permission = getRequiredPermission();
        if (permission != null && !permission.isEmpty()) {
            if (!player.hasPermission(permission)) {
                sendMessage(tr("general.no_permission"));
                playErrorSound();
                return;
            }
        }

        try {
            Component titleComponent = ConfiguredText.parse(getTitle());
            inventory = Bukkit.createInventory(
                new MythicRodMenuHolder(this),
                getSize(),
                titleComponent
            );
            build();
            player.openInventory(inventory);
            playOpenSound();
            playOpenEffect();
        } catch (RuntimeException e) {
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

    protected void setItem(int slot, ItemStack item, Runnable action) {
        setItem(slot, item, action == null ? null : new RunnableClickHandler(action));
    }

    private static final class RunnableClickHandler implements Consumer<InventoryClickEvent> {
        private final Runnable action;

        private RunnableClickHandler(Runnable action) {
            this.action = action;
        }

        @Override
        public void accept(InventoryClickEvent event) {
            action.run();
        }
    }

    protected void playClickSound() {
        playSound(Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    protected void playOpenSound() {
        playSound(Sound.BLOCK_CHEST_OPEN, 0.6f, 1.0f);
    }

    protected void playCloseSound() {
        playSound(Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
    }

    protected void playSuccessSound() {
        playSound(Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
        playSuccessEffect();
    }

    protected void playErrorSound() {
        playSound(Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
    }

    private void playSound(Sound sound, float volume, float pitch) {
        if (!plugin.getConfigManager().useSounds()) {
            return;
        }

        Player player = getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        plugin.getPlatformScheduler().runForPlayer(new PaperPlayer(player), () -> {
            if (player.isOnline()) {
                player.playSound(player, sound, SoundCategory.MASTER, volume, pitch);
            }
        });
    }

    protected boolean shouldShowVisualEffects() {
        Player player = getPlayer();
        return plugin.getConfigManager().useParticles()
            && player != null
            && player.isOnline()
            && (plugin.getPlayerDataService() == null
                || !plugin.getPlayerDataService().hasReducedEffects(player));
    }

    private void playOpenEffect() {
        playPlayerParticleEffect(Particle.END_ROD, 5, 0.22D, 0.26D, 0.01D);
    }

    private void playSuccessEffect() {
        playPlayerParticleEffect(Particle.HAPPY_VILLAGER, 7, 0.24D, 0.28D, 0.02D);
    }

    private void playPlayerParticleEffect(Particle particle, int count, double horizontalOffset, double verticalOffset, double extra) {
        if (!plugin.getConfigManager().useParticles()) {
            return;
        }

        Player player = getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        plugin.getPlatformScheduler().runForPlayer(new PaperPlayer(player), () -> {
            if (!player.isOnline() || !shouldShowVisualEffects()) {
                return;
            }
            player.spawnParticle(
                particle,
                player.getLocation().add(0.0D, 1.15D, 0.0D),
                count,
                horizontalOffset,
                verticalOffset,
                horizontalOffset,
                extra
            );
        });
    }

    protected void setItem(int slot, ItemStack item) {
        setItem(slot, item, (Consumer<InventoryClickEvent>) null);
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

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem);
        }
        for (int i = size - 9; i < size; i++) {
            inventory.setItem(i, borderItem);
        }
        for (int row = 0; row < rows; row++) {
            inventory.setItem(row * 9, borderItem);
            inventory.setItem(row * 9 + 8, borderItem);
        }
    }

    protected void setConfigurableItem(int slot, ItemStack item, Runnable onClick) {
        setItem(slot, item, () -> {
            if (!requirePermission()) {
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
        setItem(slot, item, () -> {
            playClickSound();
            plugin.getGUIManager().openMenu(getPlayer(), menuId);
        });
    }

    protected void setNavigationItem(int slot, ItemStack item, String menuId, Map<String, Object> context) {
        setItem(slot, item, () -> {
            playClickSound();
            plugin.getGUIManager().openMenu(getPlayer(), menuId, context);
        });
    }

    protected void setCloseButton(int slot, ItemStack item) {
        setItem(slot, item, () -> {
            playClickSound();
            getPlayer().closeInventory();
        });
    }

    protected void setActionItem(int slot, ItemStack item, Runnable action) {
        setItem(slot, item, () -> {
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

    public void suppressReopen() {
        this.shouldReopen = false;
    }

    public void suppressCloseFeedback() {
        this.closeFeedbackSuppressed = true;
    }

    public void onClose() {
        if (closeFeedbackSuppressed) {
            closeFeedbackSuppressed = false;
            return;
        }
        playCloseSound();
    }

    public String getRequiredPermission() {
        return null;
    }

    protected void sendMessage(String message) {
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            Component component = ConfiguredText.parse(message);
            player.sendMessage(component);
        }
    }

    protected String tr(String key) {
        return plugin.getLanguageManager().trForPlayer(playerUuid, key);
    }

    protected String tr(String key, Map<String, String> placeholders) {
        return plugin.getLanguageManager().trForPlayer(playerUuid, key, placeholders);
    }

    protected Component trComponent(String key) {
        String text = plugin.getLanguageManager().trForPlayer(playerUuid, key);
        return ConfiguredText.parse(text);
    }
}
