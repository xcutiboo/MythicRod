import os

file_path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/ConfigMenu.java"
with open(file_path, "r") as f:
    content = f.read()

# Fix saveConfig exception
content = content.replace("config.saveConfig();",
                          "try { config.saveConfig(); } catch (Exception e) { e.printStackTrace(); }")

with open(file_path, "w") as f:
    f.write(content)

print("Fixed ConfigMenu.java")
