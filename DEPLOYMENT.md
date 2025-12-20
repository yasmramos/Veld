# Veld Framework - Maven Central Deployment Guide

## Prerequisites

1. **Sonatype OSSRH Account**: You need a Sonatype account and your project must be approved for Maven Central
2. **GPG Key**: ✅ Already configured and propagated to servers
   - **Key ID**: `7ED0F874FB5F110ED4A79D1BBE6F8F8910A7EE99`
   - **Owner**: Yasmany Ramos Garcia <yasmramos95@gmail.com>
   - **Expires**: 2027-12-14
3. **Maven Settings**: Configured with OSSRH credentials

## Configuration Files

### 1. settings.xml
Create a local `settings.xml` file in your Maven user directory (typically `~/.m2/settings.xml`) with OSSRH server configuration:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <!-- Maven Central OSSRH Server Configuration -->
    <servers>
        <server>
            <id>ossrh</id>
            <username>UsC4b3</username>
            <password>GoRNnkUIedGnrN72dm08EM3pHM05P70kT</password>
        </server>
    </servers>
    
    <!-- Plugin Groups -->
    <pluginGroups>
        <pluginGroup>org.sonatype.plugins</pluginGroup>
    </pluginGroups>
    
</settings>
```

**Location**: `~/.m2/settings.xml` (your Maven user directory)
**⚠️ Security**: Keep this file secure and never commit it to version control

### 2. pom.xml Configuration
The parent pom.xml includes:
- ✅ Distribution management for OSSRH
- ✅ Source JAR generation
- ✅ Javadoc JAR generation
- ✅ GPG signing configuration
- ✅ Nexus Staging plugin enabled
- ✅ **Maven Profiles** for different environments:
  - `ci` profile: Skips GPG signing and deployment for CI builds
  - `benchmark` profile: Optimized for performance testing

### 3. Maven Profiles Available

#### CI Profile (`-Pci`)
Used for continuous integration builds:
```bash
mvn clean install -Pci
```
**Skips**: GPG signing, deployment, staging

#### Benchmark Profile (`-Pbenchmark`)
Used for performance testing:
```bash
mvn clean install -Pbenchmark
```
**Skips**: GPG signing, deployment, source JARs, Javadoc JARs

#### Default Profile (Release)
Used for production releases:
```bash
mvn clean deploy
```
**Includes**: GPG signing, deployment, all JARs

## Deployment Steps

### 1. Verify GPG Configuration

✅ **GPG keys are already imported and propagated!** 

```bash
# Verify GPG key is available
gpg --list-keys

# Expected output should show:
# pub   rsa4096 2025-12-14 [SCEA] [expires: 2027-12-14]
#       7ED0F874FB5F110ED4A79D1BBE6F8F8910A7EE99
# uid           [ unknown] Yasmany Ramos Garcia <yasmramos95@gmail.com>

# Set GPG keyname environment variable
export GPG_KEYNAME=7ED0F874FB5F110ED4A79D1BBE6F8F8910A7EE99

# Test GPG signing (optional)
echo "test" | gpg --clear-sign --armor --local-user $GPG_KEYNAME
```

### 2. Clean and Test Build
```bash
# Clean build with tests
mvn clean test

# Generate all artifacts without GPG signing (for testing)
mvn clean install -DskipGpg=true

# Build for CI/Benchmark (skip GPG and deployment)
mvn clean install -Pci

# Build specifically for benchmarks
mvn clean install -Pbenchmark
```

### 3. Deploy to OSSRH Staging

**Option A: Using the automated script (Recommended)**
```bash
# Make script executable (if needed)
chmod +x deploy.sh

# Run deployment script
./deploy.sh
```

**Option B: Manual deployment**
```bash
# Export GPG key ID
export GPG_KEYNAME=7ED0F874FB5F110ED4A79D1BBE6F8F8910A7EE99

# Deploy to staging repository
mvn clean deploy -DskipTests -Dgpg.keyname=$GPG_KEYNAME

# This will:
# 1. Build all modules
# 2. Generate JARs, sources, javadocs
# 3. Sign artifacts with GPG using key ID
# 4. Upload to OSSRH staging repository
# 5. Close and release the staging repository
```

### 4. Manual Release (if needed)
If automatic release fails, you can manually release:

1. Go to [OSSRH Nexus Repository Manager](https://s01.oss.sonatype.org/)
2. Login with your OSSRH credentials
3. Navigate to "Staging Repositories"
4. Find your staging repository
5. Click "Release" button

### 5. Wait for Maven Central Sync
After release, it takes:
- **Staging to Release**: ~10 minutes
- **Maven Central Sync**: ~2 hours (can take up to 24 hours)
- **Search Index**: ~4 hours

## Verification

### Check if artifacts are available:
```bash
# Test dependency resolution
mvn dependency:get -Dartifact=io.github.yasmramos:veld-runtime:1.0.0

# Or check Maven Central directly
curl -s "https://repo1.maven.org/maven2/io/github/yasmramos/veld-runtime/maven-metadata.xml"
```

## Troubleshooting

### GPG Issues
- Ensure GPG key is properly configured
- Set `GPG_KEYNAME` environment variable
- Check GPG agent is running

### Build Failures
- Ensure all tests pass: `mvn test`
- Check for missing dependencies
- Verify Java version compatibility (Java 11+)

### OSSRH Issues
- Check credentials in `settings.xml`
- Verify project is approved for Maven Central
- Check staging repository status in OSSRH

## Project Structure

The project includes these modules for Maven Central:
- `veld-annotations` - Core annotations
- `veld-runtime` - Runtime container
- `veld-processor` - Annotation processor
- `veld-weaver` - Bytecode weaver
- `veld-aop` - AOP support
- `veld-maven-plugin` - Maven plugin
- `veld-spring-boot-starter` - Spring Boot integration

## Notes

- Version `1.0.0` is a release version (not SNAPSHOT)
- All modules are configured for Maven Central publication
- The deployment will create a staging repository that needs to be manually released if auto-release fails
- GPG signing is required for Maven Central