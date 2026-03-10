import os
import re

file_path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/BaseMenu.java"
with open(file_path, "r") as f:
    content = f.read()

# Make BaseMenu use MythicRodMenuHolder
content = content.replace("import io.xcutiboo.mythicrod.MythicRod;",
                          "import io.xcutiboo.mythicrod.MythicRod;\nimport io.xcutiboo.mythicrod.spigot.gui.MythicRodMenuHolder;")

content = content.replace("inventory = Bukkit.createInventory(null, getSize(), LegacyComponentSerializer.legacySection().serialize(titleComponent));",
                          "inventory = Bukkit.createInventory(new MythicRodMenuHolder(this), getSize(), LegacyComponentSerializer.legacySection().serialize(titleComponent));")

with open(file_path, "w") as f:
    f.write(content)

print("Fixed BaseMenu.java")
