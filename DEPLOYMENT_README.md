# 🚀 Veld Framework Deployment Guide

This guide explains how to deploy Veld Framework to Maven Central using the automated GitHub Actions workflows.

## 📋 Quick Start

### Prerequisites
1. **Sonatype Account**: Create account at https://s01.oss.sonatype.org/
2. **GPG Key**: Generate GPG key for artifact signing
3. **GitHub Secrets**: Configure required secrets (see [Configuration Guide](./DEPLOYMENT_GUIDE.md))

### One-Command Release
```bash
# Create and push release tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

That's it! The release workflow will automatically:
- ✅ Validate the project
- ✅ Build and test all modules
- ✅ Deploy to Sonatype Nexus staging
- ✅ Generate deployment summary

## 🔄 Workflows Overview

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| **CI** | Push/PR | Quality checks, tests, security scans |
| **Validate** | Push/PR | Pre-release validation |
| **Release** | Git tag/release | Automated deployment to Maven Central |
| **Deploy** | Manual | Manual deployment with environment choice |

## 🎯 Deployment Methods

### Method 1: Automated Release (Recommended)
1. **Create Release Tag**:
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

2. **Monitor Deployment**:
   - Go to GitHub Actions tab
   - Watch "Release to Maven Central" workflow
   - Check deployment summary

3. **Complete Release**:
   - Login to https://s01.oss.sonatype.org/
   - Close staging repository
   - Release to Maven Central

### Method 2: GitHub Release
1. **Create Release**:
   - Go to GitHub repository
   - Click "Releases" → "Create a new release"
   - Tag version: `v1.0.0`
   - Publish release

2. **Follow automated deployment process**

### Method 3: Manual Deployment
1. **Trigger Workflow**:
   - Go to Actions tab
   - Select "Deploy to Maven Central"
   - Click "Run workflow"
   - Choose environment (staging/production)

## ⚙️ Configuration

### Required GitHub Secrets
```bash
SONATYPE_USERNAME          # Your Sonatype email
SONATYPE_TOKEN            # Sonatype API token
GPG_PRIVATE_KEY           # Base64-encoded GPG private key
GPG_PASSPHRASE            # GPG key passphrase
GPG_KEYNAME               # GPG key ID
```

### Environment Setup
- **Staging**: Testing deployment (default)
- **Production**: Direct deployment to Maven Central

## 📦 Deployed Artifacts

After successful deployment, the following modules will be available:

| Module | Maven Coordinates |
|--------|------------------|
| **Core Runtime** | `io.github.yasmramos:veld-runtime:1.0.0` |
| **Annotations** | `io.github.yasmramos:veld-annotations:1.0.0` |
| **Processor** | `io.github.yasmramos:veld-processor:1.0.0` |
| **Weaver** | `io.github.yasmramos:veld-weaver:1.0.0` |
| **AOP Support** | `io.github.yasmramos:veld-aop:1.0.0` |
| **Spring Boot** | `io.github.yasmramos:veld-spring-boot-starter:1.0.0` |
| **Maven Plugin** | `io.github.yasmramos:veld-maven-plugin:1.0.0` |
| **Benchmarks** | `io.github.yasmramos:veld-benchmark:1.0.0` |
| **Examples** | `io.github.yasmramos:veld-example:1.0.0` |

## 🔍 Post-Deployment Checklist

### 1. Verify Sonatype Deployment
- [ ] Login to https://s01.oss.sonatype.org/
- [ ] Check "Staging Repositories" for Veld repository
- [ ] Verify all 9 modules are present
- [ ] Close the staging repository

### 2. Release to Maven Central
- [ ] Click "Release" in Sonatype Nexus
- [ ] Confirm the release action
- [ ] Wait for Maven Central synchronization (2-24 hours)

### 3. Verify Maven Central Availability
- [ ] Check https://search.maven.org/ for `io.github.yasmramos:veld-runtime:1.0.0`
- [ ] Verify all modules are searchable
- [ ] Test dependency resolution in a sample project

### 4. Documentation Updates
- [ ] Update README.md if needed
- [ ] Update CHANGELOG.md with release date
- [ ] Announce release on appropriate channels

## 🛠️ Troubleshooting

### Common Issues

#### "Build Failed" in GitHub Actions
**Symptoms**: CI workflow fails  
**Solutions**:
1. Check test results in Actions artifacts
2. Fix failing tests
3. Ensure all dependencies are resolved
4. Verify Java version compatibility

#### "GPG Signing Failed"
**Symptoms**: Deployment workflow fails at signing step  
**Solutions**:
1. Verify GPG private key is correctly configured
2. Check GPG passphrase is correct
3. Ensure GPG keyname matches the key ID
4. Test GPG key locally: `gpg --list-keys`

#### "Sonatype Authentication Failed"
**Symptoms**: 401 Unauthorized errors  
**Solutions**:
1. Verify Sonatype username and token
2. Check token hasn't expired
3. Ensure token has deployment permissions
4. Regenerate token if necessary

#### "Tests Failed"
**Symptoms**: Test failures prevent deployment  
**Solutions**:
1. Review test reports in GitHub Actions
2. Fix failing unit or integration tests
3. Update test expectations if needed
4. Ensure test environment is properly configured

#### "Javadoc Generation Failed"
**Symptoms**: Javadoc errors prevent deployment  
**Solutions**:
1. Check for missing Javadoc on public classes
2. Fix malformed Javadoc comments
3. Ensure all public APIs are documented
4. Run `mvn javadoc:javadoc` locally to debug

### Debug Commands

#### Test Local Build
```bash
# Full build and test
mvn clean install -DskipTests=false

