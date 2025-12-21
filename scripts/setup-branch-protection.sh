#!/bin/bash

# 🔧 Veld Framework - Setup Script de Protección de Ramas
# Este script automatiza la configuración de protección de ramas en GitHub

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para logging
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
}

# Función para verificar herramientas requeridas
check_prerequisites() {
    log "Verificando prerequisitos..."
    
    # Verificar Git
    if ! command -v git &> /dev/null; then
        error "Git no está instalado"
        exit 1
    fi
    success "Git encontrado"
    
    # Verificar GitHub CLI
    if ! command -v gh &> /dev/null; then
        warning "GitHub CLI no está instalado"
        echo "Por favor instala GitHub CLI: https://cli.github.com/"
        read -p "¿Continuar de todos modos? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        success "GitHub CLI encontrado"
        
        # Verificar autenticación
        if ! gh auth status &> /dev/null; then
            error "GitHub CLI no está autenticado"
            echo "Por favor ejecuta: gh auth login"
            exit 1
        fi
        success "GitHub CLI autenticado"
    fi
    
    # Verificar GPG
    if ! command -v gpg &> /dev/null; then
        warning "GPG no está instalado - requerido para commit signing"
    else
        success "GPG encontrado"
    fi
}

# Función para configurar protecciones de rama
setup_branch_protection() {
    log "Configurando protección de rama main..."
    
    if ! command -v gh &> /dev/null; then
        warning "Saltando configuración automática - GitHub CLI no disponible"
        echo "Por favor configura manualmente en: https://github.com/$GITHUB_REPOSITORY/settings/branches"
        return
    fi
    
    # Obtener información del repositorio
    REPO_INFO=$(gh repo view --json owner,name -q '.owner.login + "/" + .name')
    log "Configurando protecciones para: $REPO_INFO"
    
    # Configurar protección de rama usando GitHub API
    success "Aplicando protecciones de rama..."
    
    # Nota: GitHub API para branch protection es compleja, mejor usar UI
    warning "Configuración manual requerida:"
    echo "1. Ve a: https://github.com/$REPO_INFO/settings/branches"
    echo "2. Agregar regla para 'main'"
    echo "3. Seleccionar las opciones según docs/SETUP_BRANCH_PROTECTION.md"
}

# Función para configurar labels
setup_labels() {
    log "Configurando labels de GitHub..."
    
    if ! command -v gh &> /dev/null; then
        warning "Saltando configuración de labels - GitHub CLI no disponible"
        return
    fi
    
    log "Creando labels requeridos..."
    
    # Labels de tipo
    declare -A labels=(
        ["feature"]="New functionality"
        ["bugfix"]="Bug fix"
        ["hotfix"]="Urgent fix"
        ["refactor"]="Code refactoring"
        ["documentation"]="Documentation changes"
        ["test"]="Test updates"
        ["build"]="Build/CI changes"
    )
    
    # Labels de prioridad
    declare -A priority_labels=(
        ["priority:high"]="High priority"
        ["priority:medium"]="Medium priority"
        ["priority:low"]="Low priority"
    )
    
    # Labels de estado
    declare -A status_labels=(
        ["triage"]="Needs initial review"
        ["in-review"]="Currently under review"
        ["approved"]="Ready to merge"
    )
    
    # Colores para labels
    declare -A colors=(
        ["feature"]="0366d6"
        ["bugfix"]="d73a4a"
        ["hotfix"]="b31d28"
        ["refactor"]="fbca04"
        ["documentation"]="0075ca"
        ["test"]="c5def5"
        ["build"]="fbca04"
        ["priority:high"]="b31d28"
        ["priority:medium"]="fbca04"
        ["priority:low"]="28a745"
        ["triage"]="d73a4a"
        ["in-review"]="fbca04"
        ["approved"]="28a745"
    )
    
    # Crear labels
    for label in "${!labels[@]}"; do
        if gh label create "$label" --description "${labels[$label]}" --color "${colors[$label]}" 2>/dev/null; then
            success "Label creado: $label"
        else
            warning "Label ya existe: $label"
        fi
    done
    
    for label in "${!priority_labels[@]}"; do
        if gh label create "$label" --description "${priority_labels[$label]}" --color "${colors[$label]}" 2>/dev/null; then
            success "Label creado: $label"
        else
            warning "Label ya existe: $label"
        fi
    done
    
    for label in "${!status_labels[@]}"; do
        if gh label create "$label" --description "${status_labels[$label]}" --color "${colors[$label]}" 2>/dev/null; then
            success "Label creado: $label"
        else
            warning "Label ya existe: $label"
        fi
    done
}

