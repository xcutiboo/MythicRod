import os
import re

file_path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/drops/DropManager.java"
with open(file_path, "r") as f:
    content = f.read()

# Fix default drops loading
content = re.sub(r"defaults\.add\(new CustomDrop\((.*?), (.*?), (.*?)\)\);",
                 r'defaults.add(new CustomDrop(new DropConfigurationRecord(\1, \2, \3, null, null, 0, null, null, false, null, null, null)));', content)

# Fix from configuration parsing
content = re.sub(r"return new CustomDrop\(identifier, chance, amount, name, lore, enchantments,\n\s+itemFlags, glowing, permission, biomes, nexoItemId\);",
                 r'return new CustomDrop(new DropConfigurationRecord(identifier, chance, amount, name, lore, 0, enchantments, itemFlags, glowing, permission, biomes, nexoItemId));', content)

with open(file_path, "w") as f:
    f.write(content)

print("Fixed DropManager.java")