# Quick compilation check
mvn clean compile

# Run specific tests
mvn test -Dtest=*Component*Test
```

#### Check Artifacts
```bash
# List generated JARs
find . -name "*.jar" -path "*/target/*"

# Verify GPG signing
gpg --verify */target/*.jar.asc

# Check JAR contents
jar -tf */target/*.jar | head -20
```

#### Validate Maven Coordinates
```bash
# Check if artifact exists on Maven Central
curl "https://search.maven.org/solrsearch/select?q=g:io.github.yasmramos+AND+a:veld-runtime&rows=10&wt=json"
```

## 📊 Success Metrics

### Deployment Success Indicators
- ✅ All GitHub Actions workflows complete successfully
- ✅ Sonatype Nexus shows all 9 modules in staging repository
- ✅ Staging repository closes without errors
- ✅ Release to Maven Central completes successfully
- ✅ Artifacts appear on https://search.maven.org/ within 24 hours
- ✅ Dependency resolution works in test projects

### Performance Benchmarks
- **Deployment Time**: ~10-15 minutes for full release
- **Maven Central Sync**: 2-24 hours (usually 2-4 hours)
- **CI Pipeline**: ~5-8 minutes for complete validation

## 🔒 Security Best Practices

1. **Secret Management**:
   - Use GitHub Secrets for all sensitive data
   - Rotate Sonatype tokens regularly
   - Keep GPG keys secure and backed up

2. **Access Control**:
   - Limit deployment permissions to maintainers only
   - Use branch protection for main branch
   - Require PR reviews for changes

3. **Monitoring**:
   - Monitor deployment history for anomalies
   - Set up alerts for failed deployments
   - Regularly review access logs

## 📞 Support

### Resources
- **Sonatype Documentation**: https://help.sonatype.com/docs
- **GitHub Actions Docs**: https://docs.github.com/en/actions
- **Maven Deployment Guide**: https://maven.apache.org/repository/guide-distribution.html

### Getting Help
1. Check this deployment guide
2. Review GitHub Actions logs
3. Consult Sonatype Nexus logs
4. Contact the Veld team

---

**Ready to Deploy?** Follow the [Configuration Guide](./DEPLOYMENT_GUIDE.md) to set up your secrets, then use the quick start commands above!