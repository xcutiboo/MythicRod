import os
import glob
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Replace .collect(Collectors.toList()) with .toList()
    content = content.replace(".collect(java.util.stream.Collectors.toList())", ".toList()")
    content = content.replace(".collect(Collectors.toList())", ".toList()")

    # Remove unused java.util.stream.Collectors imports
    content = re.sub(r'import java\.util\.stream\.Collectors;\n', '', content)

    with open(filepath, 'w') as f:
        f.write(content)

for filepath in glob.glob("mythicrod-common/src/main/java/io/xcutiboo/mythicrod/**/*.java", recursive=True):
    process_file(filepath)
    
for filepath in glob.glob("mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/**/*.java", recursive=True):
    process_file(filepath)
    
for filepath in glob.glob("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/**/*.java", recursive=True):
    process_file(filepath)
