import glob
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # audiences().player() takes org.bukkit.entity.Player. Make sure it casts.
    content = re.sub(r'plugin\.audiences\(\)\.player\((player|p)\)', r'plugin.audiences().player((org.bukkit.entity.Player) \1)', content)

    # In ConfigMenu, DropsMenu, MainHubMenu openMenu/openMainHub needs Player not PlatformPlayer
    # So we don't modify openMenu or openMainHub since they already take Player

    with open(filepath, 'w') as f:
        f.write(content)

for filepath in glob.glob("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/*.java"):
    process_file(filepath)

