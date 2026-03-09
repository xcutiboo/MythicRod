package io.xcutiboo.mythicrod.gui.menus;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.gui.utils.ItemBuilder;
public abstract class PaginatedMenu<T> extends BaseMenu {
    protected int currentPage = 0;
    protected List<T> items;
    public PaginatedMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }
    @Override
    protected int getSize() {
        return 54; // 6 rows for pagination support
    }
    protected abstract List<T> getItems();
    protected abstract ItemStack renderItem(T item, int index);
    protected abstract void onItemClick(T item, int index);
    protected int getItemsPerPage() {
        return 45;
    }
    @Override
    protected void build() {
        items = getItems();
        if (items == null) {
            items = new java.util.ArrayList<>();
        }
        if (items.isEmpty()) {
            buildEmptyState();
            return;
        }
        // Calculate pagination
        int itemsPerPage = getItemsPerPage();
        if (itemsPerPage <= 0) {
            itemsPerPage = 45; // Fallback to default
        }
        int maxPage = Math.max(0, (int) Math.ceil((double) items.size() / itemsPerPage) - 1);
        currentPage = Math.max(0, Math.min(currentPage, maxPage));
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        // Display items for current page
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= items.size()) break; // Safety check
            T item = items.get(i);
            ItemStack displayItem = renderItem(item, i);
            if (displayItem != null) {
                final int index = i;
                setItem(slot, displayItem, event -> onItemClick(item, index));
                slot++;
            }
        }
        // Add navigation buttons
        buildNavigationBar(currentPage, maxPage);
    }
    protected void buildNavigationBar(int page, int maxPage) {
        // Previous page button
        if (page > 0) {
            ItemStack prevButton = new ItemBuilder(Material.ARROW)
                    .name("&e← Previous Page")
                    .lore(
                            "&7Page: &f" + page + "&7/&f" + (maxPage + 1),
                            "",
                            "&eClick to go back"
                    )
                    .build();
            setItem(45, prevButton, event -> {
                currentPage--;
                refresh();
            });
        } else {
            // Filler when no previous page
            setItem(45, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build());
        }
        // Page indicator in center
        ItemStack pageIndicator = new ItemBuilder(Material.BOOK)
                .name("&6Page Information")
                .lore(
                        "&7Current Page: &f" + (page + 1),
                        "&7Total Pages: &f" + (maxPage + 1),
                        "&7Total Items: &f" + items.size()
                )
                .build();
        setItem(49, pageIndicator);
        // Next page button
        if (page < maxPage) {
            ItemStack nextButton = new ItemBuilder(Material.ARROW)
                    .name("&eNext Page →")
                    .lore(
                            "&7Page: &f" + (page + 2) + "&7/&f" + (maxPage + 1),
                            "",
                            "&eClick to continue"
                    )
                    .build();
            setItem(53, nextButton, event -> {
                currentPage++;
                refresh();
            });
        } else {
            // Filler when no next page
            setItem(53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build());
        }
        // Fill remaining bottom row slots
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        for (int i = 46; i <= 52; i++) {
            if (i != 49) { // Skip page indicator
                setItem(i, filler);
            }
        }
    }
    protected void buildEmptyState() {
        ItemStack emptyIcon = new ItemBuilder(Material.BARRIER)
                .name("&cNo Items")
                .lore("&7There are no items to display.")
                .build();
        setItem(22, emptyIcon); // Center of inventory
        // Fill with glass panes
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        fillEmpty(filler);
    }
    protected void goToPage(int page) {
        this.currentPage = page;
        refresh();
    }
    protected int getCurrentPage() {
        return currentPage;
    }
    protected int getTotalItems() {
        return items != null ? items.size() : 0;
    }
}
