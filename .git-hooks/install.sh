#!/bin/bash
#
# Script para instalar el pre-commit hook
# Uso: ./install.sh
#

set -e

HOOK_SOURCE=".git-hooks/pre-commit"
HOOK_DEST=".git/hooks/pre-commit"

echo "🔧 Instalando pre-commit hook..."
echo ""

# Verificar que el hook existe
if [ ! -f "$HOOK_SOURCE" ]; then
    echo "❌ Error: No se encontró el archivo $HOOK_SOURCE"
    exit 1
fi

# Verificar que .git existe
if [ ! -d ".git" ]; then
    echo "❌ Error: Este directorio no es un repositorio Git"
    exit 1
fi

# Copiar el hook
cp "$HOOK_SOURCE" "$HOOK_DEST"
chmod +x "$HOOK_DEST"

echo "✅ Pre-commit hook instalado exitosamente"
echo ""
echo "📋 Formato requerido para mensajes de commit:"
echo "   <type>(<scope>): <description>"
echo ""
echo "📝 Tipos válidos:"
echo "   feat, fix, docs, style, refactor, perf, test, chore, build, ci, revert"
echo ""
echo "💡 Para saltarte el hook: git commit --no-verify"
echo ""
echo "🔗 Más info: https://www.conventionalcommits.org/"
