import os
import re

file_path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/drops/CustomDrop.java"
with open(file_path, "r") as f:
    content = f.read()

# Make it NOT abstract
content = content.replace("public abstract class CustomDrop implements PlatformDrop",
                          "public class CustomDrop implements PlatformDrop")

content = content.replace("public abstract PlatformItem createItem();",
                          "public PlatformItem createItem() { return null; }")

with open(file_path, "w") as f:
    f.write(content)

print("Fixed CustomDrop.java")
