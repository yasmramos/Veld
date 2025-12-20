#!/bin/bash

# Veld Framework - Maven Central Deployment Script
# This script deploys all Veld modules to Maven Central

set -e

echo "🚀 Veld Framework - Maven Central Deployment"
echo "=============================================="

# Check if GPG key is configured
if [ -z "$GPG_KEYNAME" ]; then
    echo "⚠️  GPG_KEYNAME not set. Setting to default..."
    export GPG_KEYNAME=7ED0F874FB5F110ED4A79D1BBE6F8F8910A7EE99
fi

echo "📋 Configuration:"
echo "   GPG Key ID: $GPG_KEYNAME"
echo "   Project Version: $(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)"
echo ""

# Check if settings.xml exists
if [ ! -f ~/.m2/settings.xml ]; then
    echo "❌ Error: ~/.m2/settings.xml not found!"
    echo "   Please create it with OSSRH credentials (see DEPLOYMENT.md)"
    exit 1
fi

echo "✅ Maven settings.xml found"

# Verify GPG key
echo "🔐 Verifying GPG key..."
if gpg --list-keys $GPG_KEYNAME > /dev/null 2>&1; then
    echo "   ✅ GPG key $GPG_KEYNAME found"
else
    echo "   ❌ GPG key $GPG_KEYNAME not found!"
    echo "   Please import the GPG keys from the provided ZIP file"
    exit 1
fi

# Test GPG signing
echo "🧪 Testing GPG signing..."
if echo "test" | gpg --clear-sign --armor --local-user $GPG_KEYNAME > /dev/null 2>&1; then
    echo "   ✅ GPG signing test passed"
else
    echo "   ❌ GPG signing test failed!"
    exit 1
fi

echo ""
echo "🎯 Starting deployment to Maven Central..."
echo "   This will:"
echo "   1. Build all Veld modules"
echo "   2. Generate JARs, sources, and javadocs"
echo "   3. Sign artifacts with GPG"
echo "   4. Upload to OSSRH staging repository"
echo "   5. Close and release the staging repository"
echo ""

# Perform deployment
mvn clean deploy -DskipTests -Dgpg.keyname=$GPG_KEYNAME

echo ""
echo "🎉 Deployment completed successfully!"
echo "   Please check the OSSRH Nexus Repository Manager for staging status:"
echo "   https://s01.oss.sonatype.org/"
echo ""
echo "   Expected timeline:"
echo "   - Staging to Release: ~10 minutes"
echo "   - Maven Central Sync: ~2 hours (up to 24 hours)"
echo "   - Search Index: ~4 hours"