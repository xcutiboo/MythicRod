package io.xcutiboo.mythicrod.paper.gui.menus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.constants.PermissionNodes;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.paper.util.StringFormatting;

/**
 * GUI menu for editing individual drop properties in-game.
 *
 * <p>Context keys (set via {@link #setContext(Map)} before {@link #open()}):
 * <ul>
     *   <li>{@code "drop"} - {@link CustomDrop} to edit (required)</li>
     *   <li>{@code "category"} - {@link String} category name (required)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * plugin.getGUIManager().openMenu(player, "editdrop",
 *     Map.of("drop", drop, "category", category));
 * }</pre>
 */
public class EditDropMenu extends BaseMenu {

    private static final String ADMIN_PERMISSION = PermissionNodes.ADMIN_CONFIG;
    private static final String CTX_DROP = "drop";
    private static final String CTX_CATEGORY = "category";
    private static final String CTX_PAGE = "page";
    private static final String CTX_IDENTIFIER = "identifier";
    private static final String CTX_MATERIAL = "material";
    private static final String CTX_WEIGHT = "weight";
    private static final String CTX_VALUE = "value";
    private static final String CTX_BIOMES = "biomes";
    private static final String CTX_PERMISSION = "permission";
    private static final String DROPS_VIEW = "drops";
    private static final String MINECRAFT_PREFIX = "minecraft:";
    private static final String NEXO_PREFIX = "nexo:";
    private static final String EDITOR_LOG_PREFIX = "[EditDropMenu] Drop '";
    private static final String IN_CATEGORY_FRAGMENT = "' in '";
    private static final String TR_NUMBER_INVALID = "gui.edit_drop.messages.number-invalid";
    private static final String TR_ACTION_BUSY = "gui.edit_drop.messages.action-busy";
    private static final int MAX_IDENTIFIER_LENGTH = 96;
    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_LORE_LINE_LENGTH = 180;
    private static final int MAX_LORE_LINES = 10;
    private static final int MAX_PERMISSION_LENGTH = 128;
    private static final int MAX_BIOME_ENTRIES = 8;
    private static final int MAX_ENCHANTMENT_ENTRIES = 8;
    private static final int MAX_ITEM_FLAG_ENTRIES = 12;
    private static final int MAX_CUSTOM_MODEL_DATA = 1_000_000;
    private static final int MAX_ENCHANTMENT_LEVEL = 255;
    private static final Pattern PERMISSION_NODE_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

    private CustomDrop drop;
    private String category;
    private String currentIdentifier;
    private int currentWeight;
    private int currentAmount;
    private String currentName;
    private List<String> currentLore;
    private int currentCustomModelData;
    private String currentPermission;
    private List<String> currentBiomes;
    private Map<String, Integer> currentEnchantments;
    private List<String> currentItemFlags;
    private boolean currentGlow;
    private int returnPage;
    private boolean actionInProgress;

    public EditDropMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int getSize() {
        return 54;
    }

    @Override
    protected String getTitle() {
        CustomDrop d = getContext(CTX_DROP, CustomDrop.class);
        String id = currentIdentifier != null ? currentIdentifier : (d != null ? d.getIdentifier() : "?");
        return tr("gui.edit_drop.title", Map.of(CTX_IDENTIFIER, id));
    }

    @Override
    protected void build() {
        drop = getContext(CTX_DROP, CustomDrop.class);
        category = getContext(CTX_CATEGORY, String.class);
        Integer requestedReturnPage = getContext(CTX_PAGE, Integer.class);
        returnPage = requestedReturnPage != null ? Math.max(0, requestedReturnPage) : 0;
        if (drop == null || category == null) {
            Player p = getPlayer();
            if (p != null) {
                sendMessage(tr("gui.edit_drop.messages.missing-context"));
                p.closeInventory();
            }
            return;
        }

        if (currentLore == null) {
            currentIdentifier = drop.getIdentifier();
            currentWeight = drop.getWeight();
            currentAmount = drop.getAmount();
            currentName   = drop.getCustomName();
            currentLore   = new ArrayList<>(drop.getLore());
            currentCustomModelData = drop.getCustomModelData();
            currentPermission = drop.getPermission();
            currentBiomes = new ArrayList<>(drop.getBiomes());
            currentEnchantments = new LinkedHashMap<>(drop.getEnchantments());
            currentItemFlags = new ArrayList<>(drop.getItemFlags());
            currentGlow   = drop.isGlowing();
        }

        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        buildItemPreview();
        buildItemEditor();
        buildWeightEditor();
        buildAmountEditor();
        buildNameEditor();
        buildLoreEditor();
        buildGlowToggle();
        buildPermissionEditor();
        buildBiomeEditor();
        buildModelDataEditor();
        buildEnchantmentsEditor();
        buildItemFlagsEditor();
        buildSaveButton();
        buildResetButton();
        buildDeleteButton();
        buildBackButton();
        buildInfoPanel();
    }

