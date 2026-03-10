#!/bin/bash

echo "=== FORENSIC AUDIT REPORT ==="
echo ""
echo "## CRITICAL ARCHITECTURE VIOLATIONS"
echo ""

# Check for platform imports in common module
echo "### Platform Dependency Leakage in Common Module:"
grep -r "import net\.kyori\|import org\.bukkit\|import io\.papermc" mythicrod-common/src/main/java --include="*.java" | wc -l
grep -r "import net\.kyori\|import org\.bukkit\|import io\.papermc" mythicrod-common/src/main/java --include="*.java"

echo ""
echo "### God Classes (>250 lines):"
find . -name "*.java" -type f -exec wc -l {} + | awk '$1 > 250 {print $1, $2}' | grep -v ".gradle" | sort -rn

echo ""
echo "### Generic Exception Handling:"
grep -r "catch (Exception" --include="*.java" mythicrod-common mythicrod-paper mythicrod-spigot | wc -l

echo ""
echo "### String Literal Duplicates:"
echo "Checking for '&6&l[MythicRod]' occurrences:"
grep -r "&6&l\[MythicRod\]" --include="*.java" . | wc -l

echo ""
echo "### Static ItemStack Detection:"
grep -r "static.*ItemStack\|Map<String.*ItemStack>" --include="*.java" . | grep -v ".gradle"

