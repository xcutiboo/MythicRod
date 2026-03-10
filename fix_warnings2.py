import os
import re

# Fix unused fields in MythicRod.java
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/MythicRod.java"
with open(path, "r") as f: content = f.read()
content = content.replace("private static final int BSTATS_PLUGIN_ID = 23847;\n", "")
content = content.replace("private BrigadierStyleCommandManager commandManager;\n", "")
content = content.replace("this.commandManager = injector.getInstance(BrigadierStyleCommandManager.class);\n", "injector.getInstance(BrigadierStyleCommandManager.class);\n")
with open(path, "w") as f: f.write(content)

# Fix deprecated stuff in SpigotPlatformItem.java
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/item/SpigotPlatformItem.java"
with open(path, "r") as f: content = f.read()
content = content.replace("enchants.put(enchant.getKey().getKey(), level)", "enchants.put(enchant.key().value(), level)")
with open(path, "w") as f: f.write(content)

# Fix NexoItemProvider.java unused field warning
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/item/NexoItemProvider.java"
with open(path, "r") as f: content = f.read()
# Note: actually it's used via reflection but IDE thinks it's not because we don't have nexoItemsClass used much, wait, we do: Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
# we can just leave it as is or change it slightly
with open(path, "w") as f: f.write(content)

# Fix FishingListener.java deprecated Biome.name()
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/fishing/FishingListener.java"
with open(path, "r") as f: content = f.read()
content = content.replace("hook.getLocation().getBlock().getBiome().name()", "hook.getLocation().getBlock().getBiome().toString()")
with open(path, "w") as f: f.write(content)

# Remove unused ArrayList in SpigotConfiguration.java
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/config/SpigotConfiguration.java"
with open(path, "r") as f: content = f.read()
content = content.replace("import java.util.ArrayList;\n", "")
with open(path, "w") as f: f.write(content)

# Remove unused Inventory in GUIManager.java
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/GUIManager.java"
with open(path, "r") as f: content = f.read()
content = content.replace("import org.bukkit.inventory.Inventory;\n", "")
with open(path, "w") as f: f.write(content)

# Remove unused IOException in ConfigMenu.java
path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/ConfigMenu.java"
with open(path, "r") as f: content = f.read()
content = content.replace("import java.io.IOException;\n", "")
with open(path, "w") as f: f.write(content)

print("Fixed warnings in Spigot")
