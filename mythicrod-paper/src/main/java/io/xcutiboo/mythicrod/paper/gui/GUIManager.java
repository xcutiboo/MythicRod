package io.xcutiboo.mythicrod.paper.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
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

import io.papermc.paper.event.player.AsyncChatEvent;
import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.paper.gui.menus.BaseMenu;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.text.ConfiguredText;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class GUIManager implements Listener {
    private static final long TEXT_INPUT_TIMEOUT_TICKS = 20L * 60L;
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final MythicRod plugin;
    private final PlatformScheduler scheduler;
    private final Map<String, MenuFactory> menuFactories = new HashMap<>();
    private final Map<UUID, TextInputSession> pendingTextInputs = new ConcurrentHashMap<>();
    private final AtomicLong menuSessionVersion = new AtomicLong();

    public GUIManager(MythicRod plugin, PlatformScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("GUI manager initialized");
    }

    public void registerMenu(String menuId, MenuFactory factory) {
        menuFactories.put(menuId.toLowerCase(Locale.ROOT), factory);
    }

    public long getCurrentSessionVersion() {
        return menuSessionVersion.get();
    }

    public void invalidateOpenMenusForReload() {
        menuSessionVersion.incrementAndGet();
        for (Map.Entry<UUID, TextInputSession> entry : List.copyOf(pendingTextInputs.entrySet())) {
            cancelPendingTextInput(null, entry.getKey(), entry.getValue(), false);
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            scheduler.runForPlayer(new PaperPlayer(player), () -> closeStaleMenuIfPresent(player, true));
        }
    }

    public boolean openMenu(Player player, String menuId) {
        return openMenu(player, menuId, null);
    }

    public boolean openMenu(Player player, String menuId, Map<String, Object> context) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        if (plugin.isReloadInProgress()) {
            sendSystemMessage(player, "gui.system.reload_in_progress");
            return false;
        }

        MenuFactory factory = menuFactories.get(menuId.toLowerCase(Locale.ROOT));
        if (factory == null) {
            plugin.getLogger().log(Level.WARNING, "Unknown menu: {0}", menuId);
            return false;
        }

        try {
            BaseMenu menu = factory.create(plugin, player);
            if (menu == null) {
                return false;
            }
            if (context != null) {
                menu.setContext(context);
            }

            cancelPendingTextInput(player, null, null, true);
            suppressCloseFeedbackForCurrentMenu(player);
            player.closeInventory();
            menu.open();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Failed to open menu: " + menuId);
            return false;
        }
    }

    public void openMainHub(Player player) {
        openMenu(player, "main");
    }

    public void closeMenu(Player player) {
        if (player == null) return;
        player.closeInventory();
    }

    /**
     * Captures the player's next chat message for an inventory editor.
     *
     * <p>The chat event may be async, so the accepted value is delivered back on
     * the player's owning scheduler before any menu code runs.
     */
    public boolean requestTextInput(
        Player player,
        String prompt,
        String cancelMessage,
        String expiredMessage,
        Consumer<String> valueHandler,
        Runnable cancelHandler
    ) {
        if (player == null || !player.isOnline() || valueHandler == null) {
            return false;
        }

        if (plugin.isReloadInProgress()) {
            sendSystemMessage(player, "gui.system.reload_in_progress");
            return false;
        }

        UUID playerId = player.getUniqueId();
        TextInputSession session = new TextInputSession(
            valueHandler,
            cancelHandler,
            cancelMessage,
            expiredMessage,
            System.currentTimeMillis() + (TEXT_INPUT_TIMEOUT_TICKS * 50L)
        );
        pendingTextInputs.put(playerId, session);

        suppressCloseFeedbackForCurrentMenu(player);
        player.closeInventory();
        sendRawMessage(player, prompt);

        scheduler.runForPlayerDelayed(new PaperPlayer(player), () -> expireTextInput(player, playerId, session),
            TEXT_INPUT_TIMEOUT_TICKS);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player player) || !player.isOnline()) {
                return;
            }

            Inventory topInventory = event.getView().getTopInventory();
            if (!(topInventory.getHolder() instanceof MythicRodMenuHolder holder)) {
                return;
            }

            BaseMenu menu = holder.getMenu();
            if (menu == null) {
                event.setCancelled(true);
                return;
            }

            if (isStaleMenu(menu)) {
                event.setCancelled(true);
                closeStaleMenuIfPresent(player, true);
                return;
            }

            switch (event.getClick()) {
                case NUMBER_KEY, SWAP_OFFHAND, DROP, CONTROL_DROP -> {
                    event.setCancelled(true);
                    return;
                }
                default -> {
                }
            }

            Inventory clicked = event.getClickedInventory();
            if (clicked == null) {
                event.setCancelled(true);
                return;
            }

            if (!menu.isMenuInventory(clicked)) {
                if (isUnsafePlayerInventoryClick(event)) {
                    event.setCancelled(true);
                }
                return;
            }

            event.setCancelled(true);
            menu.handleClick(event);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling menu interaction. Player action may not have been processed.", e);
            if (event.getWhoClicked() instanceof Player player && player.isOnline()) {
                try {
                    player.closeInventory();
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Error closing inventory after click handler exception", ex);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        TextInputSession session = pendingTextInputs.remove(playerId);
        if (session == null) {
            return;
        }

        event.setCancelled(true);
        String input = PLAIN_TEXT.serialize(event.message()).trim();
        scheduler.runForPlayer(new PaperPlayer(player), () -> handleTextInput(player, input, session));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        try {
            Inventory topInventory = event.getView().getTopInventory();
            if (!(topInventory.getHolder() instanceof MythicRodMenuHolder holder)) {
                return;
            }

            BaseMenu menu = holder.getMenu();
            if (menu != null && isStaleMenu(menu) && event.getWhoClicked() instanceof Player player) {
                event.setCancelled(true);
                closeStaleMenuIfPresent(player, true);
                return;
            }

            if (event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling inventory drag", e);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        try {
            if (!(event.getPlayer() instanceof Player player) || !player.isOnline()) {
                return;
            }

            Inventory topInventory = event.getView().getTopInventory();
            if (!(topInventory.getHolder() instanceof MythicRodMenuHolder holder)) {
                return;
            }

            BaseMenu menu = holder.getMenu();
            if (menu == null) {
                return;
            }

            if (menu.shouldReopenOnClose() && !isStaleMenu(menu) && !plugin.isReloadInProgress()) {
                scheduler.runForPlayer(new PaperPlayer(player), () -> {
                    if (player.isOnline() && !isStaleMenu(menu) && !plugin.isReloadInProgress()) {
                        try {
                            menu.open();
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Error reopening menu", e);
                        }
                    }
                });
            } else {
                menu.onClose();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling inventory close", e);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            pendingTextInputs.remove(player.getUniqueId());

            if (player.getOpenInventory() != null) {
                Inventory topInventory = player.getOpenInventory().getTopInventory();
                if (topInventory.getHolder() instanceof MythicRodMenuHolder holder) {
                    BaseMenu menu = holder.getMenu();
                    if (menu != null) {
                        menu.onClose();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling player quit", e);
        }
    }

    public void shutdown() {
        try {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                closePlayerMenuQuietly(player);
            }
            pendingTextInputs.clear();
            menuFactories.clear();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during GUI shutdown", e);
        }
    }

    private void closePlayerMenuQuietly(Player player) {
        if (player == null || player.getOpenInventory() == null) return;
        try {
            Inventory topInventory = player.getOpenInventory().getTopInventory();
            if (topInventory.getHolder() instanceof MythicRodMenuHolder holder) {
                player.closeInventory();
                BaseMenu menu = holder.getMenu();
                if (menu != null) {
                    menu.onClose();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing player menu during shutdown", e);
        }
    }

    private boolean isUnsafePlayerInventoryClick(InventoryClickEvent event) {
        if (event.isShiftClick()) {
            return true;
        }

        return switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY,
                 HOTBAR_SWAP,
                 COLLECT_TO_CURSOR,
                 UNKNOWN -> true;
            default -> false;
        };
    }

    private void handleTextInput(Player player, String input, TextInputSession session) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (System.currentTimeMillis() > session.expiresAtMillis()) {
            sendRawMessage(player, session.expiredMessage());
            return;
        }

        if (isCancelInput(input)) {
            sendRawMessage(player, session.cancelMessage());
            if (session.cancelHandler() != null) {
                session.cancelHandler().run();
            }
            return;
        }

        try {
            session.valueHandler().accept(input);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, e,
                () -> "Failed to apply GUI text input for " + player.getName());
            sendSystemMessage(player, "gui.system.input_failed");
            if (session.cancelHandler() != null) {
                try {
                    session.cancelHandler().run();
                } catch (Exception reopenException) {
                    plugin.getLogger().log(Level.WARNING, reopenException,
                        () -> "Failed to reopen GUI after text input error for " + player.getName());
                }
            }
        }
    }

    private void expireTextInput(Player player, UUID playerId, TextInputSession session) {
        if (player == null || !player.isOnline()) {
            pendingTextInputs.remove(playerId, session);
            return;
        }

        if (!pendingTextInputs.remove(playerId, session)) {
            return;
        }

        sendRawMessage(player, session.expiredMessage());
    }

    private void cancelPendingTextInput(Player player, UUID playerId, TextInputSession session, boolean notifyWithCancelMessage) {
        UUID resolvedPlayerId = playerId != null ? playerId : player != null ? player.getUniqueId() : null;
        if (resolvedPlayerId == null) {
            return;
        }

        TextInputSession activeSession = session != null ? session : pendingTextInputs.get(resolvedPlayerId);
        if (activeSession == null || !pendingTextInputs.remove(resolvedPlayerId, activeSession)) {
            return;
        }

        if (player == null || !player.isOnline()) {
            player = plugin.getServer().getPlayer(resolvedPlayerId);
        }
        if (player == null || !player.isOnline()) {
            return;
        }

        if (notifyWithCancelMessage) {
            sendRawMessage(player, activeSession.cancelMessage());
        } else {
            sendSystemMessage(player, "gui.system.closed_for_reload");
        }
    }

    private boolean isCancelInput(String input) {
        if (input == null) {
            return false;
        }

        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("cancel")
            || normalized.equals("exit")
            || normalized.equals("back");
    }

    private boolean isStaleMenu(BaseMenu menu) {
        return menu.getGuiSessionVersion() < menuSessionVersion.get();
    }

    private void suppressCloseFeedbackForCurrentMenu(Player player) {
        try {
            Inventory topInventory = player.getOpenInventory().getTopInventory();
            if (topInventory.getHolder() instanceof MythicRodMenuHolder holder) {
                BaseMenu currentMenu = holder.getMenu();
                if (currentMenu != null) {
                    currentMenu.suppressCloseFeedback();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Unable to suppress GUI transition close feedback", e);
        }
    }

    private void closeStaleMenuIfPresent(Player player, boolean notifyPlayer) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (!(topInventory.getHolder() instanceof MythicRodMenuHolder holder)) {
            return;
        }

        BaseMenu menu = holder.getMenu();
        if (menu == null || !isStaleMenu(menu)) {
            return;
        }

        try {
            menu.suppressReopen();
            player.closeInventory();
            if (notifyPlayer) {
                sendSystemMessage(player, "gui.system.closed_for_reload");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing stale menu during reload", e);
        }
    }

    private void sendSystemMessage(Player player, String key) {
        if (player == null || !player.isOnline() || plugin.getLanguageManager() == null) {
            return;
        }

        String message = plugin.getLanguageManager().trForPlayer(player.getUniqueId(), key);
        sendRawMessage(player, message);
    }

    private void sendRawMessage(Player player, String message) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (message == null || message.isBlank()) {
            return;
        }

        player.sendMessage(ConfiguredText.parse(message));
    }

    @FunctionalInterface
    public interface MenuFactory {
        BaseMenu create(MythicRod plugin, Player player);
    }

    private record TextInputSession(
        Consumer<String> valueHandler,
        Runnable cancelHandler,
        String cancelMessage,
        String expiredMessage,
        long expiresAtMillis
    ) {
    }
}
