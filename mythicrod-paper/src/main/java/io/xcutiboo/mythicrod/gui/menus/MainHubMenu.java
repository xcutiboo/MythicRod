package io.xcutiboo.mythicrod.gui.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.item.ItemBuilder;

import java.util.Map;

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
        return tr("gui.main.title");
    }

    @Override
    protected void build() {
        fillBorder(Material.CYAN_STAINED_GLASS_PANE);
        
        Player p = getPlayer();
        boolean hasAdminPerm = p != null && p.hasPermission("mythicrod.admin.config");
        
        // Header decoration
        ItemStack decoration = new ItemBuilder(Material.PRISMARINE_SHARD)
                .name(tr("gui.main.decoration"))
                .build();
        for (int i = 1; i < 8; i++) {
            setItem(i, decoration, null);
        }
        
        // Row 3: Config (slot 20) - Only show if has admin permission
        if (hasAdminPerm) {
            ItemStack configItem = new ItemBuilder(Material.COMPARATOR)
                    .name(tr("gui.main.config.name"))
                    .lore(
                            tr("gui.main.config.lore1"),
                            "",
                            tr("gui.main.config.lore2"),
                            "",
                            tr("gui.main.config.lore3"),
                            tr("gui.main.config.lore4"),
                            tr("gui.main.config.lore5"),
                            tr("gui.main.config.lore6"),
                            "",
                            tr("gui.main.config.lore7"),
                            tr("gui.main.config.lore8")
                    )
                    .glow(true)
                    .build();
            setNavigationItem(20, configItem, "config");
        }

        // Row 3, Center: Drops (slot 22)
        ItemStack dropsItem = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.main.drops.name"))
                .lore(
                        tr("gui.main.drops.lore1"),
                        "",
                        tr("gui.main.drops.lore2"),
                        "",
                        tr("gui.main.drops.lore3"),
                        tr("gui.main.drops.lore4"),
                        tr("gui.main.drops.lore5"),
                        "",
                        tr("gui.main.drops.lore6", Map.of("%count%", String.valueOf(plugin.getDropManager().getTotalDropCount()))),
                        tr("gui.main.drops.lore7", Map.of("%categories%", String.valueOf(plugin.getDropManager().getDropCategories().size()))),
                        "",
                        tr("gui.main.drops.lore8"),
                        tr("gui.main.drops.lore9")
                )
                .build();
        setNavigationItem(22, dropsItem, "drops");

        // Row 3, Right: Stats (slot 24)
        boolean statsEnabled = plugin.getConfigManager().trackStatistics();
        ItemStack statsItem = new ItemBuilder(statsEnabled ? Material.ENCHANTED_BOOK : Material.WRITABLE_BOOK)
                .name(tr("gui.main.stats.name"))
                .lore(
                        tr("gui.main.stats.lore1"),
                        "",
                        tr("gui.main.stats.lore2"),
                        "",
                        tr("gui.main.stats.lore3"),
                        tr("gui.main.stats.lore4"),
                        tr("gui.main.stats.lore5"),
                        "",
                        statsEnabled ? tr("gui.main.stats.enabled") : tr("gui.main.stats.disabled"),
                        "",
                        statsEnabled ? tr("gui.main.stats.click_view") : tr("gui.main.stats.enable_first"),
                        tr("gui.main.stats.lore9")
                )
                .glow(statsEnabled)
                .build();
        setItem(24, statsItem, event -> {
            playClickSound();
            if (!statsEnabled) {
                playErrorSound();
                sendMessage(tr("gui.main.stats_disabled"));
                return;
            }
            plugin.getGUIManager().openMenu(getPlayer(), "stats");
        });

        // Row 5: Info (slot 40)
        ItemStack infoItem = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(tr("gui.main.info.name"))
                .lore(
                        tr("gui.main.info.lore1"),
                        "",
                        tr("gui.main.info.lore2", Map.of("%version%", plugin.getPluginMeta().getVersion())),
                        tr("gui.main.info.lore3"),
                        "",
                        tr("gui.main.info.lore4"),
                        tr("gui.main.info.lore5"),
                        tr("gui.main.info.lore6"),
                        tr("gui.main.info.lore7"),
                        "",
                        tr("gui.main.info.lore8")
                )
                .build();
        setItem(40, infoItem);

        // Admin Reload (slot 49)
        if (p != null && p.hasPermission("mythicrod.admin.reload")) {
            ItemStack reloadItem = new ItemBuilder(Material.RECOVERY_COMPASS)
                    .name(tr("gui.main.reload.name"))
                    .lore(
                            tr("gui.main.reload.lore1"),
                            "",
                            tr("gui.main.reload.lore2"),
                            tr("gui.main.reload.lore3"),
                            tr("gui.main.reload.lore4"),
                            tr("gui.main.reload.lore5"),
                            "",
                            tr("gui.main.reload.lore6")
                    )
                    .build();
            setItem(49, reloadItem, event -> {
                playClickSound();
                if (getPlayer() != null) getPlayer().closeInventory();
                try {
                    plugin.reload();
                    playSuccessSound();
                    sendMessage(tr("gui.main.reload_success"));
                } catch (Exception e) {
                    playErrorSound();
                    sendMessage(tr("gui.main.reload_failed"));
                    plugin.getLogger().severe("Error reloading from GUI: " + e.getMessage());
                }
            });
        }

        // Close Button (slot 45)
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.main.close.name"))
                .lore(tr("gui.main.close.lore"))
                .build();
        setCloseButton(45, closeItem);

        // Help (slot 53)
        ItemStack helpItem = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name(tr("gui.main.help.name"))
                .lore(
                        tr("gui.main.help.lore1"),
                        "",
                        tr("gui.main.help.lore2"),
                        tr("gui.main.help.lore3"),
                        tr("gui.main.help.lore4"),
                        tr("gui.main.help.lore5"),
                        tr("gui.main.help.lore6"),
                        "",
                        tr("gui.main.help.lore7")
                )
                .build();
        setItem(53, helpItem);
    }

    /**
     * DRY: Removed local fillBorder() - now using BaseMenu.fillBorder(Material).
     */
}
