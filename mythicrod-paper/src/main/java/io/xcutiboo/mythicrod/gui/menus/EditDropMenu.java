package io.xcutiboo.mythicrod.gui.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import net.kyori.adventure.text.minimessage.MiniMessage;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.item.ItemBuilder;

/**
 * Menu for editing drop properties directly in GUI.
 * Provides interactive controls for chance, amount, name, and lore.
 */
public class EditDropMenu extends BaseMenu {
    private static final String ADMIN_PERMISSION = "mythicrod.admin.config";
    private final CustomDrop drop;
    private final String category;
    
    // Editable values
    private int currentChance;
    private int currentAmount;
    private String currentName;
    private List<String> currentLore;
    private boolean currentGlow;

    public EditDropMenu(MythicRod plugin, Player player, CustomDrop drop, String category) {
        super(plugin, player);
        this.drop = drop;
        this.category = category;
        
        // Initialize with current values
        this.currentChance = drop.getChance();
        this.currentAmount = drop.getAmount();
        this.currentName = drop.getCustomName();
        this.currentLore = new ArrayList<>(drop.getLore());
        this.currentGlow = drop.isGlowing();
    }

    @Override
    protected int getSize() {
        return 54;
    }

    @Override
    protected String getTitle() {
        return tr("gui.edit_drop.title", Map.of("%identifier%", drop.getIdentifier()));
    }

    @Override
    protected void build() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        
        // Item preview at center top
        buildItemPreview();
        
        // Edit controls
        buildChanceEditor();
        buildAmountEditor();
        buildNameEditor();
        buildLoreEditor();
        buildGlowToggle();
        
        // Action buttons
        buildSaveButton();
        buildResetButton();
        buildDeleteButton();
        buildBackButton();
        
