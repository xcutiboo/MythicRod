import re
import glob

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Replace Math.max(1, Math.min(amount, 64)) with Math.clamp(amount, 1, 64)
    content = re.sub(r'Math\.max\(\s*(\d+)\s*,\s*Math\.min\(\s*([a-zA-Z_0-9]+)\s*,\s*(\d+)\s*\)\s*\)', r'Math.clamp(\2, \1, \3)', content)
    
    # Replace Math.min(Math.max(x, min), max) with Math.clamp(x, min, max)
    content = re.sub(r'Math\.min\(\s*Math\.max\(\s*([a-zA-Z_0-9]+)\s*,\s*([a-zA-Z_0-9]+)\s*\)\s*,\s*([a-zA-Z_0-9]+)\s*\)', r'Math.clamp(\1, \2, \3)', content)

    with open(filepath, 'w') as f:
        f.write(content)

process_file("mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/gui/utils/ItemBuilder.java")
process_file("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/utils/ItemBuilder.java")
process_file("mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/gui/menus/PaginatedMenu.java")
import re

filepath = "mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/paper/commands/BrigadierCommandManager.java"
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace("limit = Math.min(Math.max(limit, 1), 50);", "limit = Math.clamp(limit, 1, 50);")

with open(filepath, 'w') as f:
    f.write(content)

