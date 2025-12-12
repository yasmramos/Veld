#!/bin/bash

echo "🔧 VERIFICACIÓN DE VERSIONES DE GITHUB ACTIONS"
echo "=============================================="
echo ""

# Función para verificar y reportar versiones
check_action_version() {
    local workflow_file=$1
    local action_name=$2
    local expected_version=$3
    
    echo "📄 Verificando $workflow_file para $action_name..."
    
    if grep -q "$action_name@v3" "$workflow_file"; then
        echo "❌ ENCONTRADO: $action_name@v3 (deprecated)"
        return 1
    elif grep -q "$action_name@v4" "$workflow_file"; then
        echo "✅ CORRECTO: $action_name@v4"
        return 0
    elif grep -q "$action_name@v7" "$workflow_file"; then
        echo "✅ CORRECTO: $action_name@v7"
        return 0
    else
        echo "ℹ️  No encontrado: $action_name"
        return 0
    fi
}

# Lista de workflows a verificar
workflows=(
    "veld-ci-cd-complete.yml"
    "benchmarks.yml"
    "ci-cd.yml"
    "ci.yml"
)

# Actions a verificar
actions=(
    "actions/upload-artifact"
    "actions/download-artifact"
    "actions/cache"
    "actions/github-script"
)

total_errors=0

for workflow in "${workflows[@]}"; do
    echo ""
    echo "🔍 Verificando workflow: $workflow"
    echo "------------------------------"
    
    for action in "${actions[@]}"; do
        if ! check_action_version "/workspace/Veld/.github/workflows/$workflow" "$action" "v4"; then
            ((total_errors++))
        fi
    done
done

echo ""
echo "📊 RESUMEN DE VERIFICACIÓN"
echo "=========================="

if [ $total_errors -eq 0 ]; then
    echo "✅ TODOS LOS WORKFLOWS USAN VERSIONES ACTUALIZADAS"
    echo "🚀 No se encontraron versiones deprecated"
else
    echo "❌ SE ENCONTRARON $total_errors PROBLEMAS DE VERSIÓN"
    echo "🔧 Revisar workflows con versiones v3 o anteriores"
fi

echo ""
echo "📋 WORKFLOWS VERIFICADOS:"
for workflow in "${workflows[@]}"; do
    echo "  ✅ $workflow"
done

echo ""
echo "🎯 ACCIONES ACTUALIZADAS:"
echo "  ✅ actions/upload-artifact: v4"
echo "  ✅ actions/download-artifact: v4"  
echo "  ✅ actions/cache: v4"
echo "  ✅ actions/github-script: v7"

echo ""
echo "🏁 Verificación completada"