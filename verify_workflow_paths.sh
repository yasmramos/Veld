#!/bin/bash

echo "🔍 VERIFICACIÓN DE RUTAS EN WORKFLOWS GITHUB ACTIONS"
echo "===================================================="
echo ""

# Función para verificar rutas problemáticas
check_workflow_paths() {
    local workflow_file=$1
    local has_issues=false
    
    echo "📄 Verificando: $workflow_file"
    echo "--------------------------------"
    
    # Verificar rutas absolutas problemáticas
    if grep -q "/workspace" "$workflow_file"; then
        echo "❌ ENCONTRADO: Rutas /workspace (no existen en GitHub Actions)"
        grep -n "/workspace" "$workflow_file" || true
        has_issues=true
    fi
    
    # Verificar cd a directorios absolutos
    if grep -q "cd /[a-zA-Z]" "$workflow_file"; then
        echo "⚠️  ADVERTENCIA: cd a directorios absolutos detectados"
        grep -n "cd /[a-zA-Z]" "$workflow_file" || true
    fi
    
    # Verificar paths absolutos en artifact uploads
    if grep -q "path:.*/" "$workflow_file"; then
        echo "ℹ️  Verificando artifact paths:"
        grep -A 3 -B 1 "path:" "$workflow_file" || true
    fi
    
    if [ "$has_issues" = false ]; then
        echo "✅ Sin problemas de rutas encontrados"
    fi
    
    echo ""
}

# Lista de workflows a verificar
workflows=(
    "veld-ci-cd-complete.yml"
    "benchmarks.yml"
    "ci-cd.yml"
    "ci.yml"
)

echo "🔧 Verificando workflows por rutas problemáticas..."
echo ""

for workflow in "${workflows[@]}"; do
    if [ -f ".github/workflows/$workflow" ]; then
        check_workflow_paths ".github/workflows/$workflow"
    else
        echo "❌ Archivo no encontrado: .github/workflows/$workflow"
        echo ""
    fi
done

echo "📋 RECOMENDACIONES PARA GITHUB ACTIONS"
echo "======================================"
echo "✅ Usar rutas relativas (./path o path)"
echo "✅ Evitar cd a directorios absolutos"
echo "✅ Usar $GITHUB_WORKSPACE para workspace base"
echo "✅ Los artifacts deben usar paths relativos"
echo "✅ Los comandos deben ser independientes del directorio"

echo ""
echo "🏁 Verificación de rutas completada"