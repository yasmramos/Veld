#!/bin/bash

# 🌿 Veld Framework - Creador de Ramas
# Este script crea ramas siguiendo las convenciones establecidas

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Tipos de ramas permitidos
declare -A TYPE_DESCRIPTIONS=(
    ["feature"]="Nueva funcionalidad"
    ["bugfix"]="Corrección de bug no crítico"
    ["hotfix"]="Corrección urgente en producción"
    ["refactor"]="Refactorización de código"
    ["docs"]="Actualización de documentación"
    ["test"]="Mejoras en testing"
    ["chore"]="Tareas de mantenimiento"
    ["style"]="Cambios de formato y estilo"
    ["perf"]="Optimizaciones de rendimiento"
    ["release"]="Preparación de releases"
    ["temp"]="Ramas temporales"
    ["team"]="Ramas específicas de equipo"
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
    echo "🌿 Creador de Ramas - Veld Framework"
    echo ""
    echo "USAGE:"
    echo "  $0 <TIPO> <DESCRIPCION> [OPCIONES]"
    echo ""
    echo "TIPOS:"
    for type in "${!TYPE_DESCRIPTIONS[@]}"; do
        printf "  %-10s %s\n" "$type" "${TYPE_DESCRIPTIONS[$type]}"
    done
    echo ""
    echo "OPCIONES:"
    echo "  -h, --help          Mostrar esta ayuda"
    echo "  -c, --checkout      Cambiar a la nueva rama después de crearla"
    echo "  -s, --switch        Alias para --checkout"
    echo "  -p, --push          Hacer push de la nueva rama al repositorio remoto"
    echo "  -y, --yes           Responder 'yes' a todas las confirmaciones"
    echo "  -f, --force         Forzar creación incluso si la rama existe"
    echo ""
    echo "EJEMPLOS:"
    echo "  $0 feature dependency-injection-container"
    echo "  $0 bugfix npe-in-container-initialization --checkout --push"
    echo "  $0 docs api-reference-update --yes"
    echo "  $0 perf memory-optimization --switch"
    echo ""
    echo "CONVENCIONES:"
    echo "  • Los nombres deben estar en minúsculas"
    echo "  • Usar guiones (-) para separar palabras"
    echo "  • Máximo 60 caracteres total"
    echo "  • Hotfix máximo 40 caracteres"
    echo "  • Release debe seguir: release/vX.Y.Z"
    echo "  • Team debe seguir: team/nombre-equipo/tipo-tarea"
}

# Función para limpiar descripción
clean_description() {
    local description="$1"
    
    # Convertir a minúsculas
    description=$(echo "$description" | tr '[:upper:]' '[:lower:]')
    
    # Reemplazar espacios y caracteres especiales con guiones
    description=$(echo "$description" | sed 's/[^a-z0-9\-]/\-/g')
    
    # Remover múltiples guiones consecutivos
    description=$(echo "$description" | sed 's/\-\-+/\-/g')
    
    # Remover guiones al inicio y final
    description=$(echo "$description" | sed 's/^\-//' | sed 's/\-$//')
    
    echo "$description"
}

# Función para validar tipo de rama
validate_type() {
    local type="$1"
    
    if [[ ! -n "${TYPE_DESCRIPTIONS[$type]}" ]]; then
        error "Tipo de rama '$type' no está permitido"
        echo ""
        echo "Tipos permitidos:"
        for allowed_type in "${!TYPE_DESCRIPTIONS[@]}"; do
            printf "  %-10s %s\n" "$allowed_type" "${TYPE_DESCRIPTIONS[$allowed_type]}"
        done
        return 1
    fi
    
    return 0
}

