# GitHub Actions Deployment Configuration

This document explains how to configure GitHub Actions for automated deployment of Veld Framework to Maven Central.

## 🔐 Required GitHub Secrets

To enable automated deployment, you need to configure the following secrets in your GitHub repository:

### Navigate to Repository Settings
1. Go to your GitHub repository
2. Click on **Settings** tab
3. In the left sidebar, click **Secrets and variables**
4. Click **Actions**

### Add the Following Secrets

#### 1. SONATYPE_USERNAME
- **Value**: Your Sonatype Nexus username (usually your email)
- **Description**: Username for Sonatype Nexus deployment

#### 2. SONATYPE_TOKEN
- **Value**: Sonatype Nexus API token
- **Description**: Token for Sonatype Nexus deployment
- **How to get**: 
  - Login to https://s01.oss.sonatype.org/
  - Go to Profile → Token → Access Token
  - Generate a new token with deployment permissions

#### 3. GPG_PRIVATE_KEY
- **Value**: Your GPG private key (base64 encoded)
- **Description**: Private GPG key for signing artifacts
- **How to get**:
  ```bash
  # Export your private key
  gpg --armor --export-secret-keys YOUR_KEY_ID | base64
  ```

#### 4. GPG_PASSPHRASE
- **Value**: The passphrase for your GPG private key
- **Description**: Passphrase to unlock the GPG key

#### 5. GPG_KEYNAME
- **Value**: Your GPG key ID (e.g., ABCD1234)
- **Description**: The key ID of your GPG key

## 📋 Step-by-Step Setup

### 1. Sonatype Nexus Account Setup

1. **Create Sonatype Account**:
   - Go to https://s01.oss.sonatype.org/
   - Click "Sign Up"
   - Complete registration process

2. **Create a New Project**:
   - Go to "Create" → "New Repository"
   - Fill in the form:
     - Group ID: `io.github.yasmramos`
     - Project URL: `https://github.com/yasmramos/Veld`
     - SCM URL: `https://github.com/yasmramos/Veld.git`
   - Wait for Sonatype to approve your group ID (usually automatic for GitHub domains)

3. **Generate Deployment Token**:
   - Login to Sonatype Nexus
   - Click on your profile icon
   - Select "Token" → "Access Token"
   - Click "Generate Token"
   - Copy the token for `SONATYPE_TOKEN`

### 2. GPG Key Setup

