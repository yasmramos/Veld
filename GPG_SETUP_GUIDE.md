# GPG Configuration for Maven Central Deployment

## Required GitHub Actions Secrets

To enable GPG signing for Maven Central deployment, you need to configure the following secrets in your GitHub repository:

### Required Secrets

1. **GPG_PRIVATE_KEY**
   - Your ASCII-armored GPG private key
   - Format: Include the full key from `-----BEGIN PGP PRIVATE KEY BLOCK-----` to `-----END PGP PRIVATE KEY BLOCK-----`

2. **GPG_PASSPHRASE**
   - The passphrase you used when generating the GPG key
   - If no passphrase was set, use an empty string

3. **GPG_KEYNAME** (Optional)
   - The key ID (e.g., `7ED0F874FB5F110ED4A79D1BBE6F8F8910A7EE99`)
   - If not provided, the first imported key will be used

4. **SONATYPE_USERNAME**
   - Your Sonatype OSSRH username
   - Used for Maven Central deployment

5. **SONATYPE_TOKEN**
   - Your Sonatype OSSRH token (not password)
   - Generate from: https://s01.oss.sonatype.org/profile/ -> Security -> Token

### How to Add Secrets

1. Go to your GitHub repository
2. Click on **Settings** tab
3. In the left sidebar, click on **Secrets and variables**
4. Click on **Actions**
5. Click **New repository secret**
6. Add each secret with the name and value above

### Example GPG Key Generation

```bash
# Generate a new GPG key
gpg --full-generate-key

# Export the private key (replace KEY_ID with your actual key ID)
gpg --export-secret-keys KEY_ID > private-key.asc

# Get the key ID
gpg --list-keys --keyid-format LONG
```

### Current Workflow Status

The deploy workflow has been updated to:
- ✅ Handle missing GPG configuration gracefully
- ✅ Provide clear instructions when secrets are missing
- ✅ Allow deployment without signing when GPG is not configured
- ✅ Continue to work with proper GPG configuration

### Security Notes

- Never commit these secrets to your repository
- The secrets are encrypted and only available to GitHub Actions
- Consider rotating credentials periodically
- Use different tokens for different environments if needed

### Troubleshooting

If you still get GPG errors:

1. **Check GPG key format**: Make sure `GPG_PRIVATE_KEY` contains the full ASCII-armored key
2. **Verify passphrase**: Ensure `GPG_PASSPHRASE` is correct
3. **Check key availability**: Verify the key ID with `gpg --list-keys`
4. **Review workflow logs**: Check the Actions tab for detailed error messages

### Maven Central Requirements

- GPG signing is required for production releases
- For staging deployments, signing can be skipped during development
- The workflow now supports both scenarios automatically