import os

# Fix BrigadierStyleCommandManager
file_path = 'mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/commands/BrigadierStyleCommandManager.java'
with open(file_path, 'r') as f:
    content = f.read()
content = content.replace('plugin.getStatisticsManager().getPlayerStatistics(target)', 'plugin.getStatisticsManager().getPlayerStatistics(plugin.getPlatform().getPlayer(target.getUniqueId()))')
content = content.replace('drop.getMaterial().name()', 'drop.getIdentifier()')
with open(file_path, 'w') as f:
    f.write(content)

# Fix ConfigManager (add reload)
file_path = 'mythicrod-common/src/main/java/io/xcutiboo/mythicrod/config/ConfigManager.java'
with open(file_path, 'r') as f:
    content = f.read()
if 'public void reload()' not in content:
    content = content.replace('private void validateAndCache() {', 'public void reload() {\n        validateAndCache();\n    }\n\n    private void validateAndCache() {')
with open(file_path, 'w') as f:
    f.write(content)

# Fix DropsMenu
file_path = 'mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/DropsMenu.java'
with open(file_path, 'r') as f:
    content = f.read()
content = content.replace('drop.getMaterial().name()', 'drop.getIdentifier()')
content = content.replace('((org.bukkit.Keyed) enchant).getKey().getKey()', 'enchant')
with open(file_path, 'w') as f:
    f.write(content)
