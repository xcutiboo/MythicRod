package io.xcutiboo.mythicrod.gui.menus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.item.ItemBuilder;
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
            items = new ArrayList<>();
        }
        if (items.isEmpty()) {
            buildEmptyState();
            return;
        }
        int itemsPerPage = getItemsPerPage();
        if (itemsPerPage <= 0) {
            itemsPerPage = 45; // Fallback to default
        }
        int maxPage = Math.max(0, (int) Math.ceil((double) items.size() / itemsPerPage) - 1);
        currentPage = Math.max(0, Math.min(currentPage, maxPage));
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
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
        buildNavigationBar(currentPage, maxPage);
    }
    protected void buildNavigationBar(int page, int maxPage) {
        if (page > 0) {
            ItemStack prevButton = new ItemBuilder(Material.ARROW)
                    .name(tr("gui.paginated.prev_name"))
                    .lore(
                            tr("gui.paginated.page_info", Map.of("%current%", String.valueOf(page), "%total%", String.valueOf(maxPage + 1))),
                            "",
                            tr("gui.paginated.prev_click")
                    )
                    .build();
            setItem(45, prevButton, event -> {
                playClickSound();
                currentPage--;
                refresh();
            });
        } else {
            setItem(45, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build());
        }
        ItemStack pageIndicator = new ItemBuilder(Material.BOOK)
                .name(tr("gui.paginated.info_name"))
                .lore(
                        tr("gui.paginated.current_page", Map.of("%page%", String.valueOf(page + 1))),
                        tr("gui.paginated.total_pages", Map.of("%total%", String.valueOf(maxPage + 1))),
                        tr("gui.paginated.total_items", Map.of("%count%", String.valueOf(items.size())))
                )
                .build();
        setItem(49, pageIndicator);
        if (page < maxPage) {
            ItemStack nextButton = new ItemBuilder(Material.ARROW)
                    .name(tr("gui.paginated.next_name"))
                    .lore(
                            tr("gui.paginated.page_info", Map.of("%current%", String.valueOf(page + 2), "%total%", String.valueOf(maxPage + 1))),
                            "",
                            tr("gui.paginated.next_click")
                    )
                    .build();
            setItem(53, nextButton, event -> {
                playClickSound();
                currentPage++;
                refresh();
            });
        } else {
            setItem(53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build());
        }
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
                .name(tr("gui.paginated.empty_name"))
                .lore(tr("gui.paginated.empty_lore"))
                .build();
        setItem(22, emptyIcon); // Center of inventory
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
