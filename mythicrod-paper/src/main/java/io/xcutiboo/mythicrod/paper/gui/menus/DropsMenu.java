package io.xcutiboo.mythicrod.paper.gui.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.constants.PermissionNodes;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;
import io.xcutiboo.mythicrod.paper.platform.PaperPlayer;
import io.xcutiboo.mythicrod.paper.util.StringFormatting;

public class DropsMenu extends BaseMenu {
    private static final int MAX_IDENTIFIER_LENGTH = 96;
    private static final int DEFAULT_NEW_DROP_WEIGHT = 10;
    private static final int DEFAULT_NEW_DROP_AMOUNT = 1;
    private static final String CTX_CATEGORY = "category";
    private static final String CTX_COUNT = "count";
    private static final String CTX_DROP = "drop";
    private static final String CTX_PAGE = "page";
    private static final String CATEGORY_GLOBAL = "global";
    private static final String NEXO_PREFIX = "nexo:";
    private static final String MINECRAFT_PREFIX = "minecraft:";
    private static final String BIOME_PREFIX = "biome_";
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private boolean viewingCategory = false;
    private String selectedCategory = null;
    private int page = 0;
    private boolean actionInProgress = false;

    public DropsMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int getSize() {
        return 54;
    }

    @Override
    protected String getTitle() {
        if (viewingCategory && selectedCategory != null) {
            return tr("gui.drops.category_title", Map.of(CTX_CATEGORY, StringFormatting.formatCategoryName(selectedCategory)));
        }
        return tr("gui.drops.title");
    }

    @Override
    protected void build() {
        applyViewContext();
        if (viewingCategory && selectedCategory != null) {
            buildCategoryView();
        } else {
            buildCategoryList();
        }
    }

    private void applyViewContext() {
        Boolean requestedCategoryView = getContext("viewing_category", Boolean.class);
        String requestedCategory = getContext("category", String.class);
        Integer requestedPage = getContext(CTX_PAGE, Integer.class);
        if (requestedPage != null) {
            page = Math.max(0, requestedPage);
        }

        if (Boolean.TRUE.equals(requestedCategoryView) && requestedCategory != null && !requestedCategory.isBlank()) {
            viewingCategory = true;
            selectedCategory = requestedCategory;
            return;
        }

        if (Boolean.FALSE.equals(requestedCategoryView)) {
            viewingCategory = false;
            selectedCategory = null;
            page = 0;
        }
    }

