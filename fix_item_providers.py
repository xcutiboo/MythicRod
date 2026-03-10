import re

def fix_vanilla_provider():
    filepath = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/item/VanillaItemProvider.java"
    with open(filepath, 'r') as f:
        content = f.read()

    # The SpigotPlatformItem needs identifier, ItemStack, isCustom
    content = content.replace("new SpigotPlatformItem(item)", "new io.xcutiboo.mythicrod.item.SpigotPlatformItem(identifier, item, false)")
    with open(filepath, 'w') as f:
        f.write(content)

def fix_nexo_provider():
    filepath = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/item/NexoItemProvider.java"
    with open(filepath, 'r') as f:
        content = f.read()

    # The SpigotPlatformItem needs identifier, ItemStack, isCustom
    content = content.replace("new SpigotPlatformItem(item)", "new io.xcutiboo.mythicrod.item.SpigotPlatformItem(identifier, item, true)")
    with open(filepath, 'w') as f:
        f.write(content)

fix_vanilla_provider()
fix_nexo_provider()
