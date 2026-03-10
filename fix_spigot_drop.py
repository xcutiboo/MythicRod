import re

with open("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/drops/SpigotCustomDrop.java", "r") as f:
    content = f.read()

content = content.replace("import io.xcutiboo.mythicrod.drops.CustomDrop;", "import io.xcutiboo.mythicrod.drops.CustomDrop;\nimport io.xcutiboo.mythicrod.api.drop.DropConfigurationRecord;")

content = re.sub(r'public SpigotCustomDrop\(Material material, int chance, int amount\) \{.*?\}', '', content, flags=re.DOTALL)
content = re.sub(r'public SpigotCustomDrop\(Material material, int chance, int amount, String customName,.*?List<String> biomes\) \{.*?\}', '', content, flags=re.DOTALL)

new_constructor = """
    public SpigotCustomDrop(DropConfigurationRecord config) {
        super(config);
    }
"""
content = content.replace("public class SpigotCustomDrop extends CustomDrop {", "public class SpigotCustomDrop extends CustomDrop {" + new_constructor)

content = content.replace("getMaterial()", "Material.matchMaterial(getIdentifier())")
content = content.replace("getCustomName()", "getConfig().itemContext().displayName()")
content = content.replace("getLore()", "getConfig().itemContext().lore()")
content = content.replace("getItemFlags()", "getConfig().itemContext().flags()") # Need to check if itemFlags exists
content = content.replace("getEnchantments()", "getConfig().itemContext().enchantments()")
content = content.replace("isGlowing()", "getConfig().itemContext().glowing()") # Need to check glowing

# Actually, the ItemContextRecord only has material, customModelData, displayName, lore, enchantments
# Let's fix this properly in SpigotItemFactory instead of SpigotCustomDrop which shouldn't exist anymore

with open("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/drops/SpigotCustomDrop.java", "w") as f:
    f.write(content)

