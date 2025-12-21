#!/bin/bash

# 🌿 Veld Framework - Validador de Nombres de Ramas
# Este script valida que los nombres de ramas sigan las convenciones establecidas

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Tipos de ramas permitidos
ALLOWED_TYPES=(
    "feature"
    "bugfix" 
    "hotfix"
    "refactor"
    "docs"
    "test"
    "chore"
    "style"
    "perf"
    "release"
    "temp"
    "team"
)

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
    echo "🌿 Validador de Nombres de Ramas - Veld Framework"
    echo ""
    echo "USAGE:"
    echo "  $0 [OPCIÓN] [NOMBRE_RAMA]"
    echo ""
    echo "OPCIONES:"
    echo "  -h, --help          Mostrar esta ayuda"
    echo "  -v, --validate      Validar nombre de rama (por defecto)"
    echo "  -c, --current       Validar rama actual"
    echo "  -l, --list          Listar tipos de ramas permitidos"
    echo ""
    echo "EJEMPLOS:"
    echo "  $0 feature/new-container-api"
    echo "  $0 --current"
    echo "  $0 --list"
    echo ""
    echo "TIPOS DE RAMAS PERMITIDOS:"
    printf '  • %s\n' "${ALLOWED_TYPES[@]}"
}

# Función para validar formato de nombre
validate_format() {
    local branch_name="$1"
    
    # Verificar que no esté vacío
    if [ -z "$branch_name" ]; then
        error "El nombre de rama no puede estar vacío"
        return 1
    fi
    
    # Verificar que no contenga espacios
    if echo "$branch_name" | grep -q " "; then
        error "El nombre de rama no puede contener espacios"
        return 1
    fi
    
    # Verificar que no contenga caracteres especiales prohibidos
    if echo "$branch_name" | grep -q '[!@#$%^&*()+=<>?:"`~,]'; then
        error "El nombre de rama contiene caracteres especiales prohibidos"
        return 1
    fi
    
    # Verificar que no esté en mayúsculas
    if echo "$branch_name" | grep -q '[A-Z]'; then
        error "El nombre de rama debe estar en minúsculas"
        return 1
    fi
    
    # Verificar longitud máxima
    if [ ${#branch_name} -gt 60 ]; then
        error "El nombre de rama es demasiado largo (máximo 60 caracteres)"
        return 1
    fi
    
    # Verificar longitud mínima
    if [ ${#branch_name} -lt 3 ]; then
        error "El nombre de rama es demasiado corto (mínimo 3 caracteres)"
        return 1
    fi
    
    return 0
}

# Función para validar prefijo
validate_prefix() {
    local branch_name="$1"
    
    # Extraer el prefijo (parte antes del primer '/')
    local prefix=$(echo "$branch_name" | cut -d'/' -f1)
    
    # Verificar que existe un prefijo
    if [[ "$branch_name" != *"/"* ]]; then
        error "El nombre de rama debe tener un prefijo (ej: feature/, bugfix/, etc.)"
        return 1
    fi
    
    # Verificar que el prefijo esté permitido
    local valid_prefix=false
    for allowed_type in "${ALLOWED_TYPES[@]}"; do
        if [ "$prefix" = "$allowed_type" ]; then
            valid_prefix=true
            break
        fi
    done
    
    if [ "$valid_prefix" = false ]; then
        error "Prefijo '$prefix' no está permitido"
        echo "Prefijos permitidos: ${ALLOWED_TYPES[*]}"
        return 1
    fi
    
    # Validaciones específicas por tipo
    case "$prefix" in
        "hotfix")
            if [ ${#branch_name} -gt 40 ]; then
                warning "Ramas hotfix deberían ser cortas (máximo 40 caracteres)"
            fi
            ;;
        "release")
            if ! echo "$branch_name" | grep -qE '^release/v[0-9]+\.[0-9]+\.[0-9]+'; then
                error "Ramas de release deben seguir el patrón: release/vX.Y.Z"
                return 1
            fi
            ;;
        "team")
            # Permitir team/nombre-equipo/tipo-tarea
            if ! echo "$branch_name" | grep -qE '^team/[^/]+/[^/]+'; then
                error "Ramas de equipo deben seguir el patrón: team/nombre-equipo/tipo-tarea"
                return 1
            fi
            ;;
    esac
    
    return 0
}

# Función para validar descripción
validate_description() {
    local branch_name="$1"
    
    # Extraer la descripción (parte después del primer '/')
    local description=$(echo "$branch_name" | cut -d'/' -f2-)
    
    # Verificar que existe descripción
    if [ -z "$description" ]; then
        error "Falta la descripción después del prefijo"
        return 1
    fi
    
    # Verificar que no sea genérica
    local generic_descriptions=("test" "update" "fix" "new" "change")
    for generic in "${generic_descriptions[@]}"; do
        if [ "$description" = "$generic" ]; then
            error "La descripción '$description' es demasiado genérica, sé más específico"
            return 1
        fi
    done
    
    # Verificar formato de palabras (deben estar separadas por guiones)
    if echo "$description" | grep -q '_'; then
        error "Usa guiones (-) en lugar de guiones bajos (_) para separar palabras"
        return 1
    fi
    
    return 0
}

# Función principal de validación
validate_branch_name() {
    local branch_name="$1"
    
    if [ -z "$branch_name" ]; then
        error "No se proporcionó nombre de rama"
        return 1
    fi
    
    log "Validando nombre de rama: '$branch_name'"
    
    # Validaciones paso a paso
    if ! validate_format "$branch_name"; then
        return 1
    fi
    
    if ! validate_prefix "$branch_name"; then
        return 1
    fi
    
    if ! validate_description "$branch_name"; then
        return 1
    fi
    
    success "Nombre de rama válido: '$branch_name'"
    return 0
}

# Función para obtener rama actual
get_current_branch() {
    local current_branch
    
    # Verificar si estamos en un repositorio git
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        error "No estás en un repositorio Git"
        return 1
    fi
    
    # Obtener rama actual
    current_branch=$(git branch --show-current 2>/dev/null)
    
    if [ -z "$current_branch" ]; then
        error "No se pudo determinar la rama actual"
        return 1
    fi
    
    echo "$current_branch"
    return 0
}

# Función para listar tipos permitidos
list_allowed_types() {
    echo "Tipos de ramas permitidos:"
    echo ""
    
    local type_descriptions=(
        "feature:Nueva funcionalidad"
        "bugfix:Corrección de bug no crítico"
        "hotfix:Corrección urgente en producción"
        "refactor:Refactorización de código"
        "docs:Actualización de documentación"
        "test:Mejoras en testing"
        "chore:Tareas de mantenimiento"
        "style:Cambios de formato y estilo"
        "perf:Optimizaciones de rendimiento"
        "release:Preparación de releases"
        "temp:Ramas temporales"
        "team:Ramas específicas de equipo"
    )
    
    for type_desc in "${type_descriptions[@]}"; do
        local type=$(echo "$type_desc" | cut -d':' -f1)
        local desc=$(echo "$type_desc" | cut -d':' -f2)
        printf "  ${GREEN}%-10s${NC} %s\n" "$type/" "$desc"
    done
}

# Función para sugerencias de mejora
suggest_improvements() {
    local branch_name="$1"
    
    echo ""
    echo "💡 Sugerencias de mejora:"
    echo ""
    
    # Análisis del prefijo
    local prefix=$(echo "$branch_name" | cut -d'/' -f1)
    case "$prefix" in
        "feature")
            echo "• Asegúrate de que describe una funcionalidad específica"
            echo "• Usa sustantivos descriptivos (ej: 'container-api' no 'api')"
            ;;
        "bugfix")
            echo "• Incluye el tipo de error (NPE, memory leak, etc.)"
            echo "• Menciona el componente afectado"
            ;;
        "hotfix")
            echo "• Solo para situaciones críticas"
            echo "• Mantén el nombre corto y descriptivo"
            ;;
        "refactor")
            echo "• Especifica qué área se está mejorando"
            echo "• Indica el beneficio esperado"
            ;;
        "docs")
            echo "• Especifica qué documentación se actualiza"
            echo "• Menciona el tipo de documentación"
            ;;
    esac
}

# Función principal
main() {
    local action="validate"
    local branch_name=""
    
    # Procesar argumentos
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--validate)
                action="validate"
                shift
                ;;
            -c|--current)
                action="current"
                shift
                ;;
            -l|--list)
                action="list"
                shift
                ;;
            -*)
                error "Opción desconocida: $1"
                show_help
                exit 1
                ;;
            *)
                branch_name="$1"
                shift
                ;;
        esac
    done
    
    # Ejecutar acción
    case "$action" in
        "validate")
            if [ -z "$branch_name" ]; then
                error "Proporciona un nombre de rama para validar"
                show_help
                exit 1
            fi
            if validate_branch_name "$branch_name"; then
                exit 0
            else
                suggest_improvements "$branch_name"
                exit 1
            fi
            ;;
        "current")
            branch_name=$(get_current_branch)
            if [ $? -eq 0 ]; then
                log "Validando rama actual: '$branch_name'"
                if validate_branch_name "$branch_name"; then
                    exit 0
                else
                    suggest_improvements "$branch_name"
                    exit 1
                fi
            else
                exit 1
            fi
            ;;
        "list")
            list_allowed_types
            exit 0
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