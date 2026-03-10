import re

with open("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/LanguageSwitchMenu.java", 'r') as f:
    content = f.read()

# Replace p with wrapped p
content = content.replace("plugin.getLanguageManager().trForSender(p,", "plugin.getLanguageManager().trForSender(new io.xcutiboo.mythicrod.spigot.platform.SpigotPlayer(p),")
# Replace player with wrapped player
content = content.replace("plugin.getLanguageManager().trForSender(player,", "plugin.getLanguageManager().trForSender(new io.xcutiboo.mythicrod.spigot.platform.SpigotPlayer(player),")
# Replace getPlayer() with wrapped getPlayer()
content = content.replace("plugin.getLanguageManager().trForSender(getPlayer(),", "plugin.getLanguageManager().trForSender(new io.xcutiboo.mythicrod.spigot.platform.SpigotPlayer(getPlayer()),")

with open("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/LanguageSwitchMenu.java", 'w') as f:
    f.write(content)
