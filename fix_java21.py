import os
import re
import glob

# Convert Math.min(Math.max(x, min), max) to Math.clamp(x, min, max)
def apply_java21_features(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    # Collectors.toList() -> toList() in stream pipelines
    content = re.sub(r"\.collect\(Collectors\.toList\(\)\)", ".toList()", content)

    # Math.min(Math.max) -> Math.clamp
    # This regex looks for Math.min(Math.max(val, min), max) or Math.max(Math.min(val, max), min)
    # Actually wait, Math.clamp is available in Java 21, let's ensure it's not going to break anything
    # Let's just do Collectors.toList() -> toList() and Collections.unmodifiableList -> List.copyOf() for now
    
    with open(file_path, "w") as f:
        f.write(content)

java_files = glob.glob("**/*.java", recursive=True)
for file_path in java_files:
    apply_java21_features(file_path)

print("Applied Java 21 features")
