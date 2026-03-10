import os
import re

# Vanilla
file_path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/item/VanillaItemProvider.java"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("return Optional.of(new SpigotPlatformItem(item));",
                          "return Optional.of(new SpigotPlatformItem(id, item, false));")
with open(file_path, "w") as f:
    f.write(content)

# Nexo
file_path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/item/NexoItemProvider.java"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("return Optional.of(new SpigotPlatformItem(item));",
                          "return Optional.of(new SpigotPlatformItem(\"nexo:\" + cleanId, item, true));")
with open(file_path, "w") as f:
    f.write(content)

print("Fixed providers")
