package io.xcutiboo.mythicrod.gui.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.item.ItemBuilder;

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
        
        ItemStack basicRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.basic.name"))
                .lore(
                        tr("gui.rod.basic.lore1"),
                        "",
                        tr("gui.rod.basic.lore2"),
                        tr("gui.rod.basic.lore3"),
                        "",
                        currentTier.equals("basic") ? tr("gui.rod.basic.equipped") : tr("gui.rod.basic.click")
                )
                .build();
        setItem(10, basicRod, event -> {
            if (!currentTier.equals("basic")) {
                playClickSound();
                setRodTier(player, "basic");
                playSuccessSound();
                player.sendMessage("<green>Your rod tier has been set to Basic!");
                refresh();
            }
        });
        
        ItemStack advancedRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.advanced.name"))
                .glow(true)
                .lore(
                        tr("gui.rod.advanced.lore1"),
                        "",
                        tr("gui.rod.advanced.lore2"),
                        tr("gui.rod.advanced.lore3"),
                        tr("gui.rod.advanced.lore4"),
                        "",
                        currentTier.equals("advanced") ? tr("gui.rod.advanced.equipped") : tr("gui.rod.advanced.click")
                )
                .build();
        setItem(12, advancedRod, event -> {
            if (!currentTier.equals("advanced")) {
                if (player.hasPermission("mythicrod.rod.advanced")) {
                    playClickSound();
                    setRodTier(player, "advanced");
                    playSuccessSound();
                    player.sendMessage("<green>Your rod tier has been set to Advanced!");
                    refresh();
                } else {
                    playErrorSound();
                    player.sendMessage("<red>You need permission to use the Advanced rod!");
                }
            }
        });
        
        ItemStack legendaryRod = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.rod.legendary.name"))
                .glow(true)
                .lore(
                        tr("gui.rod.legendary.lore1"),
                        "",
                        tr("gui.rod.legendary.lore2"),
                        tr("gui.rod.legendary.lore3"),
                        tr("gui.rod.legendary.lore4"),
                        tr("gui.rod.legendary.lore5"),
                        "",
                        currentTier.equals("legendary") ? tr("gui.rod.legendary.equipped") : tr("gui.rod.legendary.click")
                )
                .build();
        setItem(14, legendaryRod, event -> {
            if (!currentTier.equals("legendary")) {
                if (player.hasPermission("mythicrod.rod.legendary")) {
                    playClickSound();
                    setRodTier(player, "legendary");
                    playSuccessSound();
                    player.sendMessage("<green>Your rod tier has been set to Legendary!");
                    refresh();
                } else {
                    playErrorSound();
                    player.sendMessage("<red>You need permission to use the Legendary rod!");
                }
            }
        });
        
        ItemStack autoRetrieve = new ItemBuilder(Material.CHEST)
                .name(tr("gui.rod.auto_retrieve.name"))
                .lore(
                        tr("gui.rod.auto_retrieve.lore1"),
                        "",
                        tr("gui.rod.auto_retrieve.lore2"),
                        "",
                        tr("gui.rod.auto_retrieve.lore3")
                )
                .build();
        setItem(16, autoRetrieve, event -> {
            playClickSound();
            player.sendMessage("<green>Auto-Retrieve feature coming soon!");
        });
        
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name(tr("gui.rod.back.name"))
                .lore(tr("gui.rod.back.lore"))
                .build();
        setItem(18, backItem, event -> {
            playClickSound();
            plugin.getGUIManager().openMenu(player, "main");
        });
        
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.rod.close.name"))
                .lore(tr("gui.rod.close.lore"))
                .build();
        setItem(26, closeItem, event -> {
            playClickSound();
            player.closeInventory();
        });
    }
    
    private String getCurrentRodTier(Player player) {
        return plugin.getPlayerDataService().getRodTier(player);
    }
    
    private void setRodTier(Player player, String tier) {
        plugin.getPlayerDataService().setRodTier(player, tier);
        player.sendMessage("<green>Rod tier changed to: " + tier);
    }
}