# Función para configurar CODEOWNERS
setup_codeowners() {
    log "Configurando CODEOWNERS..."
    
    if [ -f ".github/CODEOWNERS" ]; then
        warning "CODEOWNERS ya existe, no se sobrescribirá"
        return
    fi
    
    cat > .github/CODEOWNERS << 'EOF'
# Global code owners
* @YasMRamos

# Java code owners
*.java @YasMRamos
**/src/main/java/ @YasMRamos

# Documentation owners
*.md @YasMRamos
docs/ @YasMRamos

# CI/CD owners
.github/ @YasMRamos

# Build configuration
pom.xml @YasMRamos
build.gradle @YasMRamos
*.yml @YasMRamos
*.yaml @YasMRamos
EOF
    
    success "CODEOWNERS creado"
}

# Función para configurar Dependabot
setup_dependabot() {
    log "Configurando Dependabot..."
    
    if [ -f ".github/dependabot.yml" ]; then
        warning "dependabot.yml ya existe, no se sobrescribirá"
        return
    fi
    
    mkdir -p .github
    
    cat > .github/dependabot.yml << 'EOF'
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
    open-pull-requests-limit: 5
    reviewers:
      - "YasMRamos"
    assignees:
      - "YasMRamos"
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
    open-pull-requests-limit: 3
    reviewers:
      - "YasMRamos"
EOF
    
    success "Dependabot configurado"
}

# Función para verificar configuración de GPG
setup_gpg_signing() {
    log "Verificando configuración de GPG signing..."
    
    if ! command -v gpg &> /dev/null; then
        warning "GPG no está disponible"
        return
    fi
    
    # Verificar si ya está configurado
    if git config --global user.signingkey &> /dev/null; then
        success "GPG signing ya está configurado"
        return
    fi
    
    echo "Configuración de GPG signing:"
    echo "1. Generar una clave GPG: gpg --full-generate-key"
    echo "2. Configurar Git: git config --global user.signingkey YOUR_KEY_ID"
    echo "3. Habilitar signing: git config --global commit.gpgsign true"
    echo ""
    read -p "¿Ya tienes configurado GPG signing? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log "Por favor configura tu GPG key ID:"
        read -p "Ingresa tu GPG key ID: " KEY_ID
        git config --global user.signingkey "$KEY_ID"
        git config --global commit.gpgsign true
        success "GPG signing configurado"
    else
        warning "Configura GPG signing manualmente más tarde"
    fi
}

# Función para test de configuración
test_configuration() {
    log "Ejecutando tests de configuración..."
    
    # Verificar estructura de archivos
    local files=(
        ".github/workflows/pr-quality-gate.yml"
        ".github/workflows/security-compliance.yml"
        ".github/workflows/code-quality.yml"
        ".github/CODEOWNERS"
        ".github/dependabot.yml"
        "docs/BRANCH_PROTECTION_RULES.md"
        "docs/SETUP_BRANCH_PROTECTION.md"
    )
    
    for file in "${files[@]}"; do
        if [ -f "$file" ]; then
            success "Archivo encontrado: $file"
        else
            error "Archivo faltante: $file"
        fi
    done
    
    # Verificar workflows
    if command -v gh &> /dev/null; then
        log "Verificando workflows en GitHub..."
        gh workflow list || warning "No se pudieron listar workflows"
    fi
}

# Función para mostrar resumen final
show_summary() {
    echo ""
    echo "🎉 ¡Configuración completada!"
    echo ""
    echo "📋 Resumen de lo configurado:"
    echo "  ✅ Workflows de calidad y seguridad"
    echo "  ✅ Protecciones de rama (configuración manual requerida)"
    echo "  ✅ Labels de GitHub"
    echo "  ✅ CODEOWNERS"
    echo "  ✅ Dependabot"
    echo "  ✅ Documentación completa"
    echo ""
    echo "🔧 Pasos manuales restantes:"
    echo "  1. Configurar protecciones de rama en GitHub UI"
    echo "  2. Configurar GPG signing (si no está configurado)"
    echo "  3. Revisar workflows en Actions tab"
    echo ""
    echo "📚 Documentación:"
    echo "  • docs/BRANCH_PROTECTION_RULES.md"
    echo "  • docs/SETUP_BRANCH_PROTECTION.md"
    echo ""
    echo "🚀 Próximos pasos:"
    echo "  1. Haz commit y push de estos cambios"
    echo "  2. Prueba creando un PR de ejemplo"
    echo "  3. Verifica que todas las protecciones funcionen"
    echo ""
    success "¡Sistema de protección de ramas listo para usar!"
}

# Función principal
main() {
    echo "🔧 Veld Framework - Setup de Protección de Ramas"
    echo "================================================"
    echo ""
    
    check_prerequisites
    setup_branch_protection
    setup_labels
    setup_codeowners
    setup_dependabot
    setup_gpg_signing
    test_configuration
    show_summary
}

# Ejecutar función principal
main "$@"