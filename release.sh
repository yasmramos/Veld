#!/bin/bash

# Veld Release Script
# Automates the release process to Maven Central
# Usage: ./release.sh <version> <next-snapshot-version>

set -e

VERSION=${1:-"1.0.0"}
NEXT_SNAPSHOT=${2:-"1.0.1-SNAPSHOT"}

echo "🚀 Veld Release Script"
echo "======================"
echo "Release Version: $VERSION"
echo "Next Snapshot: $NEXT_SNAPSHOT"
echo ""

# Validate arguments
if [ -z "$VERSION" ] || [ -z "$NEXT_SNAPSHOT" ]; then
    echo "❌ Error: Usage: $0 <version> <next-snapshot-version>"
    echo "Example: $0 1.0.0 1.0.1-SNAPSHOT"
    exit 1
fi

# Check if we're on main branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "❌ Error: Must be on 'main' branch to release. Current branch: $CURRENT_BRANCH"
    exit 1
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo "❌ Error: Uncommitted changes detected. Please commit or stash changes first."
    exit 1
fi

# Check if version already exists as tag
if git tag | grep -q "^v$VERSION$"; then
    echo "❌ Error: Version v$VERSION already exists as tag"
    exit 1
fi

# Verify environment variables for Sonatype OSSRH
echo "🔍 Checking environment variables..."
if [ -z "$OSSRH_USERNAME" ] || [ -z "$OSSRH_PASSWORD" ]; then
    echo "⚠️  Warning: OSSRH_USERNAME or OSSRH_PASSWORD not set"
    echo "   Set these environment variables for Maven Central deployment:"
    echo "   export OSSRH_USERNAME=your_username"
    echo "   export OSSRH_PASSWORD=your_password"
    echo ""
    echo "Press Enter to continue anyway (GitHub Packages deployment only), or Ctrl+C to abort..."
    read -r
fi

# Check GPG configuration
if [ -z "$GPG_KEYNAME" ]; then
    echo "⚠️  Warning: GPG_KEYNAME not set"
    echo "   Set this environment variable for artifact signing:"
    echo "   export GPG_KEYNAME=your_gpg_key_id"
    echo ""
fi

if [ -z "$GPG_PASSPHRASE" ]; then
    echo "⚠️  Warning: GPG_PASSPHRASE not set"
    echo "   Set this environment variable for GPG signing:"
    echo "   export GPG_PASSPHRASE=your_gpg_passphrase"
    echo ""
fi

# Clean previous builds
echo "🧹 Cleaning previous builds..."
mvn clean

# Run tests
echo "🧪 Running tests..."
mvn test -q

# Check test coverage
echo "📊 Checking test coverage..."
mvn jacoco:check

# Create release
echo "📦 Creating release v$VERSION..."
mvn versions:set -DnewVersion=$VERSION -q

# Deploy to staging repository
echo "🚀 Deploying to staging repository..."
mvn deploy -DskipTests=false -DskipStaging=false

# Create and push tag
echo "🏷️  Creating tag v$VERSION..."
git add -A
git commit -m "Release version $VERSION"
git tag -a "v$VERSION" -m "Release version $VERSION"

# Update to next snapshot
echo "🔄 Updating to next snapshot version $NEXT_SNAPSHOT..."
mvn versions:set -DnewVersion=$NEXT_SNAPSHOT -q
git add -A
git commit -m "Bump version to $NEXT_SNAPSHOT for next development cycle"

# Push changes
echo "📤 Pushing changes to remote..."
git push origin main
git push origin v$VERSION

echo ""
echo "✅ Release completed successfully!"
echo ""
echo "Next steps:"
echo "1. Go to https://s01.oss.sonatype.org/#stagingRepositories"
echo "2. Close and release the staging repository"
echo "3. Wait for Maven Central sync (up to 24 hours)"
echo ""
echo "Release: https://github.com/yasmramos/Veld/releases/tag/v$VERSION"
echo "Maven Central: https://repo1.maven.org/maven2/com/veld/veld-parent/$VERSION/"
echo ""