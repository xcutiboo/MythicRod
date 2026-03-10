import os
import re

# Fix ConfigManager.java unused fields
path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/config/ConfigManager.java"
with open(path, "r") as f:
    content = f.read()

# Remove unused fields
content = re.sub(r"\s+private java\.io\.File dropsFile;\n", "", content)
content = re.sub(r"\s+private java\.io\.File messagesFile;\n", "", content)
content = re.sub(r"\s+public static final java\.util\.List<String> VALID_PROFILES = .*?;\n", "", content)
content = re.sub(r"\s+public static final java\.util\.List<String> VALID_LANGUAGES = .*?;\n", "", content)

with open(path, "w") as f:
    f.write(content)

# Fix DropManager.java unused selector field
path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/drops/DropManager.java"
with open(path, "r") as f:
    content = f.read()

content = re.sub(r"\s+private final RandomSelector<CustomDrop> selector = new RandomSelector<>\(\);\n", "", content)

with open(path, "w") as f:
    f.write(content)

# Fix NexoItemProvider.java - remove unused field
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/item/NexoItemProvider.java"
if os.path.exists(path):
    with open(path, "r") as f:
        content = f.read()
    
    content = re.sub(r"\s+private Class<\?> nexoItemsClass;\n", "", content)
    
    with open(path, "w") as f:
        f.write(content)

# Fix SpigotItemFactory.java - remove unused logger
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/item/SpigotItemFactory.java"
if os.path.exists(path):
    with open(path, "r") as f:
        content = f.read()
    
    content = re.sub(r"\s+private final Logger logger;\n", "", content)
    content = re.sub(r"\s+this\.logger = logger;\n", "", content)
    
    with open(path, "w") as f:
        f.write(content)

# Fix BrigadierStyleCommandManager.java - remove unused Collectors import
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/commands/BrigadierStyleCommandManager.java"
if os.path.exists(path):
    with open(path, "r") as f:
        content = f.read()
    
    content = content.replace("import java.util.stream.Collectors;\n", "")
    
    with open(path, "w") as f:
        f.write(content)

print("Fixed all remaining warnings")
