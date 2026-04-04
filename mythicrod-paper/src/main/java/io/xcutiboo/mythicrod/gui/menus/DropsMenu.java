package io.xcutiboo.mythicrod.gui.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.item.ItemBuilder;

public class DropsMenu extends BaseMenu {
    private boolean viewingCategory = false;
    private String selectedCategory = null;

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
            return tr("gui.drops.category_title", Map.of("%category%", selectedCategory));
        }
        return tr("gui.drops.title");
    }

    @Override
    protected void build() {
        if (viewingCategory && selectedCategory != null) {
            buildCategoryView();
        } else {
            buildCategoryList();
        }
    }

    private void buildCategoryList() {
        Map<String, List<CustomDrop>> categories = plugin.getDropManager().getDropCategories();
        fillBorder(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        // Display each category as a clickable item
        List<String> categoryNames = new ArrayList<>(categories.keySet());
        int displaySlot = 0; // Index within content area
        final int contentStart = 10;
        final int contentPerRow = 7; // Slots 10-16, 19-25, 28-34, 37-43
        for (String category : categoryNames) {
            List<CustomDrop> drops = categories.get(category);
            if (drops == null || drops.isEmpty()) {
                continue;
            }
            // Calculate actual slot position
            int row = displaySlot / contentPerRow;
            int col = displaySlot % contentPerRow;
            int slot = contentStart + (row * 9) + col;
            // Don't go past row 4
            if (row >= 4) {
                break;
            }
            // Choose icon based on category name
            Material icon = getCategoryIcon(category);
            // Calculate total weight
            int totalWeight = drops.stream()
                    .mapToInt(CustomDrop::getChance)
                    .sum();
            ItemStack categoryItem = new ItemBuilder(icon)
                    .name(tr("gui.drops.category_name", Map.of("%category%", formatCategoryName(category))))
                    .lore(
                            tr("gui.drops.category_lore1"),
                            tr("gui.drops.category_lore2"),
                            "",
                            tr("gui.drops.category_count", Map.of("%count%", String.valueOf(drops.size()))),
                            tr("gui.drops.category_weight", Map.of("%weight%", String.valueOf(totalWeight))),
                            "",
                            tr("gui.drops.category_click")
                    )
                    .glow(category.equals("global"))
                    .build();
            setItem(slot, categoryItem, event -> {
                viewingCategory = true;
                selectedCategory = category;
                refresh(); // Refresh with new view
            });
            displaySlot++;
        }
        // Info panel
        ItemStack infoItem = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(tr("gui.drops.info_name"))
                .lore(
                        tr("gui.drops.info_lore1", Map.of("%count%", String.valueOf(categories.size()))),
                        tr("gui.drops.info_lore2", Map.of("%total%", String.valueOf(plugin.getDropManager().getTotalDropCount()))),
                        "",
                        tr("gui.drops.info_lore3"),
                        tr("gui.drops.info_lore4"),
                        "",
                        tr("gui.drops.info_lore5")
                )
                .build();
        setItem(49, infoItem);
        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name(tr("gui.drops.back_name"))
                .build();
        setItem(45, backItem, event -> {
            plugin.getGUIManager().openMainHub(getPlayer());
        });
        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("<red><bold>Close")
                .build();
        setItem(53, closeItem, event -> getPlayer().closeInventory());
    }

    private void buildCategoryView() {
        List<CustomDrop> drops = plugin.getDropManager().getDropCategories().get(selectedCategory);
        if (drops == null || drops.isEmpty()) {
            viewingCategory = false;
            selectedCategory = null;
            buildCategoryList(); // Build category list directly to avoid recursive issues
            return;
        }
        fillBorder(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        // Display each drop
        int displaySlot = 0; // Index within content area
        final int contentStart = 10;
        final int contentPerRow = 7; // Slots 10-16, 19-25, 28-34, 37-43
        for (CustomDrop drop : drops) {
            // Calculate actual slot position
            int row = displaySlot / contentPerRow;
            int col = displaySlot % contentPerRow;
            int slot = contentStart + (row * 9) + col;
            // Don't go past row 4
            if (row >= 4) {
                break;
            }
            List<String> lore = new ArrayList<>();
            lore.add(tr("gui.drops.material_label", Map.of("%material%", formatMaterialName(drop.getIdentifier()))));
            lore.add(tr("gui.drops.amount_label", Map.of("%amount%", String.valueOf(drop.getAmount()))));
            lore.add(tr("gui.drops.weight_label", Map.of("%weight%", String.valueOf(drop.getChance()))));
            lore.add("");
            // Add custom name if present
            if (drop.getCustomName() != null) {
                lore.add("<gray>Custom Name: <white>" + drop.getCustomName());
            }
            // Add biome restrictions
            if (!drop.getBiomes().isEmpty()) {
                lore.add("<gray>Biomes: <white>" + String.join(", ", drop.getBiomes()));
            }
            // Add permission requirement
            if (drop.getPermission() != null) {
                lore.add("<gray>Permission: <white>" + drop.getPermission());
            }
            // Add enchantments
            if (!drop.getEnchantments().isEmpty()) {
                lore.add("");
                lore.add("<gray>Enchantments:");
                drop.getEnchantments().forEach((enchant, level) -> {
                    lore.add("  <dark_gray>• <white>" + formatEnchantName(enchant) + " " + level);
                });
            }
            // Add lore from drop
            if (drop.getLore() != null && !drop.getLore().isEmpty()) {
                lore.add("");
                lore.add("<gray>Custom Lore:");
                drop.getLore().forEach(line -> lore.add("  <dark_gray>• <gray>" + line));
            }
            Material material = Material.matchMaterial(drop.getIdentifier());
            if (material == null) material = Material.PAPER;
            String displayName = drop.getCustomName() != null 
                ? drop.getCustomName() 
                : formatMaterialName(drop.getIdentifier());
            ItemStack dropItem = new ItemBuilder(material)
                    .name(tr("gui.drops.drop_name", Map.of("%name%", displayName)))
                    .lore(lore)
                    .glow(drop.isGlowing())
                    .build();
            setItem(slot, dropItem);
            displaySlot++;
        }
        // Category info
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name("<gold><bold>" + formatCategoryName(selectedCategory))
                .lore(
                        "<gray>Total Drops: <white>" + drops.size(),
                        "",
                        "<gray>This category contains fishing",
                        "<gray>drops available to players."
                )
                .build();
        setItem(49, infoItem);
        // Back to categories
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("<yellow>← Back to Categories")
                .build();
        setItem(45, backItem, event -> {
            viewingCategory = false;
            selectedCategory = null;
            refresh(); // Refresh with category list
        });
        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("<red><bold>Close")
                .build();
        setItem(53, closeItem, event -> getPlayer().closeInventory());
    }

    /**
     * DRY: Removed local fillBorder() - now using BaseMenu.fillBorder(Material).
     */

    private Material getCategoryIcon(String category) {
        if (category == null) return Material.BUCKET;
        return switch (category.toLowerCase(java.util.Locale.ROOT)) {
            case "global" -> Material.FISHING_ROD;
            case "rare" -> Material.DIAMOND;
            case "common" -> Material.COD;
            case "treasure" -> Material.CHEST;
            default -> {
                if (category.startsWith("biome_")) {
                    yield Material.GRASS_BLOCK;
                }
                yield Material.BUCKET;
            }
        };
    }

    private String formatCategoryName(String category) {
        if (category == null || category.isEmpty()) return "Unknown";
        if (category.startsWith("biome_")) {
            String biome = category.substring(6);
            return biome.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + biome.substring(1).toLowerCase(java.util.Locale.ROOT) + " Biome";
        }
        return category.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + category.substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    private String formatEnchantName(String key) {
        if (key == null || key.isEmpty()) return "";
        String[] parts = key.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part.substring(0, 1).toUpperCase(java.util.Locale.ROOT))
                    .append(part.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return result.toString();
    }

    private String formatMaterialName(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "Unknown";
        }
        // Handle namespace prefixes like "minecraft:"
        String cleanId = identifier;
        if (identifier.contains(":")) {
            cleanId = identifier.substring(identifier.indexOf(":") + 1);
        }
        // Convert SNAKE_CASE to Title Case
        String[] parts = cleanId.split("[_\\s]");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part.substring(0, 1).toUpperCase(java.util.Locale.ROOT))
                  .append(part.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return result.toString();
    }
}
