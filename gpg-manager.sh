#!/bin/bash

# GPG Key Management Script for Veld Release
# Handles GPG key generation, import, and export

set -e

ACTION=${1:-"status"}
KEY_NAME=${2:-"Veld Release Key"}
KEY_EMAIL=${3:-"releases@veld-framework.com"}
KEY_COMMENT=${4:-"Maven Central Release Key"}

echo "🔐 Veld GPG Key Management"
echo "========================="
echo "Action: $ACTION"
echo ""

case "$ACTION" in
    "generate")
        echo "Generating new GPG key for releases..."
        
        # Check if gpg is installed
        if ! command -v gpg &> /dev/null; then
            echo "❌ Error: GPG is not installed"
            echo "   Install with: sudo apt-get install gnupg2"
            exit 1
        fi
        
        # Generate key configuration
        cat > /tmp/gpg-key-config << EOF
%no-protection
Key-Type: RSA
Key-Length: 4096
Subkey-Type: RSA
Subkey-Length: 4096
Name-Real: $KEY_NAME
Name-Email: $KEY_EMAIL
Comment: $KEY_COMMENT
Expire-Date: 2y
EOF
        
        echo "📝 Generating GPG key (this may take a few minutes)..."
        gpg --batch --generate-key /tmp/gpg-key-config
        
        # Get the key ID
        KEY_ID=$(gpg --list-secret-keys --keyid-format LONG | grep sec | awk '{print $2}' | cut -d'/' -f2)
        
        if [ -n "$KEY_ID" ]; then
            echo "✅ GPG key generated successfully!"
            echo "Key ID: $KEY_ID"
            echo ""
            echo "📤 Export public key (add to GitHub):"
            echo "gpg --armor --export $KEY_ID"
            echo ""
            echo "🔑 Export private key (store securely):"
            echo "gpg --armor --export-secret-keys $KEY_ID"
            echo ""
            echo "💡 Next steps:"
            echo "1. Add your public key to GitHub: https://github.com/settings/keys"
            echo "2. Store your private key securely (don't commit to git)"
            echo "3. Configure environment variables:"
            echo "   export GPG_KEYNAME=$KEY_ID"
            echo "   export GPG_PASSPHRASE=your_passphrase"
        else
            echo "❌ Error: Failed to generate GPG key"
            exit 1
        fi
        
        # Cleanup
        rm -f /tmp/gpg-key-config
        ;;
        
    "export")
        KEY_ID=$2
        if [ -z "$KEY_ID" ]; then
            echo "❌ Error: Usage: $0 export <key_id>"
            exit 1
        fi
        
        echo "📤 Exporting GPG key: $KEY_ID"
        echo ""
        echo "Public Key (add to GitHub):"
        echo "==========================="
        gpg --armor --export $KEY_ID
        echo ""
        echo "Private Key (store securely - NEVER commit):"
        echo "============================================"
        gpg --armor --export-secret-keys $KEY_ID
        ;;
        
    "import")
        KEY_FILE=$2
        if [ -z "$KEY_FILE" ]; then
            echo "❌ Error: Usage: $0 import <key_file>"
            exit 1
        fi
        
        if [ ! -f "$KEY_FILE" ]; then
            echo "❌ Error: Key file not found: $KEY_FILE"
            exit 1
        fi
        
        echo "📥 Importing GPG key from: $KEY_FILE"
        gpg --import "$KEY_FILE"
        echo "✅ Key imported successfully"
        ;;
        
    "list")
        echo "📋 Available GPG keys:"
        gpg --list-secret-keys --keyid-format LONG
        ;;
        
    "status")
        echo "🔍 GPG Status Check"
        echo "==================="
        
        # Check if gpg is installed
        if command -v gpg &> /dev/null; then
            echo "✅ GPG installed: $(gpg --version | head -1)"
        else
            echo "❌ GPG not installed"
        fi
        
        # Check for keys
        KEY_COUNT=$(gpg --list-secret-keys --keyid-format LONG 2>/dev/null | grep -c "sec" || echo "0")
        echo "Secret keys: $KEY_COUNT"
        
        if [ $KEY_COUNT -gt 0 ]; then
            echo ""
            echo "📋 Available keys:"
            gpg --list-secret-keys --keyid-format LONG | grep -E "sec|uid"
        fi
        
        # Check environment variables
        echo ""
        echo "🔧 Environment Variables:"
        if [ -n "$GPG_KEYNAME" ]; then
            echo "✅ GPG_KEYNAME set: $GPG_KEYNAME"
        else
            echo "⚠️  GPG_KEYNAME not set"
        fi
        
        if [ -n "$GPG_PASSPHRASE" ]; then
            echo "✅ GPG_PASSPHRASE set"
        else
            echo "⚠️  GPG_PASSPHRASE not set"
        fi
        ;;
        
    "help"|*)
        echo "📖 GPG Key Management Help"
        echo "=========================="
        echo ""
        echo "Usage: $0 <action> [arguments]"
        echo ""
        echo "Actions:"
        echo "  status                    - Check GPG installation and keys"
        echo "  generate [name] [email]   - Generate new GPG key"
        echo "  export <key_id>           - Export public and private key"
        echo "  import <key_file>         - Import key from file"
        echo "  list                      - List available keys"
        echo "  help                      - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 status"
        echo "  $0 generate"
        echo "  $0 export 1234567890ABCDEF"
        echo "  $0 import my-gpg-key.asc"
        echo ""
        echo "Environment Variables:"
        echo "  GPG_KEYNAME      - GPG key ID for signing"
        echo "  GPG_PASSPHRASE   - GPG key passphrase"
        echo ""
        echo "GitHub Integration:"
        echo "1. Add your public key to GitHub: https://github.com/settings/keys"
        echo "2. Add secrets to your repository:"
        echo "   - GPG_KEYNAME"
        echo "   - GPG_PASSPHRASE"
        echo "   - GPG_PRIVATE_KEY"
        ;;
esac

echo ""