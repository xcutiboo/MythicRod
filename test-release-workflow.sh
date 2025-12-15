#!/bin/bash
# Test script to verify the release workflow configuration
# This script checks all components needed for successful releases

set -e

echo "================================================"
echo "MythicRod Release Workflow Verification"
echo "================================================"
echo ""

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0
WARNING=0

check_pass() {
    echo -e "${GREEN}✓${NC} $1"
    PASSED=$((PASSED + 1))
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    FAILED=$((FAILED + 1))
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
    WARNING=$((WARNING + 1))
}

# Test 1: Check if workflow file exists
echo "Test 1: Workflow File Existence"
echo "--------------------------------"
if [ -f ".github/workflows/build.yml" ]; then
    check_pass "Workflow file exists at .github/workflows/build.yml"
else
    check_fail "Workflow file not found"
fi
echo ""

# Test 2: Verify workflow syntax
echo "Test 2: Workflow Syntax Validation"
echo "-----------------------------------"
if command -v yq &> /dev/null; then
    if yq eval '.jobs.build' .github/workflows/build.yml > /dev/null 2>&1; then
        check_pass "Build job is properly defined"
    else
        check_fail "Build job is not properly defined"
    fi
    
    if yq eval '.jobs.release' .github/workflows/build.yml > /dev/null 2>&1; then
        check_pass "Release job is properly defined"
    else
        check_fail "Release job is not properly defined"
    fi
else
    check_warn "yq not installed, skipping YAML validation"
fi
echo ""

# Test 3: Check Gradle configuration
echo "Test 3: Build Configuration"
echo "----------------------------"
if [ -f "build.gradle" ]; then
    check_pass "build.gradle exists"
    
    if grep -q "version = " build.gradle; then
        VERSION=$(grep "version = " build.gradle | head -1)
        check_pass "Version is configured: $VERSION"
    else
        check_warn "Version not found in build.gradle"
    fi
else
    check_fail "build.gradle not found"
fi

if [ -f "gradle.properties" ]; then
    check_pass "gradle.properties exists"
    if grep -q "version=" gradle.properties; then
        VERSION=$(grep "version=" gradle.properties)
        check_pass "Version is defined: $VERSION"
    fi
else
    check_warn "gradle.properties not found"
fi
echo ""

# Test 4: Check Gradle wrapper
echo "Test 4: Gradle Wrapper"
echo "----------------------"
if [ -f "gradlew" ]; then
    check_pass "Gradle wrapper exists"
    
    if [ -x "gradlew" ]; then
        check_pass "Gradle wrapper is executable"
    else
        check_warn "Gradle wrapper is not executable (will be fixed by workflow)"
    fi
else
    check_fail "Gradle wrapper not found"
fi
echo ""

# Test 5: Verify workflow triggers
echo "Test 5: Workflow Triggers"
echo "-------------------------"
if grep -q "tags: \['v\*'\]" .github/workflows/build.yml; then
    check_pass "Tag trigger is configured for releases (v*)"
else
    check_fail "Tag trigger not properly configured"
fi

if grep -q "branches: \['main', 'master'\]" .github/workflows/build.yml; then
    check_pass "Branch triggers are configured (main, master)"
else
    check_warn "Branch triggers may not be properly configured"
fi

if grep -q "workflow_dispatch:" .github/workflows/build.yml; then
    check_pass "Manual workflow dispatch is enabled"
else
    check_warn "Manual workflow dispatch not enabled"
fi
echo ""

# Test 6: Check release job configuration
echo "Test 6: Release Job Configuration"
echo "----------------------------------"
if grep -q "startsWith(github.ref, 'refs/tags/v')" .github/workflows/build.yml; then
    check_pass "Release job has correct tag condition"
else
    check_fail "Release job tag condition not found"
fi

if grep -q "contents: write" .github/workflows/build.yml; then
    check_pass "Release job has write permissions"
else
    check_fail "Release job missing required permissions"
fi

if grep -q "softprops/action-gh-release" .github/workflows/build.yml; then
    check_pass "Using softprops/action-gh-release for releases"
else
    check_fail "Release action not properly configured"
fi
echo ""

# Test 7: Verify artifact configuration
echo "Test 7: Artifact Configuration"
echo "-------------------------------"
if grep -q "upload-artifact@v4" .github/workflows/build.yml; then
    check_pass "Artifact upload action is configured"
else
    check_warn "Artifact upload action not found or outdated"
fi

if grep -q "build/libs/\*\.jar" .github/workflows/build.yml; then
    check_pass "JAR file path is correctly configured"
else
    check_fail "JAR file path not properly configured"
fi

if grep -q "!build/libs/\*-plain.jar" .github/workflows/build.yml; then
    check_pass "Excluding plain JAR files"
else
    check_warn "Plain JAR exclusion not configured"
fi
echo ""

# Test 8: Check Java version
echo "Test 8: Java Version Configuration"
echo "-----------------------------------"
if grep -q "java-version: '21'" .github/workflows/build.yml; then
    check_pass "Java 21 is configured in workflow"
else
    check_warn "Java version may not be properly configured"
fi

if grep -q "targetJavaVersion = 21" build.gradle; then
    check_pass "Java 21 is configured in build.gradle"
else
    check_warn "Java target version not found in build.gradle"
fi
echo ""

# Test 9: Verify source code structure
echo "Test 9: Source Code Structure"
echo "------------------------------"
if [ -d "src/main/java" ]; then
    check_pass "Java source directory exists"
    JAVA_FILES=$(find src/main/java -name "*.java" | wc -l)
    if [ "$JAVA_FILES" -gt 0 ]; then
        check_pass "Found $JAVA_FILES Java source files"
    else
        check_fail "No Java source files found"
    fi
else
    check_fail "Java source directory not found"
fi

if [ -d "src/main/resources" ]; then
    check_pass "Resources directory exists"
else
    check_warn "Resources directory not found"
fi
echo ""

# Test 10: Check for plugin metadata
echo "Test 10: Plugin Metadata"
echo "------------------------"
if [ -f "src/main/resources/plugin.yml" ] || [ -f "src/main/resources/paper-plugin.yml" ]; then
    check_pass "Plugin metadata file exists"
else
    check_warn "Plugin metadata file not found"
fi
echo ""

# Summary
echo "================================================"
echo "Test Summary"
echo "================================================"
echo -e "${GREEN}Passed: $PASSED${NC}"
if [ $WARNING -gt 0 ]; then
    echo -e "${YELLOW}Warnings: $WARNING${NC}"
fi
if [ $FAILED -gt 0 ]; then
    echo -e "${RED}Failed: $FAILED${NC}"
fi
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ Release workflow is properly configured!${NC}"
    echo ""
    echo "To create a release:"
    echo "  1. Create and push a tag: git tag v1.0.1 && git push origin v1.0.1"
    echo "  2. The workflow will automatically:"
    echo "     - Build the plugin"
    echo "     - Run tests"
    echo "     - Create a GitHub release"
    echo "     - Upload the JAR file"
    echo "     - Generate release notes"
    exit 0
else
    echo -e "${RED}✗ Some checks failed. Please fix the issues above.${NC}"
    exit 1
fi