    private void buildItemPreview() {
        Material material = materialFromIdentifier(currentIdentifier);
        if (material == null) material = Material.PAPER;

        ItemBuilder preview = ItemBuilder.of(material)
                .amount(currentAmount)
                .name(currentName != null
                        ? currentName
                        : tr("gui.edit_drop.preview.default_name",
                             Map.of(CTX_MATERIAL, currentMaterialName())));

        if (!currentLore.isEmpty()) {
            preview.addLore(tr("gui.edit_drop.preview.lore_header"));
            for (String line : currentLore) {
                preview.addLore(line);
            }
        }

        preview.addLore(tr("gui.edit_drop.preview.stats_header"))
               .addLore(tr("gui.edit_drop.preview.item", Map.of(CTX_IDENTIFIER, currentIdentifier)))
               .addLore(tr("gui.edit_drop.preview.weight", Map.of(CTX_WEIGHT, String.valueOf(currentWeight))))
               .addLore(tr("gui.edit_drop.preview.amount", Map.of("amount", String.valueOf(currentAmount))))
               .addLore(tr("gui.edit_drop.preview.glow",
                           Map.of("status", currentGlow ? tr("gui.edit_drop.enabled") : tr("gui.edit_drop.disabled"))));
        if (currentCustomModelData > 0) {
            preview.addLore(tr("gui.edit_drop.preview.model_data",
                Map.of(CTX_VALUE, String.valueOf(currentCustomModelData))));
        }
        if (currentPermission != null && !currentPermission.isBlank()) {
            preview.addLore(tr("gui.edit_drop.preview.permission", Map.of(CTX_PERMISSION, currentPermission)));
        }
        if (currentBiomes != null && !currentBiomes.isEmpty()) {
            preview.addLore(tr("gui.edit_drop.preview.biomes", Map.of(CTX_BIOMES, formatList(currentBiomes))));
        }

        if (currentGlow) preview.glow();
        setItem(13, preview.build());
    }

    private void buildItemEditor() {
        Material material = materialFromIdentifier(currentIdentifier);
        if (material == null) {
            material = Material.ITEM_FRAME;
        }

        setItem(19,
                ItemBuilder.of(material)
                        .name(tr("gui.edit_drop.item.name"))
                        .addLore(tr("gui.edit_drop.item.current", Map.of(CTX_IDENTIFIER, currentIdentifier)))
                        .addLore(tr("gui.edit_drop.item.material", Map.of(CTX_MATERIAL, currentMaterialName())))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.item.left_click"))
                        .addLore(tr("gui.edit_drop.item.supports"))
                        .build(),
                () -> {
                    playClickSound();
                    requestItemInput();
                });
    }

    private void buildWeightEditor() {
        setItem(20,
                ItemBuilder.of(Material.CLOCK)
                        .name(tr("gui.edit_drop.weight.name"))
                        .addLore(tr("gui.edit_drop.weight.current", Map.of(CTX_WEIGHT, String.valueOf(currentWeight))))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.weight.left_click"))
                        .addLore(tr("gui.edit_drop.weight.right_click"))
                        .addLore(tr("gui.edit_drop.weight.shift_left"))
                        .addLore(tr("gui.edit_drop.weight.shift_right"))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.weight.range"))
                        .build(),
                event -> {
                    playClickSound();
                    switch (event.getClick()) {
                        case LEFT        -> {
                            requestWeightInput();
                            return;
                        }
                        case RIGHT       -> currentWeight = Math.min(100, currentWeight + 1);
                        case SHIFT_LEFT  -> currentWeight = Math.min(100, currentWeight + 10);
                        case SHIFT_RIGHT -> currentWeight = Math.max(1,   currentWeight - 10);
                        default          -> {}
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
                        case LEFT        -> {
                            requestAmountInput();
                            return;
                        }
                        case RIGHT       -> currentAmount = Math.min(64, currentAmount + 1);
                        case SHIFT_LEFT  -> currentAmount = Math.min(64, currentAmount + 10);
                        case SHIFT_RIGHT -> currentAmount = Math.max(1,  currentAmount - 10);
                        default          -> {}
                    }
                    refresh();
                });
    }

    private void buildNameEditor() {
        String materialName = currentMaterialName();
        List<String> presets = new ArrayList<>();
        presets.add(null);
        presets.addAll(List.of(
            tr("gui.edit_drop.name.presets.legendary", Map.of(CTX_MATERIAL, materialName)),
            tr("gui.edit_drop.name.presets.rare", Map.of(CTX_MATERIAL, materialName)),
            tr("gui.edit_drop.name.presets.uncommon", Map.of(CTX_MATERIAL, materialName)),
            tr("gui.edit_drop.name.presets.common", Map.of(CTX_MATERIAL, materialName)),
            tr("gui.edit_drop.name.presets.power", Map.of(CTX_MATERIAL, materialName))
        ));

        String displayName = currentName != null ? currentName : tr("gui.edit_drop.name.none");

        setItem(22,
                ItemBuilder.of(Material.NAME_TAG)
                        .name(tr("gui.edit_drop.name.name"))
                        .addLore(tr("gui.edit_drop.name.current", Map.of("name", displayName)))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.name.left_click_custom"))
                        .addLore(tr("gui.edit_drop.name.right_click_preset"))
                        .addLore(tr("gui.edit_drop.name.shift_right_clear"))
                        .build(),
                event -> {
                    playClickSound();
                    if (event.getClick() == ClickType.LEFT) {
                        requestNameInput();
                        return;
                    }
                    if (event.getClick() == ClickType.RIGHT) {
                        cycleNamePreset(presets);
                        refresh();
                        return;
                    }
                    if (event.getClick() == ClickType.SHIFT_RIGHT) {
                        currentName = null;
                        sendMessage(tr("gui.edit_drop.messages.name-cleared"));
                        refresh();
                    }
                });
    }

    private void buildLoreEditor() {
        setItem(23,
                ItemBuilder.of(Material.BOOK)
                        .name(tr("gui.edit_drop.lore.name"))
                        .addLore(tr("gui.edit_drop.lore.lines", Map.of("count", String.valueOf(currentLore.size()))))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.lore.left_click_custom"))
                        .addLore(tr("gui.edit_drop.lore.right_click_remove"))
                        .addLore(tr("gui.edit_drop.lore.shift_left_replace"))
                        .addLore(tr("gui.edit_drop.lore.shift_right_clear"))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.lore.max"))
                        .build(),
                event -> {
                    playClickSound();
                    switch (event.getClick()) {
                        case LEFT -> requestLoreInput(false);
                        case RIGHT -> {
                            removeLastLoreLine();
                            refresh();
                        }
                        case SHIFT_LEFT -> requestLoreInput(true);
                        case SHIFT_RIGHT -> {
                            currentLore.clear();
                            sendMessage(tr("gui.edit_drop.messages.lore-cleared"));
                            refresh();
                        }
                        default -> {
                        }
                    }
                });
    }

