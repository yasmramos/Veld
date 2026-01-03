# Veld Framework - Deployment Guide

This guide covers how to deploy the Veld Framework to Maven Central and GitHub Packages.

## Prerequisites

### 1. Maven Settings Configuration

Copy the template and configure your credentials:

```bash
cp settings.xml.template ~/.m2/settings.xml
# Edit ~/.m2/settings.xml with your credentials
```

**Required credentials:**
- **Maven Central (OSSRH)**: Get credentials at https://oss.sonatype.org/
- **GitHub Packages**: Create a Personal Access Token at https://github.com/settings/tokens with `read:packages` and `write:packages` scopes

### 2. GPG Signing (for Maven Central)

If deploying to Maven Central, you need GPG signing:
```bash
# Install GPG if not already installed
gpg --gen-key  # Create a key pair
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID  # Publish your public key
```

## Deployment Commands

### Deploy to Maven Central (OSSRH)

```bash
# Make sure to use the correct Java and Maven
export JAVA_HOME=/path/to/jdk-17
export MAVEN_HOME=/path/to/maven

# Clean, build with tests, and deploy (includes javadoc and sources)
mvn clean deploy

# Or run tests first
mvn clean verify deploy
```

### Deploy to GitHub Packages

```bash
export JAVA_HOME=/path/to/jdk-17
export MAVEN_HOME=/path/to/maven

# Deploy to GitHub Packages using the 'github' profile
mvn clean deploy -Pgithub
```

### Deploy to Both

You can deploy to both repositories by having both server configurations in `settings.xml` and running:

```bash
# Maven Central
mvn clean deploy

# GitHub Packages
mvn clean deploy -Pgithub
```

## Tagging a Release

For a new release:

```bash
# Create a release tag
git tag -a v1.0.0 -m "Release 1.0.0"

# Push the tag
git push origin v1.0.0

# Then run the deploy command
mvn clean deploy
```

## Troubleshooting

### "Failed to install artifact" errors

Make sure you're not using the CI profile (`-Pci`) when deploying:
- ❌ Wrong: `mvn clean deploy -Pci`
- ✅ Correct: `mvn clean deploy`

### GPG signing errors

```bash
# Export your GPG key
gpg --export-secret-keys -a YOUR_KEY_ID > private.key
gpg --import private.key
```

### Authentication errors

Verify your `~/.m2/settings.xml` has the correct server IDs matching those in `pom.xml`:
- `central` for Maven Central
- `github` for GitHub Packages

## CI/CD with GitHub Actions

Example workflow for GitHub Packages deployment:

```yaml
name: Deploy to GitHub Packages
on:
  release:
    types: [created]

jobs:
  deploy:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build and Deploy
        run: mvn clean deploy
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Project Information

- **GroupId**: `io.github.yasmramos`
- **ArtifactId**: See individual module pom.xml files
- **Version**: Current version is `1.0.3` (see `pom.xml`)
