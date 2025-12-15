#!/bin/bash
# Integration test to simulate the release workflow locally
# This verifies that the build produces a valid JAR that can be released

set -e

echo "================================================"
echo "MythicRod Release Workflow Integration Test"
echo "================================================"
echo ""

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Cleanup function
cleanup() {
    echo ""
    echo "Cleaning up..."
    ./gradlew clean > /dev/null 2>&1 || true
}

trap cleanup EXIT

echo "Step 1: Validating environment"
echo "-------------------------------"
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java not found${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "Java version: $JAVA_VERSION"

if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${YELLOW}⚠ Warning: Java $JAVA_VERSION found, but Java 21+ is recommended${NC}"
else
    echo -e "${GREEN}✓ Java $JAVA_VERSION is suitable${NC}"
fi
echo ""

echo "Step 2: Making Gradle wrapper executable"
echo "-----------------------------------------"
chmod +x ./gradlew
echo -e "${GREEN}✓ Gradle wrapper is now executable${NC}"
echo ""

echo "Step 3: Cleaning previous builds"
echo "---------------------------------"
./gradlew clean --no-daemon
echo -e "${GREEN}✓ Clean completed${NC}"
echo ""

echo "Step 4: Building the plugin"
echo "---------------------------"
echo "This may take a while on first run..."
if ./gradlew build --no-daemon -Dorg.gradle.internal.http.socketTimeout=60000 2>&1; then
    echo -e "${GREEN}✓ Build succeeded${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    echo ""
    echo "This may be due to network issues accessing PaperMC repositories."
    echo "The GitHub Actions workflow will work correctly in the cloud environment."
    exit 1
fi
echo ""

echo "Step 5: Verifying build artifacts"
echo "----------------------------------"
if [ ! -d "build/libs" ]; then
    echo -e "${RED}✗ build/libs directory not found${NC}"
    exit 1
fi

JAR_FILES=(build/libs/*.jar)
JAR_COUNT=0
PLUGIN_JAR=""

for jar in "${JAR_FILES[@]}"; do
    if [ -f "$jar" ]; then
        JAR_COUNT=$((JAR_COUNT + 1))
        filename=$(basename "$jar")
        size=$(du -h "$jar" | cut -f1)
        
        # Skip plain and sources JARs (these should be excluded from releases)
        if [[ "$filename" == *"-plain.jar" ]]; then
            echo "  Found (excluded): $filename ($size)"
        elif [[ "$filename" == *"-sources.jar" ]]; then
            echo "  Found (excluded): $filename ($size)"
        else
            echo -e "${GREEN}  ✓ Found: $filename ($size)${NC}"
            PLUGIN_JAR="$jar"
        fi
    fi
done

if [ -z "$PLUGIN_JAR" ]; then
    echo -e "${RED}✗ No plugin JAR found${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Plugin JAR is ready: $(basename "$PLUGIN_JAR")${NC}"
echo ""

echo "Step 6: Validating JAR structure"
echo "---------------------------------"
if command -v unzip &> /dev/null; then
    # Check for plugin metadata
    if unzip -l "$PLUGIN_JAR" | grep -q "plugin.yml\|paper-plugin.yml"; then
        echo -e "${GREEN}✓ Plugin metadata found in JAR${NC}"
    else
        echo -e "${RED}✗ Plugin metadata not found in JAR${NC}"
        exit 1
    fi
    
    # Check for compiled classes
    if unzip -l "$PLUGIN_JAR" | grep -q "\.class$"; then
        CLASS_COUNT=$(unzip -l "$PLUGIN_JAR" | grep "\.class$" | wc -l)
        echo -e "${GREEN}✓ Found $CLASS_COUNT compiled class files${NC}"
    else
        echo -e "${RED}✗ No compiled classes found in JAR${NC}"
        exit 1
    fi
    
    # Check for manifest
    if unzip -l "$PLUGIN_JAR" | grep -q "META-INF/MANIFEST.MF"; then
        echo -e "${GREEN}✓ JAR manifest found${NC}"
        
        # Extract and show version from manifest
        VERSION=$(unzip -p "$PLUGIN_JAR" META-INF/MANIFEST.MF | grep "Implementation-Version" | cut -d' ' -f2 | tr -d '\r')
        if [ -n "$VERSION" ]; then
            echo "  Version in JAR: $VERSION"
        fi
    else
        echo -e "${YELLOW}⚠ JAR manifest not found${NC}"
    fi
else
    echo -e "${YELLOW}⚠ unzip not available, skipping JAR structure validation${NC}"
fi
echo ""

echo "Step 7: Simulating artifact upload"
echo "-----------------------------------"
ARTIFACT_DIR="/tmp/mythicrod-test-artifacts"
mkdir -p "$ARTIFACT_DIR"

# Copy only the plugin JAR (excluding plain and sources)
cp "$PLUGIN_JAR" "$ARTIFACT_DIR/"
echo -e "${GREEN}✓ Plugin JAR copied to: $ARTIFACT_DIR${NC}"
echo "  Size: $(du -h "$ARTIFACT_DIR/$(basename "$PLUGIN_JAR")" | cut -f1)"
echo ""

echo "Step 8: Simulating release preparation"
echo "---------------------------------------"
VERSION=$(grep "^version=" gradle.properties | cut -d'=' -f2)
echo "Current version from gradle.properties: $VERSION"
TAG_NAME="v$VERSION"
echo "Release tag would be: $TAG_NAME"
echo ""
echo "To create an actual release, run:"
echo "  git tag $TAG_NAME"
echo "  git push origin $TAG_NAME"
echo ""

echo "================================================"
echo "Integration Test Summary"
echo "================================================"
echo -e "${GREEN}✓ All checks passed!${NC}"
echo ""
echo "The release workflow is ready to use."
echo ""
echo "Release checklist:"
echo "  1. Ensure all changes are committed and pushed"
echo "  2. Update version in gradle.properties if needed"
echo "  3. Create and push a version tag: git tag v$VERSION && git push origin v$VERSION"
echo "  4. Monitor the workflow at: https://github.com/xcutiboo/MythicRod/actions"
echo "  5. Download the release from: https://github.com/xcutiboo/MythicRod/releases"
echo ""
echo "Test artifacts available at: $ARTIFACT_DIR"