    private void buildGlowToggle() {
        Material mat  = currentGlow ? Material.GLOWSTONE_DUST : Material.GUNPOWDER;
        String   name = currentGlow
            ? tr("gui.edit_drop.glow.enabled_name")
            : tr("gui.edit_drop.glow.disabled_name");

        setItem(24,
                ItemBuilder.of(mat)
                        .name(name)
                        .addLore(tr("gui.edit_drop.glow.lore1"))
                        .addLore(tr("gui.edit_drop.glow.lore2"))
                        .build(),
                () -> {
                    playClickSound();
                    currentGlow = !currentGlow;
                    refresh();
                });
    }

    private void buildPermissionEditor() {
        setItem(28,
                ItemBuilder.of(Material.TRIPWIRE_HOOK)
                        .name(tr("gui.edit_drop.permission.name"))
                        .addLore(tr("gui.edit_drop.permission.current",
                            Map.of(CTX_PERMISSION, currentPermissionText())))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.permission.left_click"))
                        .addLore(tr("gui.edit_drop.permission.right_click"))
                        .build(),
                event -> {
                    playClickSound();
                    if (event.getClick() == ClickType.LEFT) {
                        requestPermissionInput();
                        return;
                    }
                    if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                        currentPermission = null;
                        sendMessage(tr("gui.edit_drop.messages.permission-cleared"));
                        refresh();
                    }
                });
    }

    private void buildBiomeEditor() {
        setItem(29,
                ItemBuilder.of(Material.GRASS_BLOCK)
                        .name(tr("gui.edit_drop.biomes.name"))
                        .addLore(tr("gui.edit_drop.biomes.current", Map.of(CTX_BIOMES, currentBiomesText())))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.biomes.left_click"))
                        .addLore(tr("gui.edit_drop.biomes.right_click"))
                        .addLore(tr("gui.edit_drop.biomes.max",
                            Map.of("max", String.valueOf(MAX_BIOME_ENTRIES))))
                        .build(),
                event -> {
                    playClickSound();
                    if (event.getClick() == ClickType.LEFT) {
                        requestBiomesInput();
                        return;
                    }
                    if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                        currentBiomes.clear();
                        sendMessage(tr("gui.edit_drop.messages.biomes-cleared"));
                        refresh();
                    }
                });
    }

    private void buildModelDataEditor() {
        setItem(30,
                ItemBuilder.of(Material.ITEM_FRAME)
                        .name(tr("gui.edit_drop.model_data.name"))
                        .addLore(tr("gui.edit_drop.model_data.current",
                            Map.of(CTX_VALUE, String.valueOf(currentCustomModelData))))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.model_data.left_click"))
                        .addLore(tr("gui.edit_drop.model_data.right_click"))
                        .addLore(tr("gui.edit_drop.model_data.range",
                            Map.of("max", String.valueOf(MAX_CUSTOM_MODEL_DATA))))
                        .build(),
                event -> {
                    playClickSound();
                    if (event.getClick() == ClickType.LEFT) {
                        requestModelDataInput();
                        return;
                    }
                    if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                        currentCustomModelData = 0;
                        sendMessage(tr("gui.edit_drop.messages.model-data-cleared"));
                        refresh();
                    }
                });
    }

    private void buildEnchantmentsEditor() {
        setItem(31,
                ItemBuilder.of(Material.ENCHANTED_BOOK)
                        .name(tr("gui.edit_drop.enchantments.name"))
                        .addLore(tr("gui.edit_drop.enchantments.current",
                            Map.of("enchantments", currentEnchantmentsText())))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.enchantments.left_click"))
                        .addLore(tr("gui.edit_drop.enchantments.right_click"))
                        .addLore(tr("gui.edit_drop.enchantments.max",
                            Map.of("max", String.valueOf(MAX_ENCHANTMENT_ENTRIES))))
                        .build(),
                event -> {
                    playClickSound();
                    if (event.getClick() == ClickType.LEFT) {
                        requestEnchantmentsInput();
                        return;
                    }
                    if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                        currentEnchantments.clear();
                        sendMessage(tr("gui.edit_drop.messages.enchantments-cleared"));
                        refresh();
                    }
                });
    }

    private void buildItemFlagsEditor() {
        setItem(32,
                ItemBuilder.of(Material.LIGHT_GRAY_BANNER)
                        .name(tr("gui.edit_drop.item_flags.name"))
                        .addLore(tr("gui.edit_drop.item_flags.current", Map.of("flags", currentItemFlagsText())))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.item_flags.left_click"))
                        .addLore(tr("gui.edit_drop.item_flags.right_click"))
                        .addLore(tr("gui.edit_drop.item_flags.max",
                            Map.of("max", String.valueOf(MAX_ITEM_FLAG_ENTRIES))))
                        .build(),
                event -> {
                    playClickSound();
                    if (event.getClick() == ClickType.LEFT) {
                        requestItemFlagsInput();
                        return;
                    }
                    if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                        currentItemFlags.clear();
                        sendMessage(tr("gui.edit_drop.messages.item-flags-cleared"));
                        refresh();
                    }
                });
    }

    private void buildSaveButton() {
        if (actionInProgress) {
            setItem(38,
                    ItemBuilder.of(Material.GRAY_CONCRETE)
                            .name(tr("gui.edit_drop.save.working_name"))
                            .addLore(tr("gui.edit_drop.save.working_lore"))
                            .build(),
                    () -> {
                        sendMessage(tr(TR_ACTION_BUSY));
                        playErrorSound();
                    });
            return;
        }

        setItem(38,
                ItemBuilder.of(Material.LIME_CONCRETE)
                        .name(tr("gui.edit_drop.save.name"))
                        .addLore(tr("gui.edit_drop.save.lore1"))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.save.lore2"))
                        .addLore(tr("gui.edit_drop.save.item", Map.of(CTX_IDENTIFIER, currentIdentifier)))
                        .addLore(tr("gui.edit_drop.save.weight", Map.of(CTX_WEIGHT, String.valueOf(currentWeight))))
                        .addLore(tr("gui.edit_drop.save.amount", Map.of("amount", String.valueOf(currentAmount))))
                        .addLore(tr("gui.edit_drop.save.glow",
                                    Map.of("status", currentGlow
                                        ? tr("gui.edit_drop.status_yes")
                                        : tr("gui.edit_drop.status_no"))))
                        .addLore(tr("gui.edit_drop.save.model_data",
                            Map.of(CTX_VALUE, String.valueOf(currentCustomModelData))))
                        .addLore(tr("gui.edit_drop.save.permission",
                            Map.of(CTX_PERMISSION, currentPermissionText())))
                        .glow()
                        .build(),
                () -> {
                    if (!beginEditorAction()) {
                        return;
                    }
                    playClickSound();
                    refresh();
                    saveChanges();
                });
    }

    private void buildResetButton() {
        setItem(39,
                ItemBuilder.of(Material.ORANGE_CONCRETE)
                        .name(tr("gui.edit_drop.reset.name"))
                        .addLore(tr("gui.edit_drop.reset.lore1"))
                        .addLore(tr("gui.edit_drop.reset.lore2"))
                        .build(),
                () -> {
                    playClickSound();
                    currentWeight = drop.getWeight();
                    currentAmount = drop.getAmount();
                    currentIdentifier = drop.getIdentifier();
                    currentName   = drop.getCustomName();
                    currentLore   = new ArrayList<>(drop.getLore());
                    currentCustomModelData = drop.getCustomModelData();
                    currentPermission = drop.getPermission();
                    currentBiomes = new ArrayList<>(drop.getBiomes());
                    currentEnchantments = new LinkedHashMap<>(drop.getEnchantments());
                    currentItemFlags = new ArrayList<>(drop.getItemFlags());
                    currentGlow   = drop.isGlowing();
                    sendMessage(tr("gui.edit_drop.messages.reset"));
                    refresh();
                });
    }

    private void buildDeleteButton() {
        if (actionInProgress) {
            setItem(41,
                    ItemBuilder.of(Material.GRAY_CONCRETE)
                            .name(tr("gui.edit_drop.delete.working_name"))
                            .addLore(tr("gui.edit_drop.delete.working_lore"))
                            .build(),
                    () -> {
                        sendMessage(tr(TR_ACTION_BUSY));
                        playErrorSound();
                    });
            return;
        }

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
                        if (!beginEditorAction()) {
                            return;
                        }
                        playClickSound();
                        refresh();
                        deleteDrop();
                    } else {
                        sendMessage(tr("gui.edit_drop.messages.delete-confirm"));
                        playErrorSound();
                    }
                });
    }

    private boolean beginEditorAction() {
        if (actionInProgress) {
            sendMessage(tr(TR_ACTION_BUSY));
            playErrorSound();
            return false;
        }

        if (!requirePermission()) {
            return false;
        }

        actionInProgress = true;
        return true;
    }

    private void finishEditorActionAfterFailure() {
        actionInProgress = false;
        refresh();
    }

    private void buildBackButton() {
        setItem(45,
                ItemBuilder.of(Material.ARROW)
                        .name(tr("gui.edit_drop.back.name"))
                        .addLore(tr("gui.edit_drop.back.lore"))
                        .build(),
                () -> {
                    playClickSound();
                    plugin.getGUIManager().openMenu(getPlayer(), DROPS_VIEW, buildDropsMenuContext(category));
                });
    }

    private void buildInfoPanel() {
        setItem(49,
                ItemBuilder.of(Material.KNOWLEDGE_BOOK)
                        .name(tr("gui.edit_drop.info.name"))
                        .addLore(tr("gui.edit_drop.info.lore1",
                                    Map.of(CTX_IDENTIFIER, currentIdentifier)))
                        .addLore(tr("gui.edit_drop.info.lore2",
                                    Map.of(CTX_MATERIAL, currentMaterialName())))
                        .addLore("")
                        .addLore(tr("gui.edit_drop.info.lore3"))
                        .addLore(tr("gui.edit_drop.info.lore4"))
                        .build());
    }

    private void requestItemInput() {
        requestTextInput(tr("gui.edit_drop.input.item-prompt"), this::handleItemInput);
    }

    private void requestWeightInput() {
        requestTextInput(tr("gui.edit_drop.input.weight-prompt"), this::handleWeightInput);
    }

    private void requestAmountInput() {
        requestTextInput(tr("gui.edit_drop.input.amount-prompt"), this::handleAmountInput);
    }

    private void requestNameInput() {
        requestTextInput(tr("gui.edit_drop.input.name-prompt"), this::handleNameInput);
    }

    private void requestLoreInput(boolean replaceExisting) {
        requestTextInput(
            replaceExisting
                ? tr("gui.edit_drop.input.lore-replace-prompt")
                : tr("gui.edit_drop.input.lore-add-prompt"),
            input -> handleLoreInput(input, replaceExisting)
        );
    }

    private void requestModelDataInput() {
        requestTextInput(tr("gui.edit_drop.input.model-data-prompt"), this::handleModelDataInput);
    }

    private void requestPermissionInput() {
        requestTextInput(tr("gui.edit_drop.input.permission-prompt"), this::handlePermissionInput);
    }

    private void requestBiomesInput() {
        requestTextInput(tr("gui.edit_drop.input.biomes-prompt"), this::handleBiomesInput);
    }

    private void requestEnchantmentsInput() {
        requestTextInput(tr("gui.edit_drop.input.enchantments-prompt"), this::handleEnchantmentsInput);
    }

    private void requestItemFlagsInput() {
        requestTextInput(tr("gui.edit_drop.input.item-flags-prompt"), this::handleItemFlagsInput);
    }

    private void requestTextInput(String prompt, Consumer<String> handler) {
        Player player = getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!requirePermission()) {
            return;
        }

        boolean opened = plugin.getGUIManager().requestTextInput(
            player,
            prompt,
            tr("gui.edit_drop.input.cancelled"),
            tr("gui.edit_drop.input.expired"),
            input -> {
                if (!requirePermission()) {
                    return;
                }
                handler.accept(input);
            },
            this::open
        );
        if (!opened) {
            playErrorSound();
        }
    }

    private void handleItemInput(String input) {
        String trimmed = normalizeTypedText(input);
        if (trimmed == null || trimmed.length() > MAX_IDENTIFIER_LENGTH) {
            sendMessage(tr("gui.edit_drop.messages.item-invalid", Map.of("input", safeMessageInput(input))));
            playErrorSound();
            open();
            return;
        }

        String normalizedIdentifier = normalizeIdentifierInput(trimmed);
        if (normalizedIdentifier == null) {
            sendMessage(tr("gui.edit_drop.messages.item-invalid", Map.of("input", safeMessageInput(input))));
            playErrorSound();
            open();
            return;
        }

        currentIdentifier = normalizedIdentifier;
        sendMessage(tr("gui.edit_drop.messages.item-updated", Map.of(CTX_IDENTIFIER, currentIdentifier)));
        playSuccessSound();
        open();
    }

    private void handleWeightInput(String input) {
        Integer value = parseIntegerInput(input, 1, 100);
        if (value == null) {
            sendMessage(tr(TR_NUMBER_INVALID,
                Map.of("min", "1", "max", "100")));
            playErrorSound();
            open();
            return;
        }

        currentWeight = value;
        sendMessage(tr("gui.edit_drop.messages.weight-updated",
            Map.of(CTX_WEIGHT, String.valueOf(currentWeight))));
        playSuccessSound();
        open();
    }

    private void handleAmountInput(String input) {
        Integer value = parseIntegerInput(input, 1, 64);
        if (value == null) {
            sendMessage(tr(TR_NUMBER_INVALID,
                Map.of("min", "1", "max", "64")));
            playErrorSound();
            open();
            return;
        }

        currentAmount = value;
        sendMessage(tr("gui.edit_drop.messages.amount-updated",
            Map.of("amount", String.valueOf(currentAmount))));
        playSuccessSound();
        open();
    }

    private void handleNameInput(String input) {
        String value = normalizeTypedText(input);
        if (value == null || isClearInput(value)) {
            currentName = null;
            sendMessage(tr("gui.edit_drop.messages.name-cleared"));
            playClickSound();
            open();
            return;
        }

        if (value.length() > MAX_NAME_LENGTH) {
            sendMessage(tr("gui.edit_drop.messages.text-too-long", Map.of("max", String.valueOf(MAX_NAME_LENGTH))));
            playErrorSound();
            open();
            return;
        }

        currentName = value;
        sendMessage(tr("gui.edit_drop.messages.name-updated"));
        playSuccessSound();
        open();
    }

    private void handleModelDataInput(String input) {
        String value = normalizeTypedText(input);
        if (value == null || isClearInput(value)) {
            currentCustomModelData = 0;
            sendMessage(tr("gui.edit_drop.messages.model-data-cleared"));
            playClickSound();
            open();
            return;
        }

        Integer parsed = parseIntegerInput(value, 0, MAX_CUSTOM_MODEL_DATA);
        if (parsed == null) {
            sendMessage(tr(TR_NUMBER_INVALID,
                Map.of("min", "0", "max", String.valueOf(MAX_CUSTOM_MODEL_DATA))));
            playErrorSound();
            open();
            return;
        }

        currentCustomModelData = parsed;
        sendMessage(tr("gui.edit_drop.messages.model-data-updated",
            Map.of(CTX_VALUE, String.valueOf(currentCustomModelData))));
        playSuccessSound();
        open();
    }

    private void handlePermissionInput(String input) {
        String value = normalizeTypedText(input);
        if (value == null || isClearInput(value)) {
            currentPermission = null;
            sendMessage(tr("gui.edit_drop.messages.permission-cleared"));
            playClickSound();
            open();
            return;
        }

        if (value.length() > MAX_PERMISSION_LENGTH || !PERMISSION_NODE_PATTERN.matcher(value).matches()) {
            sendMessage(tr("gui.edit_drop.messages.permission-invalid"));
            playErrorSound();
            open();
            return;
        }

        currentPermission = value;
        sendMessage(tr("gui.edit_drop.messages.permission-updated",
            Map.of(CTX_PERMISSION, currentPermission)));
        playSuccessSound();
        open();
    }

    private void handleBiomesInput(String input) {
        String value = normalizeTypedText(input);
        if (value == null || isClearInput(value)) {
            currentBiomes.clear();
            sendMessage(tr("gui.edit_drop.messages.biomes-cleared"));
            playClickSound();
            open();
            return;
        }

        List<String> parsed = parseBiomeList(value);
        if (parsed == null) {
            playErrorSound();
            open();
            return;
        }

        currentBiomes = parsed;
        sendMessage(tr("gui.edit_drop.messages.biomes-updated",
            Map.of(CTX_BIOMES, formatList(currentBiomes))));
        playSuccessSound();
        open();
    }

    private void handleEnchantmentsInput(String input) {
        String value = normalizeTypedText(input);
        if (value == null || isClearInput(value)) {
            currentEnchantments.clear();
            sendMessage(tr("gui.edit_drop.messages.enchantments-cleared"));
            playClickSound();
            open();
            return;
        }

        Map<String, Integer> parsed = parseEnchantments(value);
        if (parsed == null) {
            playErrorSound();
            open();
            return;
        }

        currentEnchantments = parsed;
        sendMessage(tr("gui.edit_drop.messages.enchantments-updated",
            Map.of("enchantments", currentEnchantmentsText())));
        playSuccessSound();
        open();
    }

    private void handleItemFlagsInput(String input) {
        String value = normalizeTypedText(input);
        if (value == null || isClearInput(value)) {
            currentItemFlags.clear();
            sendMessage(tr("gui.edit_drop.messages.item-flags-cleared"));
            playClickSound();
            open();
            return;
        }

        List<String> parsed = parseItemFlags(value);
        if (parsed == null) {
            playErrorSound();
            open();
            return;
        }

        currentItemFlags = parsed;
        sendMessage(tr("gui.edit_drop.messages.item-flags-updated",
            Map.of("flags", currentItemFlagsText())));
        playSuccessSound();
        open();
    }

    private void handleLoreInput(String input, boolean replaceExisting) {
        String value = normalizeTypedText(input);
        if (value == null) {
            sendMessage(tr("gui.edit_drop.messages.lore-empty"));
            playErrorSound();
            open();
            return;
        }

        if (value.length() > MAX_LORE_LINE_LENGTH) {
            sendMessage(tr("gui.edit_drop.messages.text-too-long", Map.of("max", String.valueOf(MAX_LORE_LINE_LENGTH))));
            playErrorSound();
            open();
            return;
        }

        if (replaceExisting) {
            currentLore.clear();
        } else if (currentLore.size() >= MAX_LORE_LINES) {
            sendMessage(tr("gui.edit_drop.messages.lore-max"));
            playErrorSound();
            open();
            return;
        }

        currentLore.add(value);
        sendMessage(replaceExisting
            ? tr("gui.edit_drop.messages.lore-replaced")
            : tr("gui.edit_drop.messages.lore-added"));
        playSuccessSound();
        open();
    }

    private void removeLastLoreLine() {
        if (!currentLore.isEmpty()) {
            currentLore.remove(currentLore.size() - 1);
            sendMessage(tr("gui.edit_drop.messages.lore-removed"));
            return;
        }

        sendMessage(tr("gui.edit_drop.messages.lore-empty"));
        playErrorSound();
    }

    private void cycleNamePreset(List<String> presets) {
        int idx = -1;
        for (int i = 0; i < presets.size(); i++) {
            if (Objects.equals(currentName, presets.get(i))) {
                idx = i;
                break;
            }
        }
        currentName = presets.get((idx + 1) % presets.size());
    }

    private String normalizeIdentifierInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim();
        if (trimmed.regionMatches(true, 0, NEXO_PREFIX, 0, 5)) {
            String nexoId = trimmed.substring(5).trim();
            if (nexoId.isEmpty()) {
                return null;
            }
            String nexoIdentifier = NEXO_PREFIX + nexoId;
            if (plugin.getPlatformServer() == null
                    || plugin.getPlatformServer().getItemFactory() == null
                    || !plugin.getPlatformServer().getItemFactory().canCreate(nexoIdentifier)) {
                return null;
            }
            return nexoIdentifier;
        }

        Material material = materialFromIdentifier(trimmed);
        if (material == null) {
            return null;
        }

        return material.name();
    }

    private Material materialFromIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()
                || identifier.regionMatches(true, 0, NEXO_PREFIX, 0, 5)) {
            return null;
        }

        String trimmed = identifier.trim();
        Material material = Material.matchMaterial(trimmed);
        if (material == null && trimmed.regionMatches(true, 0, MINECRAFT_PREFIX, 0, MINECRAFT_PREFIX.length())) {
            material = Material.matchMaterial(trimmed.substring(MINECRAFT_PREFIX.length()));
        }
        if (material == null && !trimmed.contains(":")) {
            material = Material.matchMaterial(MINECRAFT_PREFIX + trimmed.toLowerCase(Locale.ROOT));
        }

        if (material == null || material.isAir() || !material.isItem()) {
            return null;
        }
        return material;
    }

    private String currentMaterialName() {
        if (currentIdentifier != null && currentIdentifier.regionMatches(true, 0, NEXO_PREFIX, 0, 5)) {
            return "Nexo: " + currentIdentifier.substring(5);
        }
        return StringFormatting.formatMaterialName(currentIdentifier);
    }

    private Integer parseIntegerInput(String input, int min, int max) {
        String value = normalizeTypedText(input);
        if (value == null) {
            return null;
        }

        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<String> parseBiomeList(String input) {
        String[] parts = input.split("[,;]");
        Set<String> parsed = new LinkedHashSet<>();
        Registry<Biome> biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);

        for (String part : parts) {
            String key = normalizeBiomeKey(part);
            if (key == null) {
                continue;
            }
            if (parsed.size() >= MAX_BIOME_ENTRIES) {
                sendMessage(tr("gui.edit_drop.messages.biomes-too-many",
                    Map.of("max", String.valueOf(MAX_BIOME_ENTRIES))));
                return null;
            }

            NamespacedKey namespacedKey = NamespacedKey.fromString(key);
            if (namespacedKey == null || biomeRegistry.get(namespacedKey) == null) {
                sendMessage(tr("gui.edit_drop.messages.biome-invalid", Map.of("biome", safeMessageInput(part))));
                return null;
            }
            parsed.add(namespacedKey.asString());
        }

        if (parsed.isEmpty()) {
            sendMessage(tr("gui.edit_drop.messages.biomes-empty"));
            return null;
        }
        return new ArrayList<>(parsed);
    }

    private String normalizeBiomeKey(String input) {
        String value = normalizeTypedText(input);
        if (value == null) {
            return null;
        }

        String normalized = value.toLowerCase(Locale.ROOT)
            .replace(' ', '_')
            .replace('-', '_');
        if (normalized.startsWith("biome_")) {
            normalized = normalized.substring("biome_".length());
        }
        return normalized.contains(":") ? normalized : MINECRAFT_PREFIX + normalized;
    }

    private Map<String, Integer> parseEnchantments(String input) {
        String[] parts = input.split("[,;]");
        Map<String, Integer> parsed = new LinkedHashMap<>();
        Registry<Enchantment> enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

        for (String part : parts) {
            String entry = normalizeTypedText(part);
            if (entry == null) {
                continue;
            }
            if (parsed.size() >= MAX_ENCHANTMENT_ENTRIES) {
                sendMessage(tr("gui.edit_drop.messages.enchantments-too-many",
                    Map.of("max", String.valueOf(MAX_ENCHANTMENT_ENTRIES))));
                return null;
            }

            ParsedPair parsedPair = parseNameAndLevel(entry);
            if (parsedPair == null) {
                sendMessage(tr("gui.edit_drop.messages.enchantment-invalid",
                    Map.of("enchantment", safeMessageInput(part))));
                return null;
            }

            NamespacedKey key = normalizeMinecraftKey(parsedPair.name());
            if (key == null || enchantmentRegistry.get(key) == null) {
                sendMessage(tr("gui.edit_drop.messages.enchantment-invalid",
                    Map.of("enchantment", safeMessageInput(part))));
                return null;
            }
            parsed.put(key.asString(), parsedPair.level());
        }

        if (parsed.isEmpty()) {
            sendMessage(tr("gui.edit_drop.messages.enchantments-empty"));
            return null;
        }
        return parsed;
    }

    private ParsedPair parseNameAndLevel(String entry) {
        int separator = Math.max(entry.lastIndexOf('='), entry.lastIndexOf(' '));
        int lastColon = entry.lastIndexOf(':');
        if (lastColon >= 0
                && (entry.indexOf(':') != lastColon || isIntegerText(entry.substring(lastColon + 1)))) {
            separator = Math.max(separator, lastColon);
        }

        String name = separator < 0 ? entry : entry.substring(0, separator).trim();
        String levelText = separator < 0 ? "1" : entry.substring(separator + 1).trim();
        if (name.isEmpty() || levelText.isEmpty()) {
            return null;
        }

        try {
            int level = Integer.parseInt(levelText);
            if (level < 1 || level > MAX_ENCHANTMENT_LEVEL) {
                return null;
            }
            return new ParsedPair(name, level);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isIntegerText(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        for (int i = 0; i < input.length(); i++) {
            if (!Character.isDigit(input.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private NamespacedKey normalizeMinecraftKey(String input) {
        String value = normalizeTypedText(input);
        if (value == null) {
            return null;
        }

        String normalized = value.toLowerCase(Locale.ROOT).replace(' ', '_');
        if (!normalized.contains(":")) {
            normalized = MINECRAFT_PREFIX + normalized;
        }
        return NamespacedKey.fromString(normalized);
    }

    private List<String> parseItemFlags(String input) {
        String[] parts = input.split("[,;]");
        Set<String> parsed = new LinkedHashSet<>();

        for (String part : parts) {
            String value = normalizeTypedText(part);
            if (value == null) {
                continue;
            }
            if (parsed.size() >= MAX_ITEM_FLAG_ENTRIES) {
                sendMessage(tr("gui.edit_drop.messages.item-flags-too-many",
                    Map.of("max", String.valueOf(MAX_ITEM_FLAG_ENTRIES))));
                return null;
            }

            try {
                ItemFlag flag = ItemFlag.valueOf(value.toUpperCase(Locale.ROOT));
                parsed.add(flag.name());
            } catch (IllegalArgumentException e) {
                sendMessage(tr("gui.edit_drop.messages.item-flag-invalid",
                    Map.of("flag", safeMessageInput(part))));
                return null;
            }
        }

        if (parsed.isEmpty()) {
            sendMessage(tr("gui.edit_drop.messages.item-flags-empty"));
            return null;
        }
        return new ArrayList<>(parsed);
    }

    private String normalizeTypedText(String input) {
        if (input == null) {
            return null;
        }

        String value = input.replace('\n', ' ').replace('\r', ' ').trim();
        return value.isEmpty() ? null : value;
    }

    private boolean isClearInput(String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("clear") || normalized.equals("none") || normalized.equals("reset");
    }

    private String safeMessageInput(String input) {
        String value = normalizeTypedText(input);
        if (value == null) {
            return "?";
        }
        return value.replace("<", "").replace(">", "");
    }

    private String currentPermissionText() {
        return currentPermission == null || currentPermission.isBlank()
            ? tr("gui.edit_drop.permission.none")
            : currentPermission;
    }

    private String currentBiomesText() {
        return currentBiomes == null || currentBiomes.isEmpty()
            ? tr("gui.edit_drop.biomes.none")
            : formatList(currentBiomes);
    }

    private String currentEnchantmentsText() {
        if (currentEnchantments == null || currentEnchantments.isEmpty()) {
            return tr("gui.edit_drop.enchantments.none");
        }

        List<String> entries = new ArrayList<>(currentEnchantments.size());
        currentEnchantments.forEach((key, level) ->
            entries.add(StringFormatting.formatEnchantName(key) + " " + level));
        return formatList(entries);
    }

    private String currentItemFlagsText() {
        return currentItemFlags == null || currentItemFlags.isEmpty()
            ? tr("gui.edit_drop.item_flags.none")
            : formatList(currentItemFlags);
    }

    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(", ", values);
    }

    private record ParsedPair(String name, int level) {
    }

    private void saveChanges() {
        if (!requirePermission()) {
            finishEditorActionAfterFailure();
            return;
        }

        final CustomDrop  finalDrop     = drop;
        final String      finalCategory = category;
        final String      finalIdentifier = currentIdentifier;
        final int         finalWeight   = currentWeight;
        final int         finalAmount   = currentAmount;
        final String      finalName     = currentName;
        final List<String> finalLore    = List.copyOf(currentLore);
        final int         finalCustomModelData = currentCustomModelData;
        final String      finalPermission = currentPermission;
        final List<String> finalBiomes = List.copyOf(currentBiomes);
        final Map<String, Integer> finalEnchantments = Map.copyOf(currentEnchantments);
        final List<String> finalItemFlags = List.copyOf(currentItemFlags);
        final boolean     finalGlow     = currentGlow;
        final Player      finalPlayer   = getPlayer();
        final String      finalPlayerName = playerName(finalPlayer);
        plugin.getDropManager().beginAsyncPersistenceOperation();

        try {
            plugin.getPlatformScheduler().runAsync(() -> {
                try {
                    boolean updated = plugin.getDropManager().updateDrop(
                            finalDrop, finalCategory,
                            finalIdentifier, finalWeight, finalAmount, finalName, finalLore,
                            finalCustomModelData, finalEnchantments, finalItemFlags,
                            finalGlow, finalPermission, finalBiomes);
                    if (!updated) {
                        plugin.getLogger().info(() -> EDITOR_LOG_PREFIX + finalDrop.getIdentifier()
                            + IN_CATEGORY_FRAGMENT + finalCategory + "' was already changed before "
                            + finalPlayerName + " could save it");
                        runForPlayerIfOnline(finalPlayer, () -> {
                            finishEditorActionAfterFailure();
                            playErrorSound();
                            sendMessage(tr("gui.edit_drop.messages.drop-stale"));
                        });
                        return;
                    }
                    plugin.getDropManager().saveDropsConfig();
                    plugin.getLogger().info(() -> EDITOR_LOG_PREFIX + finalDrop.getIdentifier()
                        + "' -> '" + finalIdentifier + IN_CATEGORY_FRAGMENT + finalCategory + "' updated by "
                        + finalPlayerName);

                    runForPlayerIfOnline(finalPlayer, () -> {
                        playSuccessSound();
                        sendMessage(tr("gui.edit_drop.messages.save-success"));
                        plugin.getGUIManager().openMenu(finalPlayer, DROPS_VIEW, buildDropsMenuContext(finalCategory));
                        });
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, e,
                        () -> "[EditDropMenu] Failed to save drop '" + finalDrop.getIdentifier()
                            + IN_CATEGORY_FRAGMENT + finalCategory + "'");
                    runForPlayerIfOnline(finalPlayer, () -> {
                        finishEditorActionAfterFailure();
                        playErrorSound();
                        sendMessage(tr("gui.edit_drop.messages.save-failed"));
                    });
                } finally {
                    plugin.getDropManager().endAsyncPersistenceOperation();
                }
            });
        } catch (Exception e) {
            plugin.getDropManager().endAsyncPersistenceOperation();
            actionInProgress = false;
            throw e;
        }
    }

    private void deleteDrop() {
        if (!requirePermission()) {
            finishEditorActionAfterFailure();
            return;
        }

        final CustomDrop finalDrop     = drop;
        final String     finalCategory = category;
        final Player     finalPlayer   = getPlayer();
        final String     finalPlayerName = playerName(finalPlayer);
        plugin.getDropManager().beginAsyncPersistenceOperation();

        try {
            plugin.getPlatformScheduler().runAsync(() -> {
                try {
                    boolean deleted = plugin.getDropManager().deleteDrop(finalDrop, finalCategory);
                    if (!deleted) {
                        plugin.getLogger().info(() -> EDITOR_LOG_PREFIX + finalDrop.getIdentifier()
                            + IN_CATEGORY_FRAGMENT + finalCategory + "' was already deleted before "
                            + finalPlayerName + " confirmed it");
                        runForPlayerIfOnline(finalPlayer, () -> {
                            finishEditorActionAfterFailure();
                            playErrorSound();
                            sendMessage(tr("gui.edit_drop.messages.drop-stale"));
                        });
                        return;
                    }
                    plugin.getDropManager().saveDropsConfig();
                    plugin.getLogger().info(() -> EDITOR_LOG_PREFIX + finalDrop.getIdentifier()
                        + IN_CATEGORY_FRAGMENT + finalCategory + "' deleted by " + finalPlayerName);

                    runForPlayerIfOnline(finalPlayer, () -> {
                        playSuccessSound();
                        sendMessage(tr("gui.edit_drop.messages.delete-success"));
                        plugin.getGUIManager().openMenu(finalPlayer, DROPS_VIEW, buildDropsMenuContext(finalCategory));
                    });
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, e,
                        () -> "[EditDropMenu] Failed to delete drop '" + finalDrop.getIdentifier()
                            + IN_CATEGORY_FRAGMENT + finalCategory + "'");
                    runForPlayerIfOnline(finalPlayer, () -> {
                        finishEditorActionAfterFailure();
                        playErrorSound();
                        sendMessage(tr("gui.edit_drop.messages.delete-failed"));
                    });
                } finally {
                    plugin.getDropManager().endAsyncPersistenceOperation();
                }
            });
        } catch (Exception e) {
            plugin.getDropManager().endAsyncPersistenceOperation();
            actionInProgress = false;
            throw e;
        }
    }

    private void runForPlayerIfOnline(Player player, Runnable task) {
        if (player == null || !player.isOnline()) {
            return;
        }
        plugin.getPlatformScheduler().runForPlayer(new PaperPlayer(player), () -> {
            if (player.isOnline()) {
                task.run();
            }
        });
    }

    private String playerName(Player player) {
        return player != null ? player.getName() : "unknown";
    }

    private Map<String, Object> buildDropsMenuContext(String targetCategory) {
        return Map.of(CTX_CATEGORY, targetCategory, "viewing_category", Boolean.TRUE, CTX_PAGE, returnPage);
    }

    @Override
    public String getRequiredPermission() {
        return ADMIN_PERMISSION;
    }
}
