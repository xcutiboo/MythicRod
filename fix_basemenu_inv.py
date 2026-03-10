import os

file_path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/BaseMenu.java"
with open(file_path, "r") as f:
    content = f.read()

# Add getInventory method
content = content.replace("public void open() {",
                          "public Inventory getInventory() {\n        return inventory;\n    }\n\n    public void open() {")

with open(file_path, "w") as f:
    f.write(content)

print("Fixed BaseMenu.java inventory")
