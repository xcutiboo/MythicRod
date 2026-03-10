import os
import re

# We will apply the new security listener to both Spigot and Paper (if it existed, but we saw error for Paper earlier)
# Wait, let's fix Spigot first

file_path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/GUIManager.java"
with open(file_path, "r") as f:
    content = f.read()

# Replace the current onInventoryClick
replacement_click = """    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MythicRodMenuHolder holder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Security Check: Ensure only admins can interact with GUIs
        if (!player.hasPermission("mythicrod.admin.gui")) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        event.setCancelled(true);

        try {
            holder.getMenu().handleClick(event);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MythicRod-GUIManager] Error handling menu interaction.", e);
            closeMenu(player);
        }
    }"""

content = re.sub(r"    @EventHandler\(priority = EventPriority\.HIGHEST\)\s+public void onInventoryClick\(InventoryClickEvent event\) \{.*?\n    \}", replacement_click, content, flags=re.DOTALL)

# Replace the current onInventoryDrag
replacement_drag = """    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MythicRodMenuHolder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!player.hasPermission("mythicrod.admin.gui")) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        if (event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }"""

content = re.sub(r"    @EventHandler\(priority = EventPriority\.HIGHEST\)\s+public void onInventoryDrag\(InventoryDragEvent event\) \{.*?\n    \}", replacement_drag, content, flags=re.DOTALL)

# Add Permission check in openMenu
content = re.sub(r"if \(player == null \|\| !player\.isOnline\(\)\) \{\n\s+return false;\n\s+\}", 
                 "if (player == null || !player.isOnline()) {\n            return false;\n        }\n\n        if (!player.hasPermission(\"mythicrod.admin.gui\")) {\n            player.sendMessage(\"§cYou do not have permission to use the GUI.\");\n            return false;\n        }", content)

with open(file_path, "w") as f:
    f.write(content)

print("Fixed GUIManager.java")
