import os
import re
import glob

# Remove unused imports in DropManager.java
path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/drops/DropManager.java"
with open(path, "r") as f: content = f.read()
content = content.replace("import java.util.stream.Collectors;\n", "")
content = re.sub(r"    private final RandomSelector<CustomDrop> selector = new RandomSelector<>();\n", "", content)
with open(path, "w") as f: f.write(content)

# Remove unused import in PlatformGUIManager.java
path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/api/gui/PlatformGUIManager.java"
with open(path, "r") as f: content = f.read()
content = content.replace("import java.util.UUID;\n", "")
with open(path, "w") as f: f.write(content)

# Remove unused imports in ConfigManager.java
path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/config/ConfigManager.java"
with open(path, "r") as f: content = f.read()
content = content.replace("import java.io.InputStream;\n", "")
content = content.replace("import java.io.InputStreamReader;\n", "")
content = content.replace("import java.nio.charset.StandardCharsets;\n", "")
content = content.replace("    private java.io.File dropsFile;\n", "")
content = content.replace("    private java.io.File messagesFile;\n", "")
content = re.sub(r"    public static final java\.util\.List<String> VALID_PROFILES = java\.util\.Arrays\.asList\(\"vanilla\", \"nexo\"\);\n", "", content)
content = re.sub(r"    public static final java\.util\.List<String> VALID_LANGUAGES = java\.util\.Arrays\.asList\(\"en_US\", \"es_ES\", \"zh_CN\", \"ja_JP\", \"ru_RU\", \"ko_KR\", \"pt_BR\", \"fr_FR\", \"de_DE\", \"it_IT\"\);\n", "", content)
with open(path, "w") as f: f.write(content)

# Remove unused imports in LanguageManager.java
path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/config/LanguageManager.java"
with open(path, "r") as f: content = f.read()
content = content.replace("import java.io.InputStreamReader;\n", "")
content = content.replace("import java.nio.charset.StandardCharsets;\n", "")
content = content.replace("    private final ConfigManager configManager;\n", "")
content = content.replace("        this.configManager = configManager;\n", "")
with open(path, "w") as f: f.write(content)

print("Fixed warnings in common")
