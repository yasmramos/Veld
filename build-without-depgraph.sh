#!/bin/bash

# Veld Framework Build Script - Without Depgraph Plugin
# This script builds Veld while completely avoiding depgraph plugin issues

echo "=== VELD FRAMEWORK BUILD (DEPGRAH DISABLED) ==="

# Maven command with depgraph disabled
MAVEN_CMD="mvn"

# Disable depgraph through system properties
MAVEN_OPTS="-Ddepgraph.skip=true -Ddepgraph.failOnError=false"

# Build command
BUILD_CMD="$MAVEN_CMD clean install -DskipTests"

# Execute build
echo "Building Veld Framework with depgraph disabled..."
echo "Command: $BUILD_CMD"
echo ""

# Set environment and execute
export MAVEN_OPTS="$MAVEN_OPTS"
eval "$BUILD_CMD"

# Check result
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL - Veld Framework built without depgraph issues"
else
    echo ""
    echo "❌ BUILD FAILED - Check logs for details"
    exit 1
fi