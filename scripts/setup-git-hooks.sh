#!/bin/bash

# 🔗 Veld Framework - Instalador de Git Hooks
# Este script instala automáticamente los Git hooks para validación de nombres de ramas

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

# Función para mostrar ayuda
show_help() {
    echo "🔗 Instalador de Git Hooks - Veld Framework"
    echo ""
    echo "Este script instala automáticamente los Git hooks para validar nombres de ramas."
    echo ""
    echo "USAGE:"
    echo "  $0 [OPCIONES]"
    echo ""
    echo "OPCIONES:"
    echo "  -h, --help          Mostrar esta ayuda"
    echo "  -f, --force         Forzar instalación sobrescribiendo hooks existentes"
    echo "  -u, --uninstall     Desinstalar los hooks"
    echo "  -s, --status        Mostrar estado actual de los hooks"
    echo "  -t, --test          Ejecutar tests de validación"
    echo ""
    echo "HOOKS INSTALADOS:"
    echo "  • pre-commit:  Valida nombres de ramas antes de commit"
    echo "  • pre-push:    Valida nombres de ramas antes de push"
    echo ""
    echo "FUNCIONALIDADES:"
    echo "  • Validación automática de nombres de ramas"
    echo "  • Prevención de commits en ramas con nombres inválidos"
    echo "  • Bloqueo de push con nombres de ramas incorrectos"
    echo "  • Sugerencias automáticas para nombres válidos"
    echo "  • Integración con scripts de creación de ramas"
}

# Función para verificar si estamos en un repositorio git
check_git_repo() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        error "No estás en un repositorio Git"
        echo "Ejecuta este script desde la raíz del proyecto Veld"
        exit 1
    fi
    
    success "Repositorio Git detectado"
}

# Función para verificar si los hooks ya existen
hooks_exist() {
    local hook_name="$1"
    local hooks_dir=$(git rev-parse --git-dir)/hooks
    
    [ -f "$hooks_dir/$hook_name" ]
}

# Función para hacer backup de hook existente
backup_hook() {
    local hook_name="$1"
    local hooks_dir=$(git rev-parse --git-dir)/hooks
    local backup_file="$hooks_dir/${hook_name}.backup.$(date +%Y%m%d_%H%M%S)"
    
    if [ -f "$hooks_dir/$hook_name" ]; then
        cp "$hooks_dir/$hook_name" "$backup_file"
        success "Backup creado: $backup_file"
    fi
}

# Función para instalar un hook específico
install_hook() {
    local hook_name="$1"
    local source_file="$2"
    local hooks_dir=$(git rev-parse --git-dir)/hooks
    local target_file="$hooks_dir/$hook_name"
    
    log "Instalando hook: $hook_name"
    
    # Verificar que el archivo fuente existe
    if [ ! -f "$source_file" ]; then
        error "Archivo fuente no encontrado: $source_file"
        return 1
    fi
    
    # Crear directorio de hooks si no existe
    mkdir -p "$hooks_dir"
    
    # Hacer backup si el hook ya existe
    if [ -f "$target_file" ]; then
        backup_hook "$hook_name"
    fi
    
    # Copiar el hook
    cp "$source_file" "$target_file"
    
    # Hacer ejecutable
    chmod +x "$target_file"
    
    success "Hook '$hook_name' instalado exitosamente"
}

# Función para desinstalar hooks
uninstall_hooks() {
    local hooks_dir=$(git rev-parse --git-dir)/hooks
    
    log "Desinstalando Git hooks..."
    
    local hooks=("pre-commit" "pre-push")
    
    for hook in "${hooks[@]}"; do
        if [ -f "$hooks_dir/$hook" ]; then
            backup_hook "$hook"
            rm "$hooks_dir/$hook"
            success "Hook '$hook' desinstalado"
        else
            warning "Hook '$hook' no encontrado"
        fi
    done
    
    success "Desinstalación completada"
}

# Función para mostrar estado de hooks
show_hook_status() {
    local hooks_dir=$(git rev-parse --git-dir)/hooks
    
    echo "📊 Estado de Git Hooks"
    echo "======================"
    echo ""
    
    local hooks=("pre-commit" "pre-push")
    
    for hook in "${hooks[@]}"; do
        if [ -f "$hooks_dir/$hook" ]; then
            echo -e "${GREEN}✅ $hook${NC} - Instalado"
            
            # Mostrar información adicional
            if [ -f "$hooks_dir/${hook}.backup."* ]; then
                local backup_count=$(ls "$hooks_dir/${hook}.backup."* 2>/dev/null | wc -l)
                echo "   Backups disponibles: $backup_count"
            fi
            
            # Verificar si es ejecutable
            if [ -x "$hooks_dir/$hook" ]; then
                echo "   Permisos: Ejecutable"
            else
                echo -e "   ${YELLOW}Permisos: No ejecutable${NC}"
            fi
        else
            echo -e "${RED}❌ $hook${NC} - No instalado"
        fi
        echo ""
    done
    
    echo "📋 Archivos de hooks en el proyecto:"
    echo "  • .git/hooks/pre-commit"
    echo "  • .git/hooks/pre-push"
    echo ""
    
    echo "🔧 Scripts relacionados:"
    echo "  • scripts/validate-branch-name.sh"
    echo "  • scripts/create-branch.sh"
    echo ""
    
    echo "📚 Documentación:"
    echo "  • docs/BRANCH_NAMING_CONVENTIONS.md"
}

