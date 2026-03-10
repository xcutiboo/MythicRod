import os
import re

file_path = "build.gradle.kts"
with open(file_path, "r") as f:
    content = f.read()

# Make sure we use Java 21 toolchain
if "jvmToolchain(21)" not in content:
    content = content.replace("jvmToolchain(17)", "jvmToolchain(21)")

with open(file_path, "w") as f:
    f.write(content)

print("Fixed build.gradle.kts")