1. **Generate GPG Key** (if you don't have one):
   ```bash
   gpg --full-generate-key
   # Choose RSA and RSA, 4096 bits, no expiration
   # Enter your name and email
   ```

2. **Export GPG Key**:
   ```bash
   # List your keys
   gpg --list-keys
   
   # Export private key (replace ABCD1234 with your key ID)
   gpg --armor --export-secret-keys ABCD1234 | base64
   ```

3. **Upload GPG Key to Key Server** (recommended):
   ```bash
   gpg --send-keys ABCD1234
   ```

### 3. GitHub Secrets Configuration

1. **Add SONATYPE_USERNAME**:
   - Name: `SONATYPE_USERNAME`
   - Value: Your Sonatype username (email)

2. **Add SONATYPE_TOKEN**:
   - Name: `SONATYPE_TOKEN`
   - Value: The token generated from Sonatype

3. **Add GPG_PRIVATE_KEY**:
   - Name: `GPG_PRIVATE_KEY`
   - Value: Base64-encoded private key

4. **Add GPG_PASSPHRASE**:
   - Name: `GPG_PASSPHRASE`
   - Value: Your GPG key passphrase

5. **Add GPG_KEYNAME**:
   - Name: `GPG_KEYNAME`
   - Value: Your GPG key ID (e.g., ABCD1234)

## 🚀 Workflows Overview

### 1. validate.yml
**Triggers**: Push to main, Pull requests, Manual trigger

**Purpose**: Validates that the project is ready for release
- Checks project structure
- Validates compilation
- Runs all tests
- Generates Javadoc
- Validates documentation
- Checks benchmarks
- Creates validation report

### 2. release.yml
**Triggers**: Git tag push (v*), Release published

**Purpose**: Deploys to Sonatype Nexus staging repository
- Extracts version from tag
- Runs validation checks
- Builds and tests the project
- Deploys to staging repository
- Creates deployment summary

### 3. deploy.yml
**Triggers**: Manual trigger, Release published

**Purpose**: Manual deployment to Maven Central
- Allows manual environment selection (staging/production)
- Deploys to Sonatype Nexus
- Provides deployment instructions

## 🎯 Usage Instructions

### For Automated Release

1. **Create Release Tag**:
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

2. **Or Create GitHub Release**:
   - Go to your repository
   - Click "Releases"
   - Click "Create a new release"
   - Tag version: `v1.0.0`
   - Title: `Release v1.0.0`
   - Publish release

3. **Monitor Deployment**:
   - Go to Actions tab
   - Watch the "Release to Maven Central" workflow
   - Check deployment summary

### For Manual Deployment

1. **Go to Actions Tab**:
   - Repository → Actions
   - Select "Deploy to Maven Central"

2. **Run Workflow**:
   - Click "Run workflow"
   - Choose environment (staging/production)
   - Click "Run workflow"

3. **Monitor Progress**:
   - Watch the workflow execution
   - Check the deployment summary

## 🔍 Post-Deployment Steps

After successful deployment to Sonatype Nexus:

1. **Login to Sonatype Nexus**:
   - Go to https://s01.oss.sonatype.org/
   - Login with your credentials

2. **Close Staging Repository**:
   - Go to "Staging Repositories"
   - Find the repository for Veld
   - Click "Close" to validate artifacts

3. **Release to   - After Maven Central**:
 closing, click "Release"
   - Confirm the release

4. **Wait for Synchronization**:
   - Maven Central sync takes 2-24 hours
   - Check search.maven.org for your artifacts

## 🛠️ Troubleshooting

### Common Issues

#### GPG Key Issues
- **Problem**: "gpg: signing failed: Inappropriate ioctl for device"
- **Solution**: Configure GPG to work in non-interactive mode:
  ```bash
  echo "pinentry-mode loopback" >> ~/.gnupg/gpg.conf
  echo "allow-loopback-pinentry" >> ~/.gnupg/gpg-agent.conf
  ```

#### Sonatype Authentication Issues
- **Problem**: "401 Unauthorized"
- **Solution**: Verify your username and token are correct

#### Test Failures
- **Problem**: Deployment fails due to test failures
- **Solution**: Fix failing tests before deployment

#### Javadoc Errors
- **Problem**: "Javadoc generation failed"
- **Solution**: Ensure all public classes have proper Javadoc

### Debugging

1. **Check Workflow Logs**:
   - Go to Actions tab
   - Click on the failed workflow
   - Expand failed steps to see error details

2. **Manual Verification**:
   ```bash
   # Test local build
   mvn clean deploy -DskipTests
   
   # Check generated artifacts
   ls -la */target/*.jar
   ```

3. **Verify GPG Key**:
   ```bash
   # Import and list key
   echo "YOUR_BASE64_KEY" | base64 -d | gpg --import
   gpg --list-keys
   ```

## 📊 Success Metrics

After successful deployment, you should see:
- ✅ All validation checks pass
- ✅ Deployment to Sonatype Nexus succeeds
- ✅ Staging repository closes successfully
- ✅ Release to Maven Central completes
- ✅ Artifacts appear on search.maven.org within 24 hours

## 🔒 Security Best Practices

1. **Rotate Secrets Regularly**: Update Sonatype tokens and GPG keys periodically
2. **Use Least Privilege**: Only grant necessary permissions to deployment tokens
3. **Monitor Deployments**: Regularly check deployment history for anomalies
4. **Backup Keys**: Keep secure backups of GPG keys and important secrets

---

**Need Help?**
- Sonatype Documentation: https://help.sonatype.com/docs
- GitHub Actions Documentation: https://docs.github.com/en/actions
- Maven Deployment Guide: https://maven.apache.org/repository/guide-distribution.html