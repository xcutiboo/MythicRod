import os

file_path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/DropsMenu.java"
with open(file_path, "r") as f:
    content = f.read()

# Fix getMaterial
content = content.replace("ItemStack dropItem = new ItemBuilder(drop.getMaterial())",
                          "ItemStack dropItem = new ItemBuilder(org.bukkit.Material.valueOf(drop.getIdentifier().toUpperCase().replace(\"MINECRAFT:\", \"\")))")

with open(file_path, "w") as f:
    f.write(content)

print("Fixed DropsMenu.java")
