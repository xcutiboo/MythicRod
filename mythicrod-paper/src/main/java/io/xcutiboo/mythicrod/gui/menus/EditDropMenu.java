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
 * GUI menu for editing individual drop properties in-game.
 *
 * <p>Context keys (set via {@link #setContext(Map)} before {@link #open()}):
 * <ul>
 *   <li>{@code "drop"}     — {@link CustomDrop} to edit (required)</li>
 *   <li>{@code "category"} — {@link String} category name (required)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * plugin.getGUIManager().openMenu(player, "editdrop",
 *     Map.of("drop", drop, "category", category));
 * }</pre>
 */
public class EditDropMenu extends BaseMenu {

    private static final String ADMIN_PERMISSION = "mythicrod.admin.config";

    // Editable values — populated from context in build()
    private CustomDrop drop;
    private String category;
    private int currentChance;
    private int currentAmount;
    private String currentName;
    private List<String> currentLore;
    private boolean currentGlow;

    /** No-arg-style constructor — satisfies {@code MenuFactory} (plugin, player). */
    public EditDropMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BaseMenu contract
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected int getSize() {
        return 54;
    }

    @Override
    protected String getTitle() {
        CustomDrop d = getContext("drop", CustomDrop.class);
        String id = (d != null) ? d.getIdentifier() : "?";
        return tr("gui.edit_drop.title", Map.of("identifier", id));
    }

    @Override
    protected void build() {
        // Load context — close gracefully if missing
        drop = getContext("drop", CustomDrop.class);
        category = getContext("category", String.class);
        if (drop == null || category == null) {
            Player p = getPlayer();
            if (p != null) {
                sendMessage("<red>✗ <dark_red>Failed to open drop editor — missing context.");
                p.closeInventory();
            }
            return;
        }

        // Initialise editable fields only on first build (not refresh)
        if (currentChance == 0 && currentLore == null) {
            currentChance = drop.getChance();
            currentAmount = drop.getAmount();
            currentName   = drop.getCustomName();
            currentLore   = new ArrayList<>(drop.getLore());
            currentGlow   = drop.isGlowing();
        }

        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        buildItemPreview();
        buildChanceEditor();
        buildAmountEditor();
        buildNameEditor();
        buildLoreEditor();
        buildGlowToggle();
        buildSaveButton();
        buildResetButton();
        buildDeleteButton();
        buildBackButton();
        buildInfoPanel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Panel builders
    // ─────────────────────────────────────────────────────────────────────────

    private void buildItemPreview() {
        Material material = Material.matchMaterial(drop.getIdentifier());
        if (material == null) material = Material.PAPER;

        ItemBuilder preview = ItemBuilder.of(material)
                .amount(currentAmount)
                .name(currentName != null
                        ? currentName
                        : tr("gui.edit_drop.preview.default_name",
                             Map.of("material", formatMaterialName(drop.getIdentifier()))));

        if (!currentLore.isEmpty()) {
            preview.addLore(tr("gui.edit_drop.preview.lore_header"));
            for (String line : currentLore) {
                preview.addLore(line);
            }
        }

        preview.addLore(tr("gui.edit_drop.preview.stats_header"))
               .addLore(tr("gui.edit_drop.preview.chance", Map.of("chance", String.valueOf(currentChance))))
               .addLore(tr("gui.edit_drop.preview.amount", Map.of("amount", String.valueOf(currentAmount))))
               .addLore(tr("gui.edit_drop.preview.glow",
                           Map.of("status", currentGlow ? tr("gui.edit_drop.enabled") : tr("gui.edit_drop.disabled"))));

        if (currentGlow) preview.glow();
        setItem(13, preview.build());
    }

    private void buildChanceEditor() {
        setItem(20,
                ItemBuilder.of(Material.CLOCK)
                        .name(tr("gui.edit_drop.chance.name"))
                        .addLore(tr("gui.edit_drop.chance.current", Map.of("chance", String.valueOf(currentChance))))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.chance.left_click"))
                        .addLore(tr("gui.edit_drop.chance.right_click"))
                        .addLore(tr("gui.edit_drop.chance.shift_left"))
                        .addLore(tr("gui.edit_drop.chance.shift_right"))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.chance.range"))
                        .build(),
                event -> {
                    playClickSound();
                    switch (event.getClick()) {
                        case LEFT        -> currentChance = Math.min(100, currentChance + 1);
                        case RIGHT       -> currentChance = Math.max(1,   currentChance - 1);
                        case SHIFT_LEFT  -> currentChance = Math.min(100, currentChance + 10);
                        case SHIFT_RIGHT -> currentChance = Math.max(1,   currentChance - 10);
                        default          -> { /* ignored */ }
                    }
                    refresh();
                });
    }

    private void buildAmountEditor() {
        setItem(21,
                ItemBuilder.of(Material.CHEST)
                        .name(tr("gui.edit_drop.amount.name"))
                        .addLore(tr("gui.edit_drop.amount.current", Map.of("amount", String.valueOf(currentAmount))))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.amount.left_click"))
                        .addLore(tr("gui.edit_drop.amount.right_click"))
                        .addLore(tr("gui.edit_drop.amount.shift_click"))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.amount.range"))
                        .build(),
                event -> {
                    playClickSound();
                    switch (event.getClick()) {
                        case LEFT        -> currentAmount = Math.min(64, currentAmount + 1);
                        case RIGHT       -> currentAmount = Math.max(1,  currentAmount - 1);
                        case SHIFT_LEFT  -> currentAmount = Math.min(64, currentAmount + 16);
                        case SHIFT_RIGHT -> currentAmount = Math.max(1,  currentAmount - 16);
                        default          -> { /* ignored */ }
                    }
                    refresh();
                });
    }

    private void buildNameEditor() {
        // Cycle through a small set of preset name templates
        List<String> presets = List.of(
                null,
                "<gold>✨ Legendary " + formatMaterialName(drop.getIdentifier()) + "</gold>",
                "<aqua>★ Rare "      + formatMaterialName(drop.getIdentifier()) + "</aqua>",
                "<green>♦ Uncommon " + formatMaterialName(drop.getIdentifier()) + "</green>",
                "<yellow>◇ Common "  + formatMaterialName(drop.getIdentifier()) + "</yellow>",
                "<red>⚔ "            + formatMaterialName(drop.getIdentifier()) + " of Power</red>"
        );

        String displayName = currentName != null ? currentName : "<gray>Default (material name)</gray>";

        setItem(22,
                ItemBuilder.of(Material.NAME_TAG)
                        .name(tr("gui.edit_drop.name.name"))
                        .addLore(tr("gui.edit_drop.name.current"))
                        .addLore(displayName)
                        .addLore("")
                        .addLore(tr("gui.edit_drop.name.click_cycle"))
                        .addLore(tr("gui.edit_drop.name.click_save"))
                        .build(),
                event -> {
                    playClickSound();
                    int idx = -1;
                    for (int i = 0; i < presets.size(); i++) {
                        if (java.util.Objects.equals(currentName, presets.get(i))) {
                            idx = i;
                            break;
                        }
                    }
                    currentName = presets.get((idx + 1) % presets.size());
                    refresh();
                });
    }

    private void buildLoreEditor() {
        setItem(23,
                ItemBuilder.of(Material.BOOK)
                        .name(tr("gui.edit_drop.lore.name"))
                        .addLore(tr("gui.edit_drop.lore.lines", Map.of("count", String.valueOf(currentLore.size()))))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.lore.left_click"))
                        .addLore(tr("gui.edit_drop.lore.right_click"))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.lore.max"))
                        .build(),
                event -> {
                    playClickSound();
                    if (event.getClick() == ClickType.LEFT) {
                        addRandomLoreLine();
                    } else if (event.getClick() == ClickType.RIGHT) {
                        if (!currentLore.isEmpty()) {
                            currentLore.remove(currentLore.size() - 1);
                        } else {
                            currentLore.clear();
                        }
                        sendMessage("<red>✗ Lore cleared!</red>");
                        playErrorSound();
                    }
                    refresh();
                });
    }

    private void buildGlowToggle() {
        Material mat  = currentGlow ? Material.GLOWSTONE_DUST : Material.GUNPOWDER;
        String   name = currentGlow
                ? "<green><bold>✓ Glow Enabled</bold></green>"
                : "<red><bold>✗ Glow Disabled</bold></red>";

        setItem(24,
                ItemBuilder.of(mat)
                        .name(name)
                        .addLore(tr("gui.edit_drop.glow.lore1"))
                        .addLore(tr("gui.edit_drop.glow.lore2"))
                        .build(),
                event -> {
                    playClickSound();
                    currentGlow = !currentGlow;
                    refresh();
                });
    }

    private void buildSaveButton() {
        setItem(38,
                ItemBuilder.of(Material.LIME_CONCRETE)
                        .name(tr("gui.edit_drop.save.name"))
                        .addLore(tr("gui.edit_drop.save.lore1"))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.save.lore2"))
                        .addLore(tr("gui.edit_drop.save.chance", Map.of("chance", String.valueOf(currentChance))))
                        .addLore(tr("gui.edit_drop.save.amount", Map.of("amount", String.valueOf(currentAmount))))
                        .addLore(tr("gui.edit_drop.save.glow",
                                    Map.of("status", currentGlow ? tr("gui.edit_drop.yes") : tr("gui.edit_drop.no"))))
                        .glow()
                        .build(),
                event -> {
                    playSuccessSound();
                    saveChanges();
                    sendMessage("<green><bold>✓ Drop saved successfully!</bold></green>");
                    plugin.getGUIManager().openMenu(getPlayer(), "drops",
                            Map.of("category", category, "viewing_category", true));
                });
    }

    private void buildResetButton() {
        setItem(39,
                ItemBuilder.of(Material.ORANGE_CONCRETE)
                        .name(tr("gui.edit_drop.reset.name"))
                        .addLore(tr("gui.edit_drop.reset.lore1"))
                        .addLore(tr("gui.edit_drop.reset.lore2"))
                        .build(),
                event -> {
                    playClickSound();
                    currentChance = drop.getChance();
                    currentAmount = drop.getAmount();
                    currentName   = drop.getCustomName();
                    currentLore   = new ArrayList<>(drop.getLore());
                    currentGlow   = drop.isGlowing();
                    sendMessage("<yellow>↺ Values reset to original!</yellow>");
                    refresh();
                });
    }

    private void buildDeleteButton() {
        setItem(41,
                ItemBuilder.of(Material.BARRIER)
                        .name(tr("gui.edit_drop.delete.name"))
                        .addLore(tr("gui.edit_drop.delete.lore1"))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.delete.lore2"))
                        .build(),
                event -> {
                    if (event.getClick() == ClickType.SHIFT_LEFT
                            || event.getClick() == ClickType.SHIFT_RIGHT) {
                        playErrorSound();
                        deleteDrop();
                        sendMessage("<dark_red><bold>🗑 Drop deleted permanently!</bold></dark_red>");
                        plugin.getGUIManager().openMenu(getPlayer(), "drops",
                                Map.of("category", category, "viewing_category", true));
                    } else {
                        sendMessage("<red>⚠ Shift+Click to confirm deletion</red>");
                        playErrorSound();
                    }
                });
    }

    private void buildBackButton() {
        setItem(45,
                ItemBuilder.of(Material.ARROW)
                        .name(tr("gui.edit_drop.back.name"))
                        .addLore(tr("gui.edit_drop.back.lore"))
                        .build(),
                event -> {
                    playClickSound();
                    plugin.getGUIManager().openMenu(getPlayer(), "drops",
                            Map.of("category", category, "viewing_category", true));
                });
    }

    private void buildInfoPanel() {
        setItem(49,
                ItemBuilder.of(Material.KNOWLEDGE_BOOK)
                        .name(tr("gui.edit_drop.info.name"))
                        .addLore(tr("gui.edit_drop.info.lore1",
                                    Map.of("identifier", drop.getIdentifier())))
                        .addLore(tr("gui.edit_drop.info.lore2",
                                    Map.of("material", formatMaterialName(drop.getIdentifier()))))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.info.lore3"))
                        .addLore(tr("gui.edit_drop.info.lore4"))
                        .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void addRandomLoreLine() {
        if (currentLore.size() >= 10) {
            sendMessage("<red>⚠ Maximum 10 lore lines allowed!</red>");
            playErrorSound();
            return;
        }
        List<String> pool = List.of(
                "<gray>A rare find from the depths</gray>",
                "<aqua>✦ Enchanted by ancient magic</aqua>",
                "<green>♦ Valuable treasure</green>",
                "<yellow>◇ Commonly found by fishermen</yellow>",
                "<gold>⚓ From sunken ships</gold>"
        );
        currentLore.add(pool.get(currentLore.size() % pool.size()));
        sendMessage("<green>✓ Lore line added!</green>");
    }

    private void saveChanges() {
        // Capture finals for the async lambda
        final CustomDrop  finalDrop     = drop;
        final String      finalCategory = category;
        final int         finalChance   = currentChance;
        final int         finalAmount   = currentAmount;
        final String      finalName     = currentName;
        final List<String>finalLore     = List.copyOf(currentLore);
        final boolean     finalGlow     = currentGlow;
        final Player      finalPlayer   = getPlayer();

        plugin.getPlatformScheduler().runAsync(() -> {
            try {
                plugin.getDropManager().updateDrop(
                        finalDrop.getIdentifier(), finalCategory,
                        finalChance, finalAmount, finalName, finalLore, finalGlow);
                plugin.getDropManager().saveDropsConfig();
                plugin.getLogger().info(String.format(
                        "[EditDropMenu] Drop '%s' in '%s' updated by %s",
                        finalDrop.getIdentifier(), finalCategory,
                        finalPlayer != null ? finalPlayer.getName() : "unknown"));
            } catch (Exception e) {
                plugin.getLogger().warning("[EditDropMenu] Failed to save drop: " + e.getMessage());
                if (finalPlayer != null && finalPlayer.isOnline()) {
                    finalPlayer.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>✗ Error saving drop: " + e.getMessage() + "</red>"));
                }
            }
        });
    }

    private void deleteDrop() {
        final CustomDrop finalDrop     = drop;
        final String     finalCategory = category;
        final Player     finalPlayer   = getPlayer();

        plugin.getPlatformScheduler().runAsync(() -> {
            try {
                plugin.getDropManager().deleteDrop(finalDrop.getIdentifier(), finalCategory);
                plugin.getDropManager().saveDropsConfig();
                plugin.getLogger().info(String.format(
                        "[EditDropMenu] Drop '%s' in '%s' deleted by %s",
                        finalDrop.getIdentifier(), finalCategory,
                        finalPlayer != null ? finalPlayer.getName() : "unknown"));
            } catch (Exception e) {
                plugin.getLogger().warning("[EditDropMenu] Failed to delete drop: " + e.getMessage());
            }
        });
    }

    private String formatMaterialName(String identifier) {
        if (identifier == null || identifier.isEmpty()) return "Unknown";
        String clean = identifier.contains(":") ? identifier.substring(identifier.indexOf(':') + 1) : identifier;
        String[] parts = clean.toLowerCase(java.util.Locale.ROOT).split("[_\\s]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        return sb.toString();
    }

    @Override
    public String getRequiredPermission() {
        return ADMIN_PERMISSION;
    }
}
