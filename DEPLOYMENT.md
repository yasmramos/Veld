# Veld Framework - Maven Central Deployment Guide

## Prerequisites

1. **Sonatype OSSRH Account**: You need a Sonatype account and your project must be approved for Maven Central
2. **GPG Key**: Generate a GPG key for signing artifacts
3. **Maven Settings**: Configured with OSSRH credentials

## Configuration Files

### 1. settings.xml
The project includes a `settings.xml` file with OSSRH server configuration:
- **Server ID**: `ossrh`
- **Username**: `UsC4b3`
- **Password**: `GoRNnkUIedGnrN72dm08EM3pHM05P70kT`

### 2. pom.xml Configuration
The parent pom.xml includes:
- ✅ Distribution management for OSSRH
- ✅ Source JAR generation
- ✅ Javadoc JAR generation
- ✅ GPG signing configuration
- ✅ Nexus Staging plugin enabled

## Deployment Steps

### 1. Verify GPG Configuration

Make sure you have a GPG key configured:
```bash
# List available keys
gpg --list-keys

# Set GPG keyname environment variable
export GPG_KEYNAME=your-key-id
```

### 2. Clean and Test Build
```bash
# Clean build with tests
mvn clean test

# Generate all artifacts without GPG signing (for testing)
mvn clean install -DskipGpg=true
```

### 3. Deploy to OSSRH Staging
```bash
# Deploy to staging repository
mvn clean deploy -DskipTests

# This will:
# 1. Build all modules
# 2. Generate JARs, sources, javadocs
# 3. Sign artifacts with GPG
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