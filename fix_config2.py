import os
import re

file_path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/ConfigMenu.java"
with open(file_path, "r") as f:
    content = f.read()

# Replace the catch IOException block with catch Exception
content = re.sub(r"catch \(IOException e\) \{", "catch (Exception e) {", content)

with open(file_path, "w") as f:
    f.write(content)

print("Fixed ConfigMenu.java again")