        // Info panel
        buildInfoPanel();
    }
    
    private void buildItemPreview() {
        Material material = Material.matchMaterial(drop.getIdentifier());
        if (material == null) {
            material = Material.PAPER;
        }
        
        ItemBuilder previewBuilder = ItemBuilder.of(material)
                .amount(currentAmount)
                .name(currentName != null ? currentName : tr("gui.edit_drop.preview.default_name", Map.of("%material%", formatMaterialName(drop.getIdentifier()))));
        
        // Add lore preview
        if (!currentLore.isEmpty()) {
            previewBuilder.addLore(tr("gui.edit_drop.preview.lore_header"));
            for (String line : currentLore) {
                previewBuilder.addLore(line);
            }
        }
        
        previewBuilder.addLore(tr("gui.edit_drop.preview.stats_header"))
                .addLore(tr("gui.edit_drop.preview.chance", Map.of("%chance%", String.valueOf(currentChance))))
                .addLore(tr("gui.edit_drop.preview.amount", Map.of("%amount%", String.valueOf(currentAmount))))
                .addLore(tr("gui.edit_drop.preview.glow", Map.of("%status%", currentGlow ? tr("gui.edit_drop.enabled") : tr("gui.edit_drop.disabled"))));
        
        if (currentGlow) {
            previewBuilder.glow();
        }
        
        setItem(13, previewBuilder.build());
    }
    
    private void buildChanceEditor() {
        ItemBuilder chanceBuilder = ItemBuilder.of(Material.CLOCK)
                .name(tr("gui.edit_drop.chance.name"))
                .addLore(tr("gui.edit_drop.chance.current", Map.of("%chance%", String.valueOf(currentChance))))
                .addLore("")
                .addLore(tr("gui.edit_drop.chance.left_click"))
                .addLore(tr("gui.edit_drop.chance.right_click"))
                .addLore(tr("gui.edit_drop.chance.shift_left"))
                .addLore(tr("gui.edit_drop.chance.shift_right"))
                .addLore("")
                .addLore(tr("gui.edit_drop.chance.range"));
        
        setItem(20, chanceBuilder.build(), event -> {
            playClickSound();
            if (event.getClick() == ClickType.LEFT) {
                currentChance = Math.min(100, currentChance + 1);
            } else if (event.getClick() == ClickType.RIGHT) {
                currentChance = Math.max(1, currentChance - 1);
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                currentChance = Math.min(100, currentChance + 10);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                currentChance = Math.max(1, currentChance - 10);
            }
            refresh();
        });
    }
    
    private void buildAmountEditor() {
        ItemBuilder amountBuilder = ItemBuilder.of(Material.CHEST)
                .name(tr("gui.edit_drop.amount.name"))
                .addLore(tr("gui.edit_drop.amount.current", Map.of("%amount%", String.valueOf(currentAmount))))
                .addLore("")
                .addLore(tr("gui.edit_drop.amount.left_click"))
                .addLore(tr("gui.edit_drop.amount.right_click"))
                .addLore(tr("gui.edit_drop.amount.shift_click"))
                .addLore("")
                .addLore(tr("gui.edit_drop.amount.range"));
        
        setItem(21, amountBuilder.build(), event -> {
            playClickSound();
            if (event.getClick() == ClickType.LEFT) {
                currentAmount = Math.min(64, currentAmount + 1);
            } else if (event.getClick() == ClickType.RIGHT) {
                currentAmount = Math.max(1, currentAmount - 1);
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                currentAmount = Math.min(64, currentAmount + 16);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                currentAmount = Math.max(1, currentAmount - 16);
            }
            refresh();
        });
    }
    
    private void buildNameEditor() {
        List<String> presetNames = List.of(
            null,
            "<gold>✨ Legendary " + formatMaterialName(drop.getIdentifier()) + "</gold>",
            "<aqua>★ Rare " + formatMaterialName(drop.getIdentifier()) + "</aqua>",
            "<green>♦ Uncommon " + formatMaterialName(drop.getIdentifier()) + "</green>",
            "<yellow>◇ Common " + formatMaterialName(drop.getIdentifier()) + "</yellow>",
            "<red>⚔ " + formatMaterialName(drop.getIdentifier()) + " of Power</red>"
        );
        
        String currentDisplay = currentName != null ? currentName : "<gray>Default (material name)</gray>";
        
        ItemBuilder nameBuilder = ItemBuilder.of(Material.NAME_TAG)
                .name(tr("gui.edit_drop.name.name"))
                .addLore(tr("gui.edit_drop.name.current"))
                .addLore(currentDisplay)
                .addLore("")
                .addLore(tr("gui.edit_drop.name.click_cycle"))
                .addLore(tr("gui.edit_drop.name.click_save"));
        
        setItem(22, nameBuilder.build(), event -> {
            playClickSound();
            int currentIndex = -1;
            for (int i = 0; i < presetNames.size(); i++) {
                if (java.util.Objects.equals(currentName, presetNames.get(i))) {
                    currentIndex = i;
                    break;
                }
            }
            int nextIndex = (currentIndex + 1) % presetNames.size();
            currentName = presetNames.get(nextIndex);
            refresh();
        });
    }
    
    private void buildLoreEditor() {
        ItemBuilder loreBuilder = ItemBuilder.of(Material.BOOK)
                .name(tr("gui.edit_drop.lore.name"))
                .addLore(tr("gui.edit_drop.lore.lines", Map.of("%count%", String.valueOf(currentLore.size()))))
                .addLore("")
                .addLore(tr("gui.edit_drop.lore.left_click"))
                .addLore(tr("gui.edit_drop.lore.right_click"))
                .addLore("")
                .addLore(tr("gui.edit_drop.lore.max"));
        
        setItem(23, loreBuilder.build(), event -> {
            playClickSound();
            if (event.getClick() == ClickType.LEFT) {
                addRandomLoreLine();
            } else if (event.getClick() == ClickType.RIGHT) {
                currentLore.clear();
                sendMessage("<red>Lore cleared!</red>");
                playErrorSound();
            }
            refresh();
        });
    }
    
    private void buildGlowToggle() {
        Material glowMaterial = currentGlow ? Material.GLOWSTONE_DUST : Material.GUNPOWDER;
        String glowName = currentGlow ? 
                "<green><bold>✓ Glow Enabled</bold></green>" : 
                "<red><bold>✗ Glow Disabled</bold></red>";
        
        ItemBuilder glowBuilder = ItemBuilder.of(glowMaterial)
                .name(glowName)
                .addLore(tr("gui.edit_drop.glow.lore1"))
                .addLore(tr("gui.edit_drop.glow.lore2"));
        
        setItem(24, glowBuilder.build(), event -> {
            playClickSound();
            currentGlow = !currentGlow;
            refresh();
        });
    }
    
    private void buildSaveButton() {
        ItemBuilder saveBuilder = ItemBuilder.of(Material.LIME_CONCRETE)
                .name(tr("gui.edit_drop.save.name"))
                .addLore(tr("gui.edit_drop.save.lore1"))
                .addLore("")
                .addLore(tr("gui.edit_drop.save.lore2"))
                .addLore(tr("gui.edit_drop.save.chance", Map.of("%chance%", String.valueOf(currentChance))))
                .addLore(tr("gui.edit_drop.save.amount", Map.of("%amount%", String.valueOf(currentAmount))))
                .addLore(tr("gui.edit_drop.save.glow", Map.of("%status%", currentGlow ? tr("gui.edit_drop.yes") : tr("gui.edit_drop.no"))))
                .glow();
        
        setItem(38, saveBuilder.build(), event -> {
            playSuccessSound();
            saveChanges();
            sendMessage("<green><bold>✓ Drop saved successfully!</bold></green>");
            
            // Return to drops menu
            plugin.getGUIManager().openMenu(getPlayer(), "drops", 
                    Map.of("category", category, "viewing_category", true));
        });
    }
    
    private void buildResetButton() {
        ItemBuilder resetBuilder = ItemBuilder.of(Material.RED_CONCRETE)
                .name(tr("gui.edit_drop.reset.name"))
                .addLore(tr("gui.edit_drop.reset.lore1"))
                .addLore(tr("gui.edit_drop.reset.lore2"));
        
        setItem(39, resetBuilder.build(), event -> {
            playClickSound();
            currentChance = drop.getChance();
            currentAmount = drop.getAmount();
            currentName = drop.getCustomName();
            currentLore = new ArrayList<>(drop.getLore());
            currentGlow = drop.isGlowing();
            sendMessage("<yellow>Values reset to original!</yellow>");
            refresh();
        });
    }
    
    private void buildDeleteButton() {
        ItemBuilder deleteBuilder = ItemBuilder.of(Material.BARRIER)
                .name(tr("gui.edit_drop.delete.name"))
                .addLore(tr("gui.edit_drop.delete.lore1"))
                .addLore("")
                .addLore(tr("gui.edit_drop.delete.lore2"));
        
        setItem(41, deleteBuilder.build(), event -> {
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                playErrorSound();
                deleteDrop();
                sendMessage("<dark_red><bold>🗑 Drop deleted!</bold></dark_red>");
                
                // Return to drops menu
                plugin.getGUIManager().openMenu(getPlayer(), "drops", 
                        Map.of("category", category, "viewing_category", true));
            } else {
                sendMessage("<red>⚠ Shift+Click to confirm deletion</red>");
                playErrorSound();
            }
        });
    }
    
    private void buildBackButton() {
        ItemBuilder backBuilder = ItemBuilder.of(Material.ARROW)
                .name(tr("gui.edit_drop.back.name"))
                .addLore(tr("gui.edit_drop.back.lore"));
        
        setItem(45, backBuilder.build(), event -> {
            playClickSound();
            plugin.getGUIManager().openMenu(getPlayer(), "drops", 
                    Map.of("category", category, "viewing_category", true));
        });
    }
    
    private void buildInfoPanel() {
        ItemBuilder infoBuilder = ItemBuilder.of(Material.BOOK)
                .name(tr("gui.edit_drop.info.name"))
                .addLore(tr("gui.edit_drop.info.lore1"))
                .addLore(tr("gui.edit_drop.info.lore2"))
                .addLore("")
                .addLore(tr("gui.edit_drop.info.lore3"))
                .addLore(tr("gui.edit_drop.info.lore4"));
        
        setItem(49, infoBuilder.build());
    }
    
    private void addRandomLoreLine() {
        if (currentLore.size() >= 5) {
            sendMessage("<red>Maximum 5 lore lines allowed!</red>");
            playErrorSound();
            return;
        }
        
        List<String> presetLore = List.of(
            "<gray>A rare find from the depths</gray>",
            "<aqua>✦ Enchanted by ancient magic</aqua>",
            "<green>♦ Valuable treasure</green>",
            "<yellow>◇ Commonly found by fishermen</yellow>",
            "<gold>⚓ From sunken ships</gold>"
        );
        
        String newLine = presetLore.get(currentLore.size() % presetLore.size());
        currentLore.add(newLine);
        sendMessage("<green>Lore line added!</green>");
    }
    
    private void saveChanges() {
        // Build updated drop configuration
        plugin.getPlatformScheduler().runAsync(() -> {
            try {
                // Save to DropManager
                plugin.getDropManager().updateDrop(drop.getIdentifier(), category, 
                        currentChance, currentAmount, currentName, currentLore, currentGlow);
                
                // Save to config file
                plugin.getDropManager().saveDropsConfig();
                
                // Log success
                plugin.getLogger().info("Drop updated via GUI: " + drop.getIdentifier() + 
                        " by " + getPlayer().getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save drop: " + e.getMessage());
                getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Error saving drop: " + e.getMessage() + "</red>"));
            }
        });
    }
    
    private void deleteDrop() {
        plugin.getPlatformScheduler().runAsync(() -> {
            try {
                plugin.getDropManager().deleteDrop(drop.getIdentifier(), category);
                plugin.getDropManager().saveDropsConfig();
                plugin.getLogger().info("Drop deleted via GUI: " + drop.getIdentifier() + 
                        " by " + getPlayer().getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to delete drop: " + e.getMessage());
            }
        });
    }
    
    private String formatMaterialName(String identifier) {
        if (identifier == null || identifier.isEmpty()) return "Unknown";
        String[] parts = identifier.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            String part = parts[i];
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1));
                }
            }
        }
        return result.toString();
    }
    
    @Override
    public String getRequiredPermission() {
        return ADMIN_PERMISSION;
    }
}
