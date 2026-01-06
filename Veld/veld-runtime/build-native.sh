#!/bin/bash
#
# Copyright 2025 Veld Framework
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# GraalVM Native Image Build Script for Veld Framework
#
# This script builds a native executable of the Veld Framework runtime,
# providing near-instant startup and minimal memory footprint.
#
# Prerequisites:
#   1. Install GraalVM JDK 17 or later
#   2. Install native-image component: gu install native-image
#   3. Build the project: mvn clean package -DskipTests
#
# Usage:
#   ./build-native.sh [--without-jni] [--enable-all-security-services]
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Veld Framework - Native Image Builder${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if GraalVM is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed${NC}"
    exit 1
fi

# Check Java vendor
JAVA_VENDOR=$(java -XshowSettings:all -version 2>&1 | grep "java.vendor" | cut -d'=' -f2 | tr -d ' ')
if [[ ! "$JAVA_VENDOR" == *"GraalVM"* ]]; then
    echo -e "${YELLOW}Warning: Java vendor is '$JAVA_VENDOR', not GraalVM${NC}"
    echo -e "${YELLOW}For best results, use GraalVM JDK 17 or later${NC}"
    echo ""
fi

# Check if native-image is installed
if ! command -v native-image &> /dev/null; then
    echo -e "${YELLOW}native-image not found. Installing...${NC}"
    if command -v gu &> /dev/null; then
        gu install native-image
    else
        echo -e "${RED}Error: 'gu' command not found. Please install GraalVM first.${NC}"
        echo "Download from: https://www.graalvm.org/downloads/"
        exit 1
    fi
fi

# Change to project directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Build the project if not already built
if [ ! -d "target" ]; then
    echo -e "${YELLOW}Building project...${NC}"
    mvn clean package -DskipTests
    echo ""
fi

# Find the JAR file
JAR_FILE=$(find target -name "veld-runtime-*.jar" -type f ! -name "*sources*" ! -name "*javadoc*" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}Error: JAR file not found in target directory${NC}"
    exit 1
fi

echo -e "${GREEN}Found JAR file: $JAR_FILE${NC}"
echo ""

# Parse arguments
WITHOUT_JNI=false
ENABLE_SECURITY=false

for arg in "$@"; do
    case $arg in
        --without-jni)
            WITHOUT_JNI=true
            ;;
        --enable-all-security-services)
            ENABLE_SECURITY=true
            ;;
    esac
done

# Build native image
echo -e "${GREEN}Building native image...${NC}"
echo ""

# Base command
NATIVE_IMAGE_CMD="native-image \
    -jar $JAR_FILE \
    -H:Name=veld-runtime \
    -H:ClassName=io.github.yasmramos.veld.VeldApplication \
    --no-fallback \
    -H:+ReportExceptionStackTraces \
    -H:ConfigurationFileDirectories=src/main/resources/META-INF/native-image \
    -Djava.awt.headless=true"

# Add options based on arguments
if [ "$WITHOUT_JNI" = true ]; then
    NATIVE_IMAGE_CMD="$NATIVE_IMAGE_CMD --no-jni"
fi

if [ "$ENABLE_SECURITY" = true ]; then
    NATIVE_IMAGE_CMD="$NATIVE_IMAGE_CMD --enable-all-security-services"
fi

# Execute the build command
eval $NATIVE_IMAGE_CMD

echo ""
if [ -f "veld-runtime" ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Native image built successfully!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "Executable: ./veld-runtime"
    echo -e "Size: $(du -h veld-runtime | cut -f1)"
    echo ""
    echo -e "${YELLOW}To run:${NC}"
    echo "  ./veld-runtime"
    echo ""
else
    echo -e "${RED}Error: Native image build failed${NC}"
    exit 1
fi