# Función para validar descripción
validate_description() {
    local type="$1"
    local description="$2"
    
    # Verificar que no esté vacía
    if [ -z "$description" ]; then
        error "La descripción no puede estar vacía"
        return 1
    fi
    
    # Verificar longitud según el tipo
    local branch_name="${type}/${description}"
    local max_length=60
    
    case "$type" in
        "hotfix")
            max_length=40
            ;;
        "release")
            if ! echo "$description" | grep -qE '^v[0-9]+\.[0-9]+\.[0-9]+'; then
                error "Las ramas de release deben seguir el patrón: vX.Y.Z"
                return 1
            fi
            ;;
        "team")
            if ! echo "$description" | grep -qE '^[^/]+/[^/]+'; then
                error "Las ramas de equipo deben seguir el patrón: nombre-equipo/tipo-tarea"
                return 1
            fi
            ;;
    esac
    
    if [ ${#branch_name} -gt "$max_length" ]; then
        error "El nombre de rama es demasiado largo (máximo $max_length caracteres)"
        echo "Nombre actual: $branch_name (${#branch_name} caracteres)"
        return 1
    fi
    
    # Verificar que no sea genérica
    local generic_descriptions=("test" "update" "fix" "new" "change" "something" "thing")
    for generic in "${generic_descriptions[@]}"; do
        if [ "$description" = "$generic" ]; then
            error "La descripción '$description' es demasiado genérica, sé más específico"
            return 1
        fi
    done
    
    return 0
}

# Función para verificar si la rama ya existe
branch_exists() {
    local branch_name="$1"
    
    if git show-ref --verify --quiet "refs/heads/$branch_name"; then
        return 0
    elif git show-ref --verify --quiet "refs/remotes/origin/$branch_name"; then
        return 0
    else
        return 1
    fi
}

# Función para crear la rama
create_branch() {
    local type="$1"
    local description="$2"
    local branch_name="${type}/${description}"
    
    log "Creando rama: $branch_name"
    
    # Verificar si estamos en un repositorio git
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        error "No estás en un repositorio Git"
        return 1
    fi
    
    # Verificar si la rama ya existe
    if branch_exists "$branch_name"; then
        if [ "$FORCE" = true ]; then
            warning "La rama '$branch_name' ya existe, forzando creación..."
            git branch -D "$branch_name" 2>/dev/null || true
        else
            error "La rama '$branch_name' ya existe"
            echo ""
            echo "Opciones:"
            echo "  • Usar la rama existente: git checkout $branch_name"
            echo "  • Usar --force para sobrescribir"
            echo "  • Usar un nombre diferente"
            return 1
        fi
    fi
    
    # Crear la rama desde la rama actual
    local current_branch=$(git branch --show-current)
    log "Creando desde rama: $current_branch"
    
    if git checkout -b "$branch_name"; then
        success "Rama '$branch_name' creada exitosamente"
        
        # Mostrar información de la rama
        echo ""
        echo "📋 Información de la rama:"
        echo "  • Nombre: $branch_name"
        echo "  • Tipo: $type (${TYPE_DESCRIPTIONS[$type]})"
        echo "  • Descripción: $description"
        echo "  • Base: $current_branch"
        echo "  • Longitud: ${#branch_name} caracteres"
        
        return 0
    else
        error "Error al crear la rama '$branch_name'"
        return 1
    fi
}

# Función para hacer checkout
checkout_branch() {
    local branch_name="$1"
    
    log "Cambiando a rama: $branch_name"
    
    if git checkout "$branch_name"; then
        success "Cambiado a rama '$branch_name'"
        return 0
    else
        error "Error al cambiar a rama '$branch_name'"
        return 1
    fi
}

# Función para hacer push
push_branch() {
    local branch_name="$1"
    
    log "Haciendo push de rama: $branch_name"
    
    # Verificar si existe remote
    if ! git remote get-url origin > /dev/null 2>&1; then
        warning "No se encontró remote 'origin', saltando push"
        return 0
    fi
    
    if git push -u origin "$branch_name"; then
        success "Push de rama '$branch_name' completado"
        echo ""
        echo "🌐 URL del repositorio remoto disponible en:"
        echo "   $(git remote get-url origin)"
        return 0
    else
        error "Error al hacer push de rama '$branch_name'"
        return 1
    fi
}

# Función para mostrar próximos pasos
show_next_steps() {
    local type="$1"
    local branch_name="${type}/${2}"
    
    echo ""
    echo "🎯 Próximos pasos:"
    echo ""
    
    case "$type" in
        "feature")
            echo "1. Implementar la nueva funcionalidad"
            echo "2. Escribir tests unitarios"
            echo "3. Actualizar documentación si es necesario"
            echo "4. Crear PR con título: 'feat: $2'"
            ;;
        "bugfix")
            echo "1. Investigar y reproducir el bug"
            echo "2. Implementar la corrección"
            echo "3. Escribir tests para prevenir regresión"
            echo "4. Crear PR con título: 'fix: $2'"
            ;;
        "hotfix")
            echo "1. ¡Acción urgente! Implementar la corrección"
            echo "2. Probar en entorno de desarrollo"
            echo "3. Crear PR con título: 'hotfix: $2'"
            echo "4. Solicitar revisión urgente"
            ;;
        "refactor")
            echo "1. Identificar áreas de mejora específicas"
            echo "2. Implementar refactoring incremental"
            echo "3. Ejecutar todos los tests para verificar funcionalidad"
            echo "4. Crear PR con título: 'refactor: $2'"
            ;;
        "docs")
            echo "1. Actualizar documentación correspondiente"
            echo "2. Verificar enlaces y ejemplos"
            echo "3. Crear PR con título: 'docs: $2'"
            ;;
        "test")
            echo "1. Identificar áreas que necesitan más tests"
            echo "2. Implementar nuevos tests"
            echo "3. Mejorar cobertura de código"
            echo "4. Crear PR con título: 'test: $2'"
            ;;
        "chore")
            echo "1. Realizar tareas de mantenimiento"
            echo "2. Actualizar dependencias si es necesario"
            echo "3. Limpiar código/recursos"
            echo "4. Crear PR con título: 'chore: $2'"
            ;;
        "perf")
            echo "1. Identificar bottlenecks de performance"
            echo "2. Implementar optimizaciones"
            echo "3. Ejecutar benchmarks antes/después"
            echo "4. Crear PR con título: 'perf: $2'"
            ;;
        "release")
            echo "1. Actualizar versión en pom.xml"
            echo "2. Actualizar CHANGELOG.md"
            echo "3. Crear tag de release"
            echo "4. Hacer push del tag"
            ;;
    esac
    
    echo ""
    echo "💡 Consejos:"
    echo "  • Usa commits atómicos y descriptivos"
    echo "  • Mantén los PRs pequeños y enfocados"
    echo "  • Ejecuta tests antes de hacer push"
    echo "  • Sigue las convenciones de commit: https://www.conventionalcommits.org/"
}

