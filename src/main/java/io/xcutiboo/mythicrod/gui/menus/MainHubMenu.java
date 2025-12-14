package io.xcutiboo.mythicrod.gui.menus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.gui.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
public class MainHubMenu extends BaseMenu {
    public MainHubMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }
    @Override
    protected int getSize() {
        return 54; // 6 rows
    }
    @Override
    protected String getTitle() {
        return "&6&lMythicRod &8⚡ &7Main Menu";
    }
    @Override
    protected void build() {
        // Border decoration
        fillBorder();
        // Configuration button
        ItemStack configItem = new ItemBuilder(Material.COMPARATOR)
                .name("&e&lConfiguration Settings")
                .lore(
                        "&7Manage plugin settings and features",
                        "",
                        "&8• Visual effects",
                        "&8• Performance settings",
                        "&8• Feature toggles",
                        "&8• Debug options",
                        "",
                        "&eClick to configure"
                )
                .glow(true)
                .build();
        setItem(20, configItem, event -> {
            plugin.getGUIManager().openMenu(player, "config");
        });
        // Drops management button
        ItemStack dropsItem = new ItemBuilder(Material.FISHING_ROD)
                .name("&b&lDrop Management")
                .lore(
                        "&7View and manage fishing drops",
                        "",
                        "&8• Browse drop categories",
                        "&8• View drop rates",
                        "&8• Check biome restrictions",
                        "",
                        "&7Total Drops: &f" + plugin.getDropManager().getTotalDropCount(),
                        "&7Categories: &f" + plugin.getDropManager().getDropCategories().size(),
                        "",
                        "&eClick to manage drops"
                )
                .build();
        setItem(22, dropsItem, event -> {
            plugin.getGUIManager().openMenu(player, "drops");
        });
        // Statistics button
        ItemStack statsItem = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&d&lStatistics & Leaderboards")
                .lore(
                        "&7View player fishing statistics",
                        "",
                        "&8• Your fishing stats",
                        "&8• Top fishers leaderboard",
                        "&8• Material breakdowns",
                        "",
                        "&7Tracking: &f" + (plugin.getConfigManager().trackStatistics() ? "&aEnabled" : "&cDisabled"),
                        "",
                        "&eClick to view stats"
                )
                .build();
        setItem(24, statsItem, event -> {
            if (!plugin.getConfigManager().trackStatistics()) {
                sendMessage("&cStatistics tracking is currently disabled!");
                return;
            }
            plugin.getGUIManager().openMenu(player, "stats");
        });
        // Plugin info button
        ItemStack infoItem = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name("&6&lPlugin Information")
                .lore(
                        "&7About MythicRod",
                        "",
                        "&7Version: &f" + plugin.getPluginMeta().getVersion(),
                        "&7Author: &fxcutiboo",
                        "",
                        "&7A feature-rich fishing plugin",
                        "&7for Paper servers with custom",
                        "&7drops, biome restrictions, and",
                        "&7comprehensive statistics.",
                        "",
                        "&8Running on Paper API"
                )
                .build();
        setItem(40, infoItem);
        // Reload button (admin only)
        if (player.hasPermission("mythicrod.admin.reload")) {
            ItemStack reloadItem = new ItemBuilder(Material.RECOVERY_COMPASS)
                    .name("&a&lReload Configuration")
                    .lore(
                            "&7Reload all plugin configurations",
                            "",
                            "&cWarning: &7This will reload:",
                            "&8• config.yml",
                            "&8• drops.yml",
                            "&8• messages.yml",
                            "",
                            "&eClick to reload"
                    )
                    .build();
            setItem(49, reloadItem, event -> {
                player.closeInventory();
                try {
                    plugin.reload();
                    sendMessage("&aConfiguration reloaded successfully!");
                } catch (Exception e) {
                    sendMessage("&cFailed to reload configuration! Check console for errors.");
                    plugin.getLogger().severe("Error reloading from GUI: " + e.getMessage());
                }
            });
        }
        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("&c&lClose Menu")
                .lore("&7Click to close")
                .build();
        setItem(45, closeItem, event -> player.closeInventory());
        // Help button
        ItemStack helpItem = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name("&b&lHelp & Commands")
                .lore(
                        "&7Available commands:",
                        "",
                        "&e/mythicrod &7- Open this menu",
                        "&e/mythicrod reload &7- Reload config",
                        "&e/mythicrod stats [player] &7- View stats",
                        "&e/mythicrod top [limit] &7- Leaderboard",
                        "&e/mythicrod drops &7- List drops",
                        "",
                        "&7Use the GUI for easier access!"
                )
                .build();
        setItem(53, helpItem);
    }
    private void fillBorder() {
        ItemStack borderItem = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            setItem(i, borderItem);
            setItem(45 + i, borderItem);
        }
        // Left and right columns
        for (int row = 1; row < 5; row++) {
            setItem(row * 9, borderItem);
            setItem(row * 9 + 8, borderItem);
        }
    }
    private void sendMessage(String message) {
        Component component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getConfigManager().getPrefix() + message);
        player.sendMessage(component);
    }
}