# Función para ejecutar tests de validación
run_validation_tests() {
    log "Ejecutando tests de validación de hooks..."
    
    echo ""
    echo "🧪 Test 1: Validar nombre de rama válido"
    if ./scripts/validate-branch-name.sh feature/test-feature-123; then
        success "Test 1: ✅ Pasó"
    else
        error "Test 1: ❌ Falló"
    fi
    
    echo ""
    echo "🧪 Test 2: Validar nombre de rama inválido (debería fallar)"
    if ./scripts/validate-branch-name.sh InvalidBranchName 2>/dev/null; then
        error "Test 2: ❌ No debería haber pasado"
    else
        success "Test 2: ✅ Pasó (correctamente detectó nombre inválido)"
    fi
    
    echo ""
    echo "🧪 Test 3: Crear rama válida"
    if ./scripts/create-branch.sh test test-validation-temp --yes; then
        success "Test 3: ✅ Rama de prueba creada"
        
        # Limpiar rama de prueba
        git branch -D test/test-validation-temp 2>/dev/null || true
        success "Test 3: ✅ Rama de prueba eliminada"
    else
        error "Test 3: ❌ No se pudo crear rama de prueba"
    fi
    
    echo ""
    echo "🧪 Test 4: Verificar permisos de hooks"
    local hooks_dir=$(git rev-parse --git-dir)/hooks
    if [ -x "$hooks_dir/pre-commit" ] && [ -x "$hooks_dir/pre-push" ]; then
        success "Test 4: ✅ Hooks tienen permisos ejecutables"
    else
        error "Test 4: ❌ Hooks no tienen permisos ejecutables"
    fi
    
    echo ""
    success "Tests de validación completados"
}

# Función para instalar todos los hooks
install_all_hooks() {
    local force=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -f|--force)
                force=true
                shift
                ;;
            *)
                shift
                ;;
        esac
    done
    
    log "🔗 Instalando Git Hooks para Veld Framework"
    echo "=============================================="
    
    # Verificar repositorio
    check_git_repo
    
    # Verificar scripts necesarios
    log "Verificando scripts de soporte..."
    
    local required_scripts=(
        "scripts/validate-branch-name.sh"
        "scripts/create-branch.sh"
        ".git/hooks/pre-commit"
        ".git/hooks/pre-push"
    )
    
    for script in "${required_scripts[@]}"; do
        if [ ! -f "$script" ]; then
            error "Script requerido no encontrado: $script"
            echo "Asegúrate de que todos los archivos estén en su lugar"
            exit 1
        fi
        success "Encontrado: $script"
    done
    
    echo ""
    
    # Instalar hooks
    local hooks=(
        "pre-commit:.git/hooks/pre-commit"
        "pre-push:.git/hooks/pre-push"
    )
    
    for hook_pair in "${hooks[@]}"; do
        local hook_name=$(echo "$hook_pair" | cut -d':' -f1)
        local hook_file=$(echo "$hook_pair" | cut -d':' -f2)
        
        if hooks_exist "$hook_name" && [ "$force" = false ]; then
            warning "Hook '$hook_name' ya existe"
            echo -n "¿Sobrescribir? (y/N): "
            read -r response
            
            case "$response" in
                [yY][eE][sS]|[yY])
                    install_hook "$hook_name" "$hook_file"
                    ;;
                *)
                    warning "Saltando hook '$hook_name'"
                    ;;
            esac
        else
            install_hook "$hook_name" "$hook_file"
        fi
    done
    
    echo ""
    success "¡Instalación de hooks completada!"
    
    echo ""
    echo "🎯 Próximos pasos:"
    echo "  1. Los hooks validarán automáticamente los nombres de ramas"
    echo "  2. Usa './scripts/create-branch.sh' para crear ramas válidas"
    echo "  3. Usa './scripts/validate-branch-name.sh' para validar manualmente"
    echo "  4. Lee la documentación: docs/BRANCH_NAMING_CONVENTIONS.md"
    
    echo ""
    echo "🧪 ¿Quieres ejecutar tests de validación? (recomendado)"
    echo -n "Ejecutar tests? (Y/n): "
    read -r response
    
    case "$response" in
        [nN][oO]|[nN])
            echo "Tests omitidos. Puedes ejecutarlos manualmente con: $0 --test"
            ;;
        *)
            run_validation_tests
            ;;
    esac
}

# Función principal
main() {
    local action="install"
    
    # Procesar argumentos
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -f|--force)
                action="install-force"
                shift
                ;;
            -u|--uninstall)
                action="uninstall"
                shift
                ;;
            -s|--status)
                action="status"
                shift
                ;;
            -t|--test)
                action="test"
                shift
                ;;
            -*)
                error "Opción desconocida: $1"
                show_help
                exit 1
                ;;
            *)
                shift
                ;;
        esac
    done
    
    # Ejecutar acción
    case "$action" in
        "install")
            check_git_repo
            install_all_hooks
            ;;
        "install-force")
            check_git_repo
            install_all_hooks --force
            ;;
        "uninstall")
            check_git_repo
            uninstall_hooks
            ;;
        "status")
            check_git_repo
            show_hook_status
            ;;
        "test")
            check_git_repo
            run_validation_tests
            ;;
        *)
            error "Acción no reconocida: $action"
            show_help
            exit 1
            ;;
    esac
}

# Ejecutar función principal
main "$@"