# Función para confirmar acción
confirm_action() {
    local message="$1"
    
    if [ "$YES" = true ]; then
        return 0
    fi
    
    echo ""
    echo -n "$message (y/N): "
    read -r response
    
    case "$response" in
        [yY][eE][sS]|[yY])
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

# Función principal
main() {
    local type=""
    local description=""
    local checkout=false
    local push=false
    local yes=false
    local force=false
    
    # Procesar argumentos
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -c|--checkout|-s|--switch)
                checkout=true
                shift
                ;;
            -p|--push)
                push=true
                shift
                ;;
            -y|--yes)
                yes=true
                shift
                ;;
            -f|--force)
                force=true
                shift
                ;;
            -*)
                error "Opción desconocida: $1"
                show_help
                exit 1
                ;;
            *)
                if [ -z "$type" ]; then
                    type="$1"
                elif [ -z "$description" ]; then
                    description="$1"
                else
                    error "Demasiados argumentos"
                    show_help
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    # Validar argumentos requeridos
    if [ -z "$type" ] || [ -z "$description" ]; then
        error "Se requieren el tipo y la descripción de la rama"
        show_help
        exit 1
    fi
    
    # Configurar variables globales
    YES="$yes"
    FORCE="$force"
    
    log "🌿 Creando rama con convenciones de Veld Framework"
    
    # Validar tipo
    if ! validate_type "$type"; then
        exit 1
    fi
    
    # Limpiar y validar descripción
    local clean_desc=$(clean_description "$description")
    if ! validate_description "$type" "$clean_desc"; then
        exit 1
    fi
    
    # Confirmar creación
    local branch_name="${type}/${clean_desc}"
    if ! confirm_action "¿Crear rama '$branch_name'?"; then
        echo "Creación de rama cancelada"
        exit 0
    fi
    
    # Crear la rama
    if ! create_branch "$type" "$clean_desc"; then
        exit 1
    fi
    
    # Opcional: hacer checkout
    if [ "$checkout" = true ]; then
        checkout_branch "$branch_name"
    fi
    
    # Opcional: hacer push
    if [ "$push" = true ]; then
        if confirm_action "¿Hacer push de la rama al repositorio remoto?"; then
            push_branch "$branch_name"
        fi
    fi
    
    # Mostrar próximos pasos
    show_next_steps "$type" "$clean_desc"
    
    success "¡Rama '$branch_name' lista para usar!"
}

# Ejecutar función principal
main "$@"