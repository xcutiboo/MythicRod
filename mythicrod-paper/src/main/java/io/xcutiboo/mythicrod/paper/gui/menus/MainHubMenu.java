package io.xcutiboo.mythicrod.paper.gui.menus;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.constants.PermissionNodes;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;

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
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        Player p = getPlayer();
        boolean hasAdminPerm = p != null && p.hasPermission(PermissionNodes.ADMIN_CONFIG);

        String serverVersion = plugin.getServer().getName() + " " + plugin.getServer().getMinecraftVersion();

        ItemStack dropsItem = new ItemBuilder(Material.CHEST)
                .name(tr("gui.main.drops.name"))
                .lore(
                        tr("gui.main.drops.lore1"),
                        tr("gui.main.drops.lore2"),
                        "",
                        tr("gui.main.drops.lore6", Map.of("count", String.valueOf(plugin.getDropManager().getTotalDropCount()))),
                        tr("gui.main.drops.lore7", Map.of("categories", String.valueOf(plugin.getDropManager().getDropCategories().size()))),
                        tr("gui.main.drops.lore8"),
                        tr("gui.main.drops.lore9")
                )
                .glow(true)
                .build();
        setNavigationItem(22, dropsItem, "drops");

        ItemStack rodItem = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.main.rod.name"))
                .lore(
                        tr("gui.main.rod.lore1"),
                        tr("gui.main.rod.lore2"),
                        "",
                        tr("gui.main.rod.lore5")
                )
                .build();
        setNavigationItem(20, rodItem, "rod");

        boolean statsEnabled = plugin.getConfigManager().trackStatistics();
        ItemStack statsItem = new ItemBuilder(statsEnabled ? Material.FILLED_MAP : Material.MAP)
                .name(tr("gui.main.stats.name"))
                .lore(
                        tr("gui.main.stats.lore1"),
                        tr("gui.main.stats.lore2"),
                        "",
                        statsEnabled ? tr("gui.main.stats.enabled") : tr("gui.main.stats.disabled"),
                        "",
                        statsEnabled ? tr("gui.main.stats.click_view") : tr("gui.main.stats.enable_first"),
                        tr("gui.main.stats.lore9")
                )
                .glow(statsEnabled)
                .build();
        setItem(24, statsItem, () -> {
            playClickSound();
            if (!statsEnabled) {
                playErrorSound();
                sendMessage(tr("gui.main.stats_disabled"));
                return;
            }
            plugin.getGUIManager().openMenu(getPlayer(), "stats");
        });

        if (hasAdminPerm) {
            ItemStack configItem = new ItemBuilder(Material.COMPARATOR)
                    .name(tr("gui.main.config.name"))
                    .lore(
                            tr("gui.main.config.lore1"),
                            tr("gui.main.config.lore2"),
                            "",
                            tr("gui.main.config.lore8"),
                            tr("gui.main.config.lore9")
                    )
                    .glow(true)
                    .build();
            setNavigationItem(29, configItem, "config");
        }

        ItemStack infoItem = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(tr("gui.main.info.name"))
                .lore(
                        tr("gui.main.info.lore1"),
                        "",
                        tr("gui.main.info.lore3", Map.of("version", plugin.getPluginMeta().getVersion())),
                        tr("gui.main.info.lore4", Map.of("server", serverVersion)),
                        tr("gui.main.info.lore5"),
                        tr("gui.main.info.lore6")
                )
                .build();
        setItem(31, infoItem);

        if (p != null && p.hasPermission(PermissionNodes.ADMIN_RELOAD)) {
            ItemStack reloadItem = new ItemBuilder(Material.RECOVERY_COMPASS)
                    .name(tr("gui.main.reload.name"))
                    .lore(
                            tr("gui.main.reload.lore1"),
                            tr("gui.main.reload.lore2"),
                            tr("gui.main.reload.lore3"),
                            tr("gui.main.reload.lore4"),
                            tr("gui.main.reload.lore5"),
                            tr("gui.main.reload.lore6"),
                            tr("gui.main.reload.lore7")
                    )
                    .build();
            setItem(33, reloadItem, event -> {
                if (!event.isShiftClick()) {
                    playErrorSound();
                    sendMessage(tr("gui.main.reload_confirm"));
                    return;
                }

                playClickSound();
                if (getPlayer() != null) getPlayer().closeInventory();
                try {
                    if (!plugin.reload()) {
                        playErrorSound();
                        if (plugin.isReloadInProgress()) {
                            sendMessage(tr("gui.main.reload_busy"));
                            return;
                        }
                        sendMessage(tr("gui.main.reload_failed"));
                        return;
                    }
                    playSuccessSound();
                    sendMessage(tr("gui.main.reload_success"));
                } catch (Exception e) {
                    playErrorSound();
                    sendMessage(tr("gui.main.reload_failed"));
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error reloading from GUI", e);
                }
            });
        }

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.main.close.name"))
                .lore(tr("gui.main.close.lore"))
                .build();
        setCloseButton(49, closeItem);

        ItemStack helpItem = new ItemBuilder(Material.BOOK)
                .name(tr("gui.main.help.name"))
                .lore(
                        tr("gui.main.help.lore1"),
                        tr("gui.main.help.lore2"),
                        tr("gui.main.help.lore3"),
                        tr("gui.main.help.lore4"),
                        tr("gui.main.help.lore5"),
                        tr("gui.main.help.lore6"),
                        tr("gui.main.help.lore7"),
                        tr("gui.main.help.lore8")
                )
                .build();
        setItem(47, helpItem);
    }

}