    private void buildCategoryList() {
        Map<String, List<CustomDrop>> categories = plugin.getDropManager().getDropCategories();
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        List<String> categoryNames = categories.keySet().stream()
            .filter(category -> {
                List<CustomDrop> drops = categories.get(category);
                return drops != null && !drops.isEmpty();
            })
            .sorted(this::compareCategoryNames)
            .toList();

        clampPage(categoryNames.size());
        if (categoryNames.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER)
                .name(tr("gui.drops.empty_categories.name"))
                .lore(tr("gui.drops.empty_categories.lore"))
                .build());
        }

        int startIndex = page * CONTENT_SLOTS.length;
        int endIndex = Math.min(categoryNames.size(), startIndex + CONTENT_SLOTS.length);
        for (int index = startIndex; index < endIndex; index++) {
            String category = categoryNames.get(index);
            List<CustomDrop> drops = categories.get(category);
            int slot = CONTENT_SLOTS[index - startIndex];
            Material icon = getCategoryIcon(category);
            int totalWeight = drops.stream()
                    .mapToInt(CustomDrop::getWeight)
                    .sum();
            ItemStack categoryItem = new ItemBuilder(icon)
                    .name(tr("gui.drops.category_name", Map.of(CTX_CATEGORY, StringFormatting.formatCategoryName(category))))
                    .lore(
                            tr("gui.drops.category_lore1"),
                            tr("gui.drops.category_lore2"),
                            "",
                            tr("gui.drops.category_count", Map.of(CTX_COUNT, String.valueOf(drops.size()))),
                            tr("gui.drops.category_weight", Map.of("weight", String.valueOf(totalWeight))),
                            "",
                            tr("gui.drops.category_click")
                    )
                    .glow(category.equals(CATEGORY_GLOBAL))
                    .build();
            setItem(slot, categoryItem, () -> {
                playClickSound();
                viewingCategory = true;
                selectedCategory = category;
                page = 0;
                refresh();
            });
        }

        ItemStack infoItem = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(tr("gui.drops.info_name"))
                .lore(
                        tr("gui.drops.info_lore1", Map.of(CTX_COUNT, String.valueOf(categoryNames.size()))),
                        tr("gui.drops.info_lore2", Map.of("total", String.valueOf(plugin.getDropManager().getTotalDropCount()))),
                        "",
                        tr("gui.drops.info_lore3"),
                        tr("gui.drops.info_lore4"),
                        "",
                        tr("gui.drops.info_lore5")
                )
                .build();
        setItem(49, infoItem);
        addPaginationControls(categoryNames.size());

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name(tr("gui.drops.back_name"))
                .build();
        setItem(45, backItem, () -> {
            playClickSound();
            plugin.getGUIManager().openMainHub(getPlayer());
        });

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.drops.close_name"))
                .build();
        setItem(53, closeItem, () -> {
            playClickSound();
            getPlayer().closeInventory();
        });
    }

    private void buildCategoryView() {
        List<CustomDrop> drops = plugin.getDropManager().getDropCategories().get(selectedCategory);
        if (drops == null || drops.isEmpty()) {
            sendMessage(tr("gui.drops.category_missing"));
            viewingCategory = false;
            selectedCategory = null;
            page = 0;
            buildCategoryList();
            return;
        }
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        clampPage(drops.size());

        boolean canEditDrops = false;
        Player currentPlayer = getPlayer();
        if (currentPlayer != null) {
            canEditDrops = currentPlayer.hasPermission(PermissionNodes.ADMIN_CONFIG);
        }

        int startIndex = page * CONTENT_SLOTS.length;
        int endIndex = Math.min(drops.size(), startIndex + CONTENT_SLOTS.length);
        for (int index = startIndex; index < endIndex; index++) {
            CustomDrop drop = drops.get(index);
            int slot = CONTENT_SLOTS[index - startIndex];
            List<String> lore = new ArrayList<>();
            lore.add(tr("gui.drops.material_label", Map.of("material", displayItemName(drop))));
            lore.add(tr("gui.drops.amount_label", Map.of("amount", String.valueOf(drop.getAmount()))));
            lore.add(tr("gui.drops.weight_label", Map.of("weight", String.valueOf(drop.getWeight()))));
            lore.add("");
            if (drop.getCustomName() != null) {
                lore.add(tr("gui.drops.custom_name_label", Map.of("name", drop.getCustomName())));
            }
            if (!drop.getBiomes().isEmpty()) {
                lore.add(tr("gui.drops.biomes_label", Map.of("biomes", String.join(", ", drop.getBiomes()))));
            }
            if (drop.getPermission() != null) {
                lore.add(tr("gui.drops.permission_label", Map.of("permission", drop.getPermission())));
            }
            if (!drop.getEnchantments().isEmpty()) {
                lore.add("");
                lore.add(tr("gui.drops.enchantments_header"));
                drop.getEnchantments().forEach((enchant, level) ->
                    lore.add(tr("gui.drops.enchantment_entry", Map.of(
                            "name", StringFormatting.formatEnchantName(enchant),
                            "level", String.valueOf(level)
                    ))));
            }
            if (drop.getLore() != null && !drop.getLore().isEmpty()) {
                lore.add("");
                lore.add(tr("gui.drops.custom_lore_header"));
                drop.getLore().forEach(line -> lore.add(tr("gui.drops.lore_entry", Map.of("line", line))));
            }
            lore.add("");
            lore.add(canEditDrops ? tr("gui.drops.edit_hint") : tr("gui.drops.view_only_hint"));
            Material material = displayMaterial(drop);
            String displayName = drop.getCustomName() != null
                ? drop.getCustomName()
                : displayItemName(drop);
            ItemStack dropItem = new ItemBuilder(material)
                    .name(tr("gui.drops.drop_name", Map.of("name", displayName)))
                    .lore(lore)
                    .glow(drop.isGlowing())
                    .build();
            final CustomDrop dropFinal = drop;
            setItem(slot, dropItem, () -> {
                Player p = getPlayer();
                if (p != null && p.hasPermission(PermissionNodes.ADMIN_CONFIG)) {
                    playClickSound();
                    plugin.getGUIManager().openMenu(p, "editdrop",
                        Map.of(CTX_DROP, dropFinal, "category", selectedCategory, CTX_PAGE, page));
                } else {
                    playErrorSound();
                    sendMessage(tr("gui.drops.edit_locked"));
                }
            });
        }

        ItemStack infoItem = new ItemBuilder(Material.BOOK)
            .name(tr("gui.drops.category_info_name",
                Map.of(CTX_CATEGORY, StringFormatting.formatCategoryName(selectedCategory))))
            .lore(
                tr("gui.drops.category_info_count", Map.of(CTX_COUNT, String.valueOf(drops.size()))),
                "",
                tr("gui.drops.category_info_lore1"),
                tr("gui.drops.category_info_lore2")
            )
            .build();
        setItem(49, infoItem);
        addPaginationControls(drops.size());
        if (canEditDrops) {
            buildAddDropButton();
        }

        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name(tr("gui.drops.back_categories_name"))
            .build();
        setItem(45, backItem, () -> {
            playClickSound();
            viewingCategory = false;
            selectedCategory = null;
            page = 0;
            refresh();
        });

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
            .name(tr("gui.drops.close_name"))
            .build();
        setItem(53, closeItem, () -> {
            playClickSound();
            getPlayer().closeInventory();
        });
    }

    private void buildAddDropButton() {
        if (actionInProgress) {
            setItem(46, new ItemBuilder(Material.GRAY_CONCRETE)
                .name(tr("gui.drops.add.working_name"))
                .lore(tr("gui.drops.add.working_lore"))
                .build(), () -> {
                    sendMessage(tr("gui.drops.add.busy"));
                    playErrorSound();
                });
            return;
        }

        setItem(46, new ItemBuilder(Material.LIME_DYE)
            .name(tr("gui.drops.add.name"))
            .lore(
                tr("gui.drops.add.lore1"),
                tr("gui.drops.add.lore2"),
                "",
                tr("gui.drops.add.click")
            )
            .build(), () -> {
                if (!requirePermission()) {
                    return;
                }
                playClickSound();
                requestAddDropInput();
            });
    }

    private void requestAddDropInput() {
        Player player = getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!requirePermission()) {
            return;
        }

        boolean opened = plugin.getGUIManager().requestTextInput(
            player,
            tr("gui.drops.add.prompt"),
            tr("gui.drops.add.cancelled"),
            tr("gui.drops.add.expired"),
            input -> {
                if (!requirePermission()) {
                    return;
                }
                handleAddDropInput(input);
            },
            this::open
        );
        if (!opened) {
            playErrorSound();
        }
    }

    private void handleAddDropInput(String input) {
        if (!requirePermission()) {
            return;
        }

        String normalizedIdentifier = normalizeIdentifierInput(input);
        if (normalizedIdentifier == null) {
            sendMessage(tr("gui.drops.add.invalid", Map.of("input", safeMessageInput(input))));
            playErrorSound();
            open();
            return;
        }

        actionInProgress = true;
        Player player = getPlayer();
        String playerName = player != null ? player.getName() : "unknown";
        String category = selectedCategory;
        plugin.getDropManager().beginAsyncPersistenceOperation();

        try {
            plugin.getPlatformScheduler().runAsync(() -> {
                try {
                    CustomDrop addedDrop = plugin.getDropManager().addDrop(
                        category,
                        normalizedIdentifier,
                        DEFAULT_NEW_DROP_WEIGHT,
                        DEFAULT_NEW_DROP_AMOUNT,
                        null,
                        List.of(),
                        0,
                        Map.of(),
                        List.of(),
                        false,
                        null,
                        List.of()
                    );
                    if (addedDrop == null) {
                        runForPlayerIfOnline(player, () -> {
                            actionInProgress = false;
                            sendMessage(tr("gui.drops.add.failed"));
                            playErrorSound();
                            open();
                        });
                        return;
                    }

                    plugin.getDropManager().saveDropsConfig();
                    plugin.getLogger().info(() -> "[DropsMenu] Drop '" + addedDrop.getIdentifier()
                        + "' added to '" + category + "' by " + playerName);
                    runForPlayerIfOnline(player, () -> {
                        actionInProgress = false;
                        sendMessage(tr("gui.drops.add.success",
                            Map.of("identifier", addedDrop.getIdentifier())));
                        playSuccessSound();
                        plugin.getGUIManager().openMenu(player, "editdrop",
                            Map.of(CTX_DROP, addedDrop, "category", category, CTX_PAGE, page));
                    });
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, e,
                        () -> "[DropsMenu] Failed to add drop '" + normalizedIdentifier + "' to '" + category + "'");
                    runForPlayerIfOnline(player, () -> {
                        actionInProgress = false;
                        sendMessage(tr("gui.drops.add.failed"));
                        playErrorSound();
                        open();
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

    private Material displayMaterial(CustomDrop drop) {
        if (drop == null || drop.isNexoItem()) {
            return Material.ITEM_FRAME;
        }

        String identifier = drop.getIdentifier();
        Material material = Material.matchMaterial(identifier);
        if (material == null && identifier != null
                && identifier.regionMatches(true, 0, MINECRAFT_PREFIX, 0, MINECRAFT_PREFIX.length())) {
            material = Material.matchMaterial(identifier.substring(MINECRAFT_PREFIX.length()));
        }
        if (material == null && identifier != null && !identifier.contains(":")) {
            material = Material.matchMaterial(MINECRAFT_PREFIX + identifier.toLowerCase(Locale.ROOT));
        }

        return material != null && material.isItem() && !material.isAir()
            ? material
            : Material.PAPER;
    }

    private String normalizeIdentifierInput(String input) {
        String trimmed = normalizeTypedText(input);
        if (trimmed == null || trimmed.length() > MAX_IDENTIFIER_LENGTH) {
            return null;
        }

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
        return material != null ? material.name() : null;
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

    private String normalizeTypedText(String input) {
        if (input == null) {
            return null;
        }

        String value = input.replace('\n', ' ').replace('\r', ' ').trim();
        return value.isEmpty() ? null : value;
    }

    private String safeMessageInput(String input) {
        String value = normalizeTypedText(input);
        if (value == null) {
            return "?";
        }
        return value.replace("<", "").replace(">", "");
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

    private String displayItemName(CustomDrop drop) {
        if (drop == null) {
            return StringFormatting.formatMaterialName(null);
        }
        if (drop.isNexoItem()) {
            return "Nexo: " + StringFormatting.formatMaterialName(drop.getNexoItemId());
        }
        return StringFormatting.formatMaterialName(drop.getIdentifier());
    }

    private void addPaginationControls(int itemCount) {
        int maxPage = getMaxPage(itemCount);

        ItemStack pageInfo = new ItemBuilder(Material.MAP)
            .name(tr("gui.drops.pagination.info_name"))
            .lore(
                tr("gui.drops.pagination.page_status", Map.of(
                    CTX_PAGE, String.valueOf(page + 1),
                    "pages", String.valueOf(maxPage + 1)
                )),
                tr("gui.drops.pagination.item_status", Map.of(
                    "shown", String.valueOf(getVisibleItemCount(itemCount)),
                    "total", String.valueOf(itemCount)
                ))
            )
            .build();
        setItem(50, pageInfo);

        if (page > 0) {
            ItemStack previous = new ItemBuilder(Material.ARROW)
                .name(tr("gui.drops.pagination.previous_name"))
                .lore(tr("gui.drops.pagination.previous_lore"))
                .build();
            setItem(47, previous, () -> {
                playClickSound();
                page--;
                refresh();
            });
        } else {
            setItem(47, disabledPageItem(tr("gui.drops.pagination.previous_disabled")));
        }

        if (page < maxPage) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                .name(tr("gui.drops.pagination.next_name"))
                .lore(tr("gui.drops.pagination.next_lore"))
                .build();
            setItem(51, next, () -> {
                playClickSound();
                page++;
                refresh();
            });
        } else {
            setItem(51, disabledPageItem(tr("gui.drops.pagination.next_disabled")));
        }
    }

    private ItemStack disabledPageItem(String label) {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(label)
            .build();
    }

    private void clampPage(int itemCount) {
        page = Math.max(0, Math.min(page, getMaxPage(itemCount)));
    }

    private int getMaxPage(int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        return (itemCount - 1) / CONTENT_SLOTS.length;
    }

    private int getVisibleItemCount(int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        int startIndex = page * CONTENT_SLOTS.length;
        return Math.max(0, Math.min(CONTENT_SLOTS.length, itemCount - startIndex));
    }

    private Material getCategoryIcon(String category) {
        if (category == null) return Material.BUCKET;
        String normalizedCategory = category.toLowerCase(Locale.ROOT);
        String biomeName = normalizedCategory.startsWith(BIOME_PREFIX)
            ? normalizedCategory.substring(BIOME_PREFIX.length())
            : normalizedCategory;

        return switch (biomeName) {
            case CATEGORY_GLOBAL -> Material.FISHING_ROD;
            case "rare" -> Material.DIAMOND;
            case "legendary" -> Material.NETHER_STAR;
            case "common" -> Material.COD;
            case "treasure" -> Material.CHEST;
            case "ocean" -> Material.TROPICAL_FISH;
            case "desert" -> Material.CACTUS;
            case "jungle" -> Material.JUNGLE_SAPLING;
            case "mushroom_fields" -> Material.RED_MUSHROOM_BLOCK;
            case "forest" -> Material.OAK_SAPLING;
            case "swamp" -> Material.LILY_PAD;
            case "plains" -> Material.GRASS_BLOCK;
            case "mountains", "stony_peaks", "jagged_peaks" -> Material.STONE;
            case "snowy", "snowy_plains", "ice_spikes" -> Material.SNOW_BLOCK;
            case "nether", "crimson_forest", "warped_forest" -> Material.NETHERRACK;
            default -> normalizedCategory.startsWith(BIOME_PREFIX) ? Material.GRASS_BLOCK : Material.BUCKET;
        };
    }

    private int compareCategoryNames(String left, String right) {
        int rankCompare = Integer.compare(categoryRank(left), categoryRank(right));
        if (rankCompare != 0) {
            return rankCompare;
        }
        return StringFormatting.formatCategoryName(left)
            .compareToIgnoreCase(StringFormatting.formatCategoryName(right));
    }

    private int categoryRank(String category) {
        if (category == null) {
            return 4;
        }

        String normalizedCategory = category.toLowerCase(Locale.ROOT);
        return switch (normalizedCategory) {
            case CATEGORY_GLOBAL -> 0;
            case "rare" -> 1;
            case "legendary" -> 2;
            default -> normalizedCategory.startsWith(BIOME_PREFIX) ? 3 : 4;
        };
    }

}
