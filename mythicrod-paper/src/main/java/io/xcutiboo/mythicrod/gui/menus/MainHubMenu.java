package io.xcutiboo.mythicrod.gui.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.gui.utils.ItemBuilder;

public class MainHubMenu extends BaseMenu {

    public MainHubMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int getSize() {
        return 54;
    }

    @Override
    protected String getTitle() {
        return "&6&lMythicRod &8⚡ &7Main Menu";
    }

    @Override
    protected void build() {
        fillBorder();
        
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
            plugin.getGUIManager().openMenu(getPlayer(), "config");
        });

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
            plugin.getGUIManager().openMenu(getPlayer(), "drops");
        });

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
            plugin.getGUIManager().openMenu(getPlayer(), "stats");
        });

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

        Player p = getPlayer();
        if (p != null && p.hasPermission("mythicrod.admin.reload")) {
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
                if (getPlayer() != null) getPlayer().closeInventory();
                try {
                    plugin.reload();
                    sendMessage("&aConfiguration reloaded successfully!");
                } catch (Exception e) {
                    sendMessage("&cFailed to reload configuration! Check console for errors.");
                    plugin.getLogger().severe("Error reloading from GUI: " + e.getMessage());
                }
            });
        }

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("&c&lClose Menu")
                .lore("&7Click to close")
                .build();
        setItem(45, closeItem, event -> { if (getPlayer() != null) getPlayer().closeInventory(); });

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
        for (int i = 0; i < 9; i++) {
            setItem(i, borderItem);
            setItem(45 + i, borderItem);
        }
        for (int row = 1; row < 5; row++) {
            setItem(row * 9, borderItem);
            setItem(row * 9 + 8, borderItem);
        }
    }

}
