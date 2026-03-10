import os
import re

file_path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/drops/CustomDrop.java"
with open(file_path, "r") as f:
    content = f.read()

# Refactor CustomDrop to use DropConfigurationRecord
content = content.replace("public class CustomDrop implements PlatformDrop {",
                          "public class CustomDrop implements PlatformDrop {\n    private final DropConfigurationRecord config;")

# Fix constructor
content = re.sub(r"public CustomDrop\(String identifier, int chance, int amount\) \{.*?\n    \}",
                 """public CustomDrop(DropConfigurationRecord config) {
        this.config = config;
    }""", content, flags=re.DOTALL)

content = re.sub(r"public CustomDrop\(String identifier, int chance, int amount, String customName,.*?\{.*?\n        \}\n        this\.nexoItemId = nexoItemId;\n    \}",
                 "", content, flags=re.DOTALL)

# Fix getters
content = re.sub(r"public String getIdentifier\(\) \{ return identifier; \}", "public String getIdentifier() { return config.identifier(); }", content)
content = re.sub(r"public int getChance\(\) \{ return chance; \}", "public int getChance() { return config.chance(); }", content)
content = re.sub(r"public int getAmount\(\) \{ return amount; \}", "public int getAmount() { return config.amount(); }", content)
content = re.sub(r"public String getCustomName\(\) \{ return customName; \}", "public String getCustomName() { return config.customName(); }", content)
content = re.sub(r"public List<String> getLore\(\) \{ return lore; \}", "public List<String> getLore() { return config.lore(); }", content)
content = re.sub(r"public int getCustomModelData\(\) \{ return 0; \}", "public int getCustomModelData() { return config.customModelData(); }", content)
content = re.sub(r"public Map<String, Integer> getEnchantments\(\) \{ return enchantments; \}", "public Map<String, Integer> getEnchantments() { return config.enchantments(); }", content)
content = re.sub(r"public List<String> getItemFlags\(\) \{ return itemFlags; \}", "public List<String> getItemFlags() { return config.itemFlags(); }", content)
content = re.sub(r"public boolean isGlowing\(\) \{ return glowing; \}", "public boolean isGlowing() { return config.glowing(); }", content)
content = re.sub(r"public String getPermission\(\) \{ return permission; \}", "public String getPermission() { return config.permission(); }", content)
content = re.sub(r"public List<String> getBiomes\(\) \{ return biomes; \}", "public List<String> getBiomes() { return config.biomes(); }", content)
content = re.sub(r"public String getNexoItemId\(\) \{ return nexoItemId; \}", "public String getNexoItemId() { return config.nexoItemId(); }", content)

# Remove old fields
content = re.sub(r"    private final String identifier;\n    private final int chance;\n    private final int amount;\n    private String customName;\n    private List<String> lore;\n    private final Map<String, Integer> enchantments;\n    private final List<String> itemFlags;\n    private boolean glowing;\n    private final String permission;\n    private final List<String> biomes;\n    private final String nexoItemId;", "", content)

# Remove setCustomName, setLore, setGlowing
content = re.sub(r"    public void setCustomName\(String customName\) \{.*?\}\n", "", content, flags=re.DOTALL)
content = re.sub(r"    public void setLore\(List<String> lore\) \{.*?\}\n", "", content, flags=re.DOTALL)
content = re.sub(r"    public void setGlowing\(boolean glowing\) \{.*?\}\n", "", content, flags=re.DOTALL)

with open(file_path, "w") as f:
    f.write(content)

print("Fixed CustomDrop.java record")
