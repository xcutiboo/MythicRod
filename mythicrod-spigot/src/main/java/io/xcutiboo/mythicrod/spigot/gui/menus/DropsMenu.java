package io.xcutiboo.mythicrod.spigot.gui.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.spigot.gui.utils.ItemBuilder;

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
            return "&6&lDrops &8⚡ &7" + selectedCategory;
        }
        return "&6&lMythicRod &8⚡ &7Drop Categories";
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
        fillBorder();
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
                    .name("&e&l" + formatCategoryName(category))
                    .lore(
                            "&7This category contains various",
                            "&7fishing drops with different rarities",
                            "",
                            "&7Drops: &f" + drops.size(),
                            "&7Total Weight: &f" + totalWeight,
                            "",
                            "&eClick to view drops"
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
                .name("&6&lDrop Information")
                .lore(
                        "&7Total Categories: &f" + categories.size(),
                        "&7Total Drops: &f" + plugin.getDropManager().getTotalDropCount(),
                        "",
                        "&7Categories organize drops by",
                        "&7type, biome, or permission group.",
                        "",
                        "&7Click a category to view its drops!"
                )
                .build();
        setItem(49, infoItem);
        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("&e← Back to Main Menu")
                .build();
        setItem(45, backItem, event -> {
            Player p = getPlayer();
            if (p != null) plugin.getGUIManager().openMainHub(p);
        });
        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("&c&lClose")
                .build();
        setItem(53, closeItem, event -> {
            Player p = getPlayer();
            if (p != null) p.closeInventory();
        });
    }

    private void buildCategoryView() {
        List<CustomDrop> drops = plugin.getDropManager().getDropCategories().get(selectedCategory);
        if (drops == null || drops.isEmpty()) {
            viewingCategory = false;
            selectedCategory = null;
            buildCategoryList(); // Build category list directly to avoid recursive issues
            return;
        }
        fillBorder();
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
            lore.add("&7Material: &f" + drop.getMaterial().name());
            lore.add("&7Amount: &f" + drop.getAmount());
            lore.add("&7Drop Weight: &f" + drop.getChance());
            lore.add("");
            // Add custom name if present
            if (drop.getCustomName() != null) {
                lore.add("&7Custom Name: &f" + drop.getCustomName());
            }
            // Add biome restrictions
            if (!drop.getBiomes().isEmpty()) {
                lore.add("&7Biomes: &f" + String.join(", ", drop.getBiomes()));
            }
            // Add permission requirement
            if (drop.getPermission() != null) {
                lore.add("&7Permission: &f" + drop.getPermission());
            }
            // Add enchantments
            if (!drop.getEnchantments().isEmpty()) {
                lore.add("");
                lore.add("&7Enchantments:");
                drop.getEnchantments().forEach((enchant, level) -> {
                    lore.add("  &8• &f" + formatEnchantName(((org.bukkit.Keyed) enchant).getKey().getKey()) + " " + level);
                });
            }
            // Add lore from drop
            if (drop.getLore() != null && !drop.getLore().isEmpty()) {
                lore.add("");
                lore.add("&7Custom Lore:");
                drop.getLore().forEach(line -> lore.add("  &8• &7" + line));
            }
            ItemStack dropItem = new ItemBuilder(drop.getMaterial())
                    .name("&e" + (drop.getCustomName() != null ? drop.getCustomName() : drop.getMaterial().name()))
                    .lore(lore)
                    .glow(drop.isGlowing())
                    .build();
            setItem(slot, dropItem);
            displaySlot++;
        }
        // Category info
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name("&6&l" + formatCategoryName(selectedCategory))
                .lore(
                        "&7Total Drops: &f" + drops.size(),
                        "",
                        "&7This category contains fishing",
                        "&7drops available to players."
                )
                .build();
        setItem(49, infoItem);
        // Back to categories
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("&e← Back to Categories")
                .build();
        setItem(45, backItem, event -> {
            viewingCategory = false;
            selectedCategory = null;
            refresh(); // Refresh with category list
        });
        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("&c&lClose")
                .build();
        setItem(53, closeItem, event -> {
            Player p = getPlayer();
            if (p != null) p.closeInventory();
        });
    }

    private void fillBorder() {
        ItemStack borderItem = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        for (int i = 0; i < 9; i++) {
            setItem(i, borderItem);
            setItem(45 + i, borderItem);
        }
        for (int row = 1; row < 5; row++) {
            setItem(row * 9, borderItem);
            setItem(row * 9 + 8, borderItem);
        }
    }

    private Material getCategoryIcon(String category) {
        return switch (category.toLowerCase()) {
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
        if (category.startsWith("biome_")) {
            String biome = category.substring(6);
            return biome.substring(0, 1).toUpperCase() + biome.substring(1).toLowerCase() + " Biome";
        }
        return category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();
    }

    private String formatEnchantName(String key) {
        String[] parts = key.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
}

