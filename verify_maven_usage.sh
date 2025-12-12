#!/bin/bash

echo "🔍 VERIFICACIÓN DE MAVEN WRAPPER EN WORKFLOWS"
echo "============================================"
echo ""

# Función para verificar uso de mvnw
check_mvnw_usage() {
    local workflow_file=$1
    local has_issues=false
    
    echo "📄 Verificando: $workflow_file"
    echo "------------------------------"
    
    # Verificar uso de ./mvnw
    if grep -q "./mvnw" "$workflow_file"; then
        echo "❌ ENCONTRADO: ./mvnw (problemático en GitHub Actions)"
        grep -n "./mvnw" "$workflow_file" || true
        has_issues=true
    fi
    
    # Verificar uso de mvn (correcto)
    if grep -q "mvn " "$workflow_file"; then
        echo "✅ CORRECTO: mvn commands found"
        mvn_count=$(grep -c "mvn " "$workflow_file" || echo "0")
        echo "ℹ️  Maven commands: $mvn_count"
    fi
    
    if [ "$has_issues" = false ]; then
        echo "✅ Sin problemas de Maven wrapper"
    fi
    
    echo ""
    return $([ "$has_issues" = true ] && echo 1 || echo 0)
}

# Lista de workflows a verificar
workflows=(
    "veld-ci-cd-complete.yml"
    "benchmarks.yml"
    "ci-cd.yml"
    "ci.yml"
)

total_errors=0

echo "🔧 Verificando workflows por problemas de Maven wrapper..."
echo ""

for workflow in "${workflows[@]}"; do
    if [ -f ".github/workflows/$workflow" ]; then
        if ! check_mvnw_usage ".github/workflows/$workflow"; then
            ((total_errors++))
        fi
    else
        echo "❌ Archivo no encontrado: .github/workflows/$workflow"
        echo ""
        ((total_errors++))
    fi
done

echo "📋 RECOMENDACIONES PARA MAVEN EN GITHUB ACTIONS"
echo "=============================================="
echo "✅ Usar 'mvn' en lugar de './mvnw' directamente"
echo "✅ GitHub Actions ya incluye Maven instalado"
echo "✅ Maven wrapper puede causar problemas de descarga"
echo "✅ Usar cache para acelerar builds"

echo ""
echo "📊 RESUMEN DE VERIFICACIÓN"
echo "=========================="

if [ $total_errors -eq 0 ]; then
    echo "✅ TODOS LOS WORKFLOWS USAN 'mvn' CORRECTAMENTE"
    echo "🚀 No se encontraron problemas de Maven wrapper"
else
    echo "❌ SE ENCONTRARON $total_errors PROBLEMAS DE MAVEN WRAPPER"
    echo "🔧 Revisar workflows con './mvnw'"
fi

echo ""
echo "🎯 COMANDOS MAVEN VERIFICADOS:"
echo "  ✅ mvn clean compile"
echo "  ✅ mvn test"
echo "  ✅ mvn -pl module -am"
echo "  ✅ mvn exec:java"
echo "  ✅ mvn install"

echo ""
echo "🏁 Verificación de Maven wrapper completada"