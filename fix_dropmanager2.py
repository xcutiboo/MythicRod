import os
import re

file_path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/drops/DropManager.java"
with open(file_path, "r") as f:
    content = f.read()

# Fix from configuration parsing remaining issues with constructor
content = content.replace("return new CustomDrop(identifier, chance, amount);",
                          "return new CustomDrop(new DropConfigurationRecord(identifier, chance, amount, null, null, 0, null, null, false, null, null, null));")

with open(file_path, "w") as f:
    f.write(content)

print("Fixed DropManager.java again")
