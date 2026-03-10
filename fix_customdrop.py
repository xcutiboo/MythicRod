import os
import re

file_path = "mythicrod-common/src/main/java/io/xcutiboo/mythicrod/drops/CustomDrop.java"
with open(file_path, "r") as f:
    content = f.read()

# Make class abstract
content = content.replace("public class CustomDrop implements PlatformDrop",
                          "public abstract class CustomDrop implements PlatformDrop")

with open(file_path, "w") as f:
    f.write(content)

print("Fixed CustomDrop.java abstract")
