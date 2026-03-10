import glob
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # BaseMenu.java has getPlatformPlayer() inside which returns PlatformPlayer.
    if "BaseMenu.java" in filepath:
        if "public io.xcutiboo.mythicrod.api.platform.PlatformPlayer getPlatformPlayer()" not in content:
            content = content.replace("protected Player getPlayer() {\n        return plugin.getServer().getPlayer(playerUuid);\n    }", 
                                      "protected Player getPlayer() {\n        return plugin.getServer().getPlayer(playerUuid);\n    }\n\n    public io.xcutiboo.mythicrod.api.platform.PlatformPlayer getPlatformPlayer() {\n        Player p = getPlayer();\n        return p != null ? plugin.getPlatform().getPlayer(p.getUniqueId()) : null;\n    }")

    # Fix LanguageSwitchMenu
    if "LanguageSwitchMenu.java" in filepath:
        content = re.sub(r'plugin\.getLanguageManager\(\)\.trForSender\((p|player|getPlayer\(\)),', r'plugin.getLanguageManager().trForSender(getPlatformPlayer(),', content)

    # Fix ConfigMenu
    if "ConfigMenu.java" in filepath:
        content = re.sub(r'plugin\.getLanguageManager\(\)\.trForSender\((p|player|getPlayer\(\)),', r'plugin.getLanguageManager().trForSender(getPlatformPlayer(),', content)

    # Fix MainHubMenu
    if "MainHubMenu.java" in filepath:
        content = re.sub(r'plugin\.getLanguageManager\(\)\.trForSender\((p|player|getPlayer\(\)),', r'plugin.getLanguageManager().trForSender(getPlatformPlayer(),', content)

    # Fix DropsMenu
    if "DropsMenu.java" in filepath:
        content = re.sub(r'plugin\.getLanguageManager\(\)\.trForSender\((p|player|getPlayer\(\)),', r'plugin.getLanguageManager().trForSender(getPlatformPlayer(),', content)

    # Fix StatsMenu
    if "StatsMenu.java" in filepath:
        content = re.sub(r'plugin\.getLanguageManager\(\)\.trForSender\((p|player|getPlayer\(\)),', r'plugin.getLanguageManager().trForSender(getPlatformPlayer(),', content)
        content = re.sub(r'plugin\.getStatisticsManager\(\)\.getPlayerStats\((player|p)\)', r'plugin.getStatisticsManager().getPlayerStats(getPlatformPlayer())', content)

    # Write back
    with open(filepath, 'w') as f:
        f.write(content)

for filepath in glob.glob("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/*.java"):
    process_file(filepath)

# Fix Brigadier command manager
cmd_file = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/commands/BrigadierStyleCommandManager.java"
with open(cmd_file, 'r') as f:
    content = f.read()

# Replace sender with plugin.getPlatform().getCommandSender(...)
wrapper = """(sender instanceof Player ? plugin.getPlatform().getPlayer(((Player) sender).getUniqueId()) : plugin.getPlatform().getCommandSender("CONSOLE"))"""
content = re.sub(r'plugin\.getLanguageManager\(\)\.trForSender\(sender,', f'plugin.getLanguageManager().trForSender({wrapper},', content)

with open(cmd_file, 'w') as f:
    f.write(content)
