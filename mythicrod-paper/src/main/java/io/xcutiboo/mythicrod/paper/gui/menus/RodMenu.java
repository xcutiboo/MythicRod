package io.xcutiboo.mythicrod.paper.gui.menus;

import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.constants.PermissionNodes;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;

public class RodMenu extends BaseMenu {

    public RodMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int getSize() {
        return 27;
    }

    @Override
    protected String getTitle() {
        return tr("gui.rod.title");
    }

    @Override
    protected void build() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);

        Player player = getPlayer();
        String currentTier = getCurrentRodTier(player);
        boolean globalEffectsEnabled = plugin.getConfigManager().useParticles();
        boolean reducedEffects = plugin.getPlayerDataService().hasReducedEffects(player);

        ItemStack basicRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.basic.name"))
                .lore(
                        tr("gui.rod.basic.lore1"),
                        "",
                        tr("gui.rod.basic.lore2"),
                        tr("gui.rod.multiplier", Map.of("multiplier", formatMultiplier("basic"))),
                        tr("gui.rod.basic.lore3"),
                        "",
                        currentTier.equals("basic") ? tr("gui.rod.basic.equipped") : tr("gui.rod.basic.click")
                )
                .glow(currentTier.equals("basic"))
                .build();
        setItem(10, basicRod, () -> {
            if (currentTier.equals("basic")) {
                playClickSound();
                sendMessage(tr("gui.rod.already_selected", Map.of("tier", tr("gui.rod.basic.label"))));
                return;
            }
            playClickSound();
            setRodTier(player, "basic");
            playSuccessSound();
            sendMessage(tr("gui.rod.basic.selected"));
            refresh();
        });

        ItemStack advancedRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.advanced.name"))
                .glow(currentTier.equals("advanced"))
                .lore(
                        tr("gui.rod.advanced.lore1"),
                        "",
                        tr("gui.rod.advanced.lore2"),
                        tr("gui.rod.multiplier", Map.of("multiplier", formatMultiplier("advanced"))),
                        tr("gui.rod.advanced.lore3"),
                        tr("gui.rod.advanced.lore4"),
                        "",
                        currentTier.equals("advanced") ? tr("gui.rod.advanced.equipped") : tr("gui.rod.advanced.click")
                )
                .build();
        setItem(12, advancedRod, () -> {
            if (currentTier.equals("advanced")) {
                playClickSound();
                sendMessage(tr("gui.rod.already_selected", Map.of("tier", tr("gui.rod.advanced.label"))));
                return;
            }
            if (player.hasPermission(PermissionNodes.ROD_ADVANCED)) {
                playClickSound();
                setRodTier(player, "advanced");
                playSuccessSound();
                sendMessage(tr("gui.rod.advanced.selected"));
                refresh();
            } else {
                playErrorSound();
                sendMessage(tr("gui.rod.advanced.locked"));
            }
        });

        ItemStack legendaryRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.legendary.name"))
                .glow(currentTier.equals("legendary"))
                .lore(
                        tr("gui.rod.legendary.lore1"),
                        "",
                        tr("gui.rod.legendary.lore2"),
                        tr("gui.rod.multiplier", Map.of("multiplier", formatMultiplier("legendary"))),
                        tr("gui.rod.legendary.lore3"),
                        tr("gui.rod.legendary.lore4"),
                        tr("gui.rod.legendary.lore5"),
                        "",
                        currentTier.equals("legendary") ? tr("gui.rod.legendary.equipped") : tr("gui.rod.legendary.click")
                )
                .build();
        setItem(14, legendaryRod, () -> {
            if (currentTier.equals("legendary")) {
                playClickSound();
                sendMessage(tr("gui.rod.already_selected", Map.of("tier", tr("gui.rod.legendary.label"))));
                return;
            }
            if (player.hasPermission(PermissionNodes.ROD_LEGENDARY)) {
                playClickSound();
                setRodTier(player, "legendary");
                playSuccessSound();
                sendMessage(tr("gui.rod.legendary.selected"));
                refresh();
            } else {
                playErrorSound();
                sendMessage(tr("gui.rod.legendary.locked"));
            }
        });

        ItemStack visualEffects = new ItemBuilder(globalEffectsEnabled && !reducedEffects ? Material.AMETHYST_SHARD : Material.GRAY_DYE)
                .name(tr("gui.rod.effects.name"))
                .lore(
                        tr("gui.rod.effects.lore1"),
                        tr("gui.rod.effects.lore2"),
                        "",
                        globalEffectsEnabled
                            ? reducedEffects ? tr("gui.rod.effects.reduced") : tr("gui.rod.effects.full")
                            : tr("gui.rod.effects.globally_disabled"),
                        "",
                        globalEffectsEnabled ? tr("gui.rod.effects.click") : tr("gui.rod.effects.disabled_click")
                )
                .glow(globalEffectsEnabled && !reducedEffects)
                .build();
        setItem(16, visualEffects, () -> {
            playClickSound();
            if (!globalEffectsEnabled) {
                playErrorSound();
                sendMessage(tr("gui.rod.effects.globally_disabled_message"));
                return;
            }
            plugin.getPlayerDataService().toggleReducedEffects(player);
            sendMessage(plugin.getPlayerDataService().hasReducedEffects(player)
                ? tr("gui.rod.effects.reduced_message")
                : tr("gui.rod.effects.full_message"));
            playSuccessSound();
            refresh();
        });

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name(tr("gui.rod.back.name"))
                .lore(tr("gui.rod.back.lore"))
                .build();
        setItem(18, backItem, () -> {
            playClickSound();
            plugin.getGUIManager().openMenu(player, "main");
        });

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.rod.close.name"))
                .lore(tr("gui.rod.close.lore"))
                .build();
        setItem(26, closeItem, () -> {
            playClickSound();
            player.closeInventory();
        });
    }

    private String getCurrentRodTier(Player player) {
        return plugin.getPlayerDataService().getRodTier(player);
    }

    private void setRodTier(Player player, String tier) {
        plugin.getPlayerDataService().setRodTier(player, tier);
    }

    private String formatMultiplier(String tier) {
        return String.format(
            Locale.ROOT,
            "%.2f",
            plugin.getConfigManager().getRodLuckMultiplier(tier)
        );
    }
}
