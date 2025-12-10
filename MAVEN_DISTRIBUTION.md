# 🚀 Veld Maven Distribution Guide

This guide explains how to use Veld Framework from Maven Central and how to release new versions.

## 📦 Using Veld from Maven Central

### Add Dependencies

Add Veld to your `pom.xml`:

```xml
<dependencies>
    <!-- Core Veld Framework -->
    <dependency>
        <groupId>com.veld</groupId>
        <artifactId>veld-annotations</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Runtime utilities -->
    <dependency>
        <groupId>com.veld</groupId>
        <artifactId>veld-runtime</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### Add Maven Plugin

For annotation processing:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.veld</groupId>
            <artifactId>veld-maven-plugin</artifactId>
            <version>1.0.0</version>
            <extensions>true</extensions>
        </plugin>
    </plugins>
</build>
```

### Gradle Support

```gradle
dependencies {
    implementation 'com.veld:veld-annotations:1.0.0'
    implementation 'com.veld:veld-runtime:1.0.0'
    annotationProcessor 'com.veld:veld-maven-plugin:1.0.0'
}
```

## 🔧 Development Setup

### Prerequisites

1. **Java 17+** - Required for building
2. **Maven 3.6+** - For building and releasing
3. **GPG** - For signing artifacts (for releases)
4. **Sonatype OSSRH account** - For Maven Central publication

### Build Commands

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Generate Javadoc
mvn javadoc:javadoc

# Build without tests (faster builds)
mvn clean compile -DskipTests

# Generate assembly
mvn assembly:single
```

### Environment Variables

Set these for releases:

```bash
# Sonatype OSSRH credentials
export OSSRH_USERNAME=your_sonatype_username
export OSSRH_PASSWORD=your_sonatype_password

# GPG signing
export GPG_KEYNAME=your_gpg_key_id
export GPG_PASSPHRASE=your_gpg_passphrase

# For GitHub Actions
export GITHUB_TOKEN=your_github_token
```

## 🎯 Release Process

### Automated Release (Recommended)

1. **Create Release on GitHub**
   - Go to https://github.com/yasmramos/Veld/releases
   - Click "Create a new release"
   - Tag version: `v1.0.0`
   - Release title: `Veld Framework v1.0.0`
   - Description: Include release notes

2. **GitHub Actions will automatically:**
   - Build and test on multiple Java versions
   - Deploy to Maven Central
   - Create GitHub release with artifacts
   - Generate and upload Javadoc

### Manual Release

```bash
# Make sure you're on main branch with clean working directory
git checkout main
git pull origin main

# Run the release script
./release.sh 1.0.0 1.0.1-SNAPSHOT
```

The script will:
1. Clean and test the project
2. Set version to release version
3. Deploy to staging repository
4. Create and push git tag
5. Update to next snapshot version
6. Push all changes

### Manual Maven Central Release

```bash
# Set version
mvn versions:set -DnewVersion=1.0.0

# Deploy to staging
mvn clean deploy -DskipTests=false

# Close staging repository
# Go to https://s01.oss.sonatype.org/#stagingRepositories
# Close the staging repository

# Release to Maven Central
# Click "Release" in the staging repository
```

## 🔐 GPG Key Setup

### Generate GPG Key

```bash
# Run the GPG management script
./gpg-manager.sh generate

# Or generate manually
gpg --gen-key
```

### Configure GPG for Maven

```bash
# Export GPG key information
./gpg-manager.sh export <key_id>

# Add to GitHub: https://github.com/settings/keys
```

### GPG Best Practices

1. **Use a strong passphrase** for your GPG key
2. **Never commit private keys** to version control
3. **Store private key securely** (e.g., password manager)
4. **Backup your GPG key** before generating
5. **Use keyserver** for public key distribution

## 🏗️ Project Structure

```
Veld/
├── pom.xml                    # Parent POM with distribution config
├── maven-settings-template.xml # Maven settings template
├── release.sh                 # Automated release script
├── gpg-manager.sh            # GPG key management
├── .github/workflows/
│   └── ci-cd.yml             # CI/CD pipeline
└── veld-*/                   # Individual modules
    ├── pom.xml               # Module POMs
    ├── src/main/java/        # Source code
    └── src/test/java/        # Tests
```

## 📋 Release Checklist

### Before Release
- [ ] All tests pass
- [ ] Test coverage >= 10%
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version numbers consistent
- [ ] GPG key configured
- [ ] Sonatype OSSRH account active

### During Release
- [ ] Git branch: `main`
- [ ] Working directory: clean
- [ ] Environment variables set
- [ ] Network: stable connection
- [ ] Sufficient disk space

### After Release
- [ ] Staging repository closed
- [ ] Staging repository released
- [ ] Maven Central sync completed
- [ ] GitHub release created
- [ ] Documentation deployed
- [ ] Release announcement posted

## 🔗 Useful Links

### Maven Central
- **Search**: https://search.maven.org/artifact/com.veld/veld-annotations
- **Repository**: https://repo1.maven.org/maven2/com/veld/
- **Staging**: https://s01.oss.sonatype.org/#stagingRepositories

### GitHub
- **Repository**: https://github.com/yasmramos/Veld
- **Releases**: https://github.com/yasmramos/Veld/releases
- **GitHub Packages**: https://github.com/yasmramos/Veld/packages

### Documentation
- **GitHub Pages**: https://yasmramos.github.io/Veld
- **Javadoc**: Available in Maven artifacts

## 🛠️ Troubleshooting

### Build Failures

**Java Version Issues**
```bash
# Check Java version
java -version

# Set JAVA_HOME
export JAVA_HOME=/path/to/java17
```

**Maven Configuration**
```bash
# Validate Maven configuration
mvn validate

# Clear Maven cache
mvn dependency:purge-local-repository
```

### GPG Issues

**Key Not Found**
```bash
# List available keys
./gpg-manager.sh list

# Import key from file
./gpg-manager.sh import key.asc
```

**Passphrase Issues**
```bash
# Test GPG signing
echo "test" | gpg --clearsign

# Check GPG agent
gpgconf --list-components
```

### Maven Central Issues

**Deployment Failures**
1. Check OSSRH credentials
2. Verify GPG signing
3. Check artifact metadata
4. Review staging repository logs

**Sync Issues**
- Wait up to 24 hours for sync
- Check Maven Central index
- Verify repository URL

## 📊 CI/CD Status

Current CI/CD pipeline status:

| Job | Status | Description |
|-----|--------|-------------|
| Test | ✅ | Multi-Java version testing |
| Quality | ✅ | SonarQube & OWASP checks |
| Build | ✅ | Artifact generation |
| Deploy (GitHub) | ✅ | GitHub Packages deployment |
| Deploy (Maven Central) | ✅ | Maven Central publication |
| Documentation | ✅ | GitHub Pages deployment |

## 🤝 Contributing

To contribute to the release process:

1. **Fork the repository**
2. **Create feature branch**
3. **Make changes and test**
4. **Create pull request**
5. **CI/CD will handle deployment**

For release-related contributions, please:
- Follow semantic versioning
- Update CHANGELOG.md
- Test thoroughly before release
- Use the automated release process

## 📞 Support

For release issues:
1. Check this guide first
2. Review GitHub Actions logs
3. Check Sonatype staging repository
4. Open GitHub issue with details

---

**Last Updated**: 2025-12-11  
**Version**: 1.0.0  
**Maven Central**: https://search.maven.org/artifact/com.veld/veld-annotations