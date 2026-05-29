package io.xcutiboo.mythicrod.paper.gui.menus;

import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.constants.PermissionNodes;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;

public class RodMenu extends BaseMenu {
    private static final String TIER_BASIC = "basic";
    private static final String TIER_ADVANCED = "advanced";
    private static final String TIER_LEGENDARY = "legendary";
    private static final String TIER_MYTHIC = "mythic";
    private static final String CTX_MULTIPLIER = "multiplier";
    private static final String TR_MULTIPLIER = "gui.rod.multiplier";
    private static final String TR_ALREADY_SELECTED = "gui.rod.already_selected";


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

        placeBasicTier(player, currentTier);
        placeAdvancedTier(player, currentTier);
        placeLegendaryTier(player, currentTier);
        placeMythicTier(player, currentTier);
        placeVisualEffectsToggle(player, globalEffectsEnabled, reducedEffects);
        placeBackButton(player);
        placeCloseButton(player);
    }

    private void placeBasicTier(Player player, String currentTier) {
        ItemStack basicRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.basic.name"))
                .lore(
                        tr("gui.rod.basic.lore1"),
                        "",
                        tr("gui.rod.basic.lore2"),
                        tr(TR_MULTIPLIER, Map.of(CTX_MULTIPLIER, formatMultiplier(TIER_BASIC))),
                        tr("gui.rod.basic.lore3"),
                        "",
                        currentTier.equals(TIER_BASIC) ? tr("gui.rod.basic.equipped") : tr("gui.rod.basic.click")
                )
                .glow(currentTier.equals(TIER_BASIC))
                .build();
        setItem(10, basicRod, () -> selectBasicTier(player, currentTier));
    }

    private void selectBasicTier(Player player, String currentTier) {
        playClickSound();
        if (currentTier.equals(TIER_BASIC)) {
            sendMessage(tr(TR_ALREADY_SELECTED, Map.of("tier", tr("gui.rod.basic.label"))));
            return;
        }
        setRodTier(player, TIER_BASIC);
        playSuccessSound();
        sendMessage(tr("gui.rod.basic.selected"));
        refresh();
    }

    private void placeAdvancedTier(Player player, String currentTier) {
        ItemStack advancedRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.advanced.name"))
                .glow(currentTier.equals(TIER_ADVANCED))
                .lore(
                        tr("gui.rod.advanced.lore1"),
                        "",
                        tr("gui.rod.advanced.lore2"),
                        tr(TR_MULTIPLIER, Map.of(CTX_MULTIPLIER, formatMultiplier(TIER_ADVANCED))),
                        tr("gui.rod.advanced.lore3"),
                        tr("gui.rod.advanced.lore4"),
                        "",
                        currentTier.equals(TIER_ADVANCED) ? tr("gui.rod.advanced.equipped") : tr("gui.rod.advanced.click")
                )
                .build();
        setItem(12, advancedRod, () -> selectGatedTier(
            player, currentTier, TIER_ADVANCED, PermissionNodes.ROD_ADVANCED,
            "gui.rod.advanced.label", "gui.rod.advanced.selected", "gui.rod.advanced.locked"));
    }

    private void placeLegendaryTier(Player player, String currentTier) {
        ItemStack legendaryRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.legendary.name"))
                .glow(currentTier.equals(TIER_LEGENDARY))
                .lore(
                        tr("gui.rod.legendary.lore1"),
                        "",
                        tr("gui.rod.legendary.lore2"),
                        tr(TR_MULTIPLIER, Map.of(CTX_MULTIPLIER, formatMultiplier(TIER_LEGENDARY))),
                        tr("gui.rod.legendary.lore3"),
                        tr("gui.rod.legendary.lore4"),
                        tr("gui.rod.legendary.lore5"),
                        "",
                        currentTier.equals(TIER_LEGENDARY) ? tr("gui.rod.legendary.equipped") : tr("gui.rod.legendary.click")
                )
                .build();
        setItem(14, legendaryRod, () -> selectGatedTier(
            player, currentTier, TIER_LEGENDARY, PermissionNodes.ROD_LEGENDARY,
            "gui.rod.legendary.label", "gui.rod.legendary.selected", "gui.rod.legendary.locked"));
    }

    private void placeMythicTier(Player player, String currentTier) {
        ItemStack mythicRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.mythic.name"))
                .glow(currentTier.equals(TIER_MYTHIC))
                .lore(
                        tr("gui.rod.mythic.lore1"),
                        "",
                        tr("gui.rod.mythic.lore2"),
                        tr(TR_MULTIPLIER, Map.of(CTX_MULTIPLIER, formatMultiplier(TIER_MYTHIC))),
                        tr("gui.rod.mythic.lore3"),
                        tr("gui.rod.mythic.lore4"),
                        "",
                        currentTier.equals(TIER_MYTHIC) ? tr("gui.rod.mythic.equipped") : tr("gui.rod.mythic.click")
                )
                .build();
        setItem(16, mythicRod, () -> selectGatedTier(
            player, currentTier, TIER_MYTHIC, PermissionNodes.ROD_MYTHIC,
            "gui.rod.mythic.label", "gui.rod.mythic.selected", "gui.rod.mythic.locked"));
    }

    private void selectGatedTier(Player player, String currentTier, String targetTier,
                                  String permission, String labelKey, String selectedKey, String lockedKey) {
        if (currentTier.equals(targetTier)) {
            playClickSound();
            sendMessage(tr(TR_ALREADY_SELECTED, Map.of("tier", tr(labelKey))));
            return;
        }
        if (!player.hasPermission(permission)) {
            playErrorSound();
            sendMessage(tr(lockedKey));
            return;
        }
        playClickSound();
        setRodTier(player, targetTier);
        playSuccessSound();
        sendMessage(tr(selectedKey));
        refresh();
    }

    private void placeVisualEffectsToggle(Player player, boolean globalEffectsEnabled, boolean reducedEffects) {
        ItemStack visualEffects = new ItemBuilder(globalEffectsEnabled && !reducedEffects ? Material.AMETHYST_SHARD : Material.GRAY_DYE)
                .name(tr("gui.rod.effects.name"))
                .lore(
                        tr("gui.rod.effects.lore1"),
                        tr("gui.rod.effects.lore2"),
                        "",
                        effectsStateLine(globalEffectsEnabled, reducedEffects),
                        "",
                        globalEffectsEnabled ? tr("gui.rod.effects.click") : tr("gui.rod.effects.disabled_click")
                )
                .glow(globalEffectsEnabled && !reducedEffects)
                .build();
        setItem(22, visualEffects, () -> {
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
    }

    private void placeBackButton(Player player) {
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name(tr("gui.rod.back.name"))
                .lore(tr("gui.rod.back.lore"))
                .build();
        setItem(18, backItem, () -> {
            playClickSound();
            plugin.getGUIManager().openMenu(player, "main");
        });
    }

    private void placeCloseButton(Player player) {
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.rod.close.name"))
                .lore(tr("gui.rod.close.lore"))
                .build();
        setItem(26, closeItem, () -> {
            playClickSound();
            player.closeInventory();
        });
    }

    private String effectsStateLine(boolean globalEffectsEnabled, boolean reducedEffects) {
        if (!globalEffectsEnabled) {
            return tr("gui.rod.effects.globally_disabled");
        }
        return reducedEffects ? tr("gui.rod.effects.reduced") : tr("gui.rod.effects.full");
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
