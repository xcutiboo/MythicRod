import os
import re

# Fix back SpigotPlatformItem.java
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/item/SpigotPlatformItem.java"
with open(path, "r") as f: content = f.read()
content = content.replace("enchants.put(enchant.key().value(), level)", "enchants.put(enchant.getKey().getKey(), level)")
with open(path, "w") as f: f.write(content)

print("Fixed warnings in Spigot")
