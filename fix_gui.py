import glob
import re

def fix_basemenu():
    with open("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/BaseMenu.java", "r") as f:
        content = f.read()

    if "getPlatformPlayer()" not in content:
        content = content.replace("protected Player getPlayer() {\n        return plugin.getServer().getPlayer(playerUuid);\n    }", 
                                  "protected Player getPlayer() {\n        return plugin.getServer().getPlayer(playerUuid);\n    }\n\n    public io.xcutiboo.mythicrod.api.platform.PlatformPlayer getPlatformPlayer() {\n        Player p = getPlayer();\n        return p != null ? new io.xcutiboo.mythicrod.spigot.platform.SpigotPlayer(p) : null;\n    }")
        
    with open("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/BaseMenu.java", "w") as f:
        f.write(content)

def fix_menus():
    for filepath in glob.glob("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/*.java"):
        with open(filepath, "r") as f:
            content = f.read()

        # Wrap for trForSender
        content = re.sub(r'plugin\.getLanguageManager\(\)\.trForSender\((p|player|getPlayer\(\)),', r'plugin.getLanguageManager().trForSender(getPlatformPlayer(),', content)

        # Wrap for getPlayerStats
        content = re.sub(r'plugin\.getStatisticsManager\(\)\.getPlayerStats\((player|p)\)', r'plugin.getStatisticsManager().getPlayerStats(getPlatformPlayer())', content)

        with open(filepath, "w") as f:
            f.write(content)

fix_basemenu()
fix_menus()

# Fix BrigadierCommandManager
with open("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/commands/BrigadierStyleCommandManager.java", "r") as f:
    content = f.read()

wrapper = """(sender instanceof Player ? new io.xcutiboo.mythicrod.spigot.platform.SpigotPlayer((Player) sender) : new io.xcutiboo.mythicrod.spigot.platform.SpigotCommandSender(sender))"""
content = re.sub(r'plugin\.getLanguageManager\(\)\.trForSender\(sender,', f'plugin.getLanguageManager().trForSender({wrapper},', content)

with open("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/commands/BrigadierStyleCommandManager.java", "w") as f:
    f.write(content)

