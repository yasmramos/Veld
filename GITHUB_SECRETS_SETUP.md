# GitHub Secrets Setup for Maven Central Deployment

## Required GitHub Secrets

To enable Maven Central deployment, you need to configure the following secrets in your GitHub repository:

### 1. Navigate to Repository Settings
1. Go to your GitHub repository: `yasmramos/Veld`
2. Click on **Settings** tab
3. In the left sidebar, click on **Secrets and variables**
4. Click on **Actions**

### 2. Add Repository Secrets

Add the following secrets with these exact names:

#### GPG Related Secrets:
- **Name**: `GPG_PRIVATE_KEY`
  - **Value**: The ASCII-armored private key (contents of `private-key.asc`)
  - **How to get**: `cat MindForge/.gpg-keys/private-key.asc`

- **Name**: `GPG_PASSPHRASE`
  - **Value**: The passphrase you used when generating the GPG key
  - **Note**: If you didn't set a passphrase, use an empty string

- **Name**: `GPG_KEYNAME`
  - **Value**: `your-gpg-key-id-here`

#### Sonatype OSSRH Secrets:
- **Name**: `SONATYPE_USERNAME`
  - **Value**: `your-ossrh-username`

- **Name**: `SONATYPE_TOKEN`
  - **Value**: `your-ossrh-token`

### 3. Update the Workflow (Optional)

The current workflow (`deploy.yml`) already has the correct configuration. If you want to modify it, ensure these environment variables are passed:

```yaml
env:
  SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
  SONATYPE_TOKEN: ${{ secrets.SONATYPE_TOKEN }}
  GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
  GPG_KEYNAME: ${{ secrets.GPG_KEYNAME }}
```

### 4. Test the Configuration

After adding the secrets:

1. Go to the **Actions** tab in your repository
2. Click on **Deploy to Maven Central** workflow
3. Click **Run workflow**
4. Select environment (staging/production)
5. Click **Run workflow** button

### 5. Troubleshooting

If you still get GPG errors:

#### Check GPG Key Format:
Make sure `GPG_PRIVATE_KEY` contains the full ASCII-armored key including:
```
-----BEGIN PGP PRIVATE KEY BLOCK-----
...
-----END PGP PRIVATE KEY BLOCK-----
```

#### Verify GPG Key ID:
```bash
# In your local environment, verify the key ID
gpg --list-keys
# Should show: your-gpg-key-id-here
```

#### Check Secret Values:
Make sure there are no extra spaces or line breaks in the secret values.

### 6. Alternative: Disable GPG Signing (Not Recommended)

If GPG continues to cause issues, you can temporarily disable it by modifying the `pom.xml`:

```xml
<properties>
    <skipGpg>true</skipGpg>
</properties>
```

**Note**: Maven Central requires GPG signing, so this is only for testing.

### 7. Secret Value Examples

Here's what each secret should look like:

**GPG_PRIVATE_KEY** (truncated for display):
```
-----BEGIN PGP PRIVATE KEY BLOCK-----
...
[Your full private key content]
...
-----END PGP PRIVATE KEY BLOCK-----
```

**GPG_PASSPHRASE**: `your-passphrase-here` (or empty if no passphrase)

**GPG_KEYNAME**: `your-gpg-key-id-here`

**SONATYPE_USERNAME**: `your-ossrh-username`

**SONATYPE_TOKEN**: `your-ossrh-token`

### 8. Security Notes

- Never commit these secrets to your repository
- The secrets are encrypted and only available to GitHub Actions
- Use different tokens for different environments if needed
- Consider rotating credentials periodically