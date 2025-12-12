#!/bin/bash

echo "🔍 VERIFICACIÓN DE WORKFLOW DE BENCHMARKS CORREGIDO"
echo "=================================================="
echo ""

# Función para verificar configuración del workflow
check_benchmark_workflow() {
    local workflow_file=".github/workflows/benchmarks.yml"
    local has_issues=false
    
    echo "📄 Verificando: $workflow_file"
    echo "------------------------------"
    
    # Verificar que no use clases inexistentes
    if grep -q "com.veld.benchmark.Phase1OptimizationBenchmark" "$workflow_file"; then
        echo "❌ ENCONTRADO: Clase inexistente com.veld.benchmark.Phase1OptimizationBenchmark"
        has_issues=true
    fi
    
    # Verificar que use el enfoque simplificado
    if grep -q "SimpleBenchmarkTest" "$workflow_file"; then
        echo "✅ CORRECTO: Usa SimpleBenchmarkTest"
    else
        echo "⚠️  ADVERTENCIA: No usa SimpleBenchmarkTest"
    fi
    
    # Verificar que compile Veld manualmente
    if grep -q "javac.*annotation" "$workflow_file"; then
        echo "✅ CORRECTO: Compila Veld annotations manualmente"
    else
        echo "⚠️  ADVERTENCIA: No compila Veld manualmente"
    fi
    
    # Verificar que no use mvn -pl que puede fallar
    if grep -q "mvn -pl.*exec:java" "$workflow_file"; then
        echo "❌ ENCONTRADO: Uso de mvn -pl exec:java (problemático)"
        has_issues=true
    else
        echo "✅ CORRECTO: No usa mvn -pl exec:java"
    fi
    
    if [ "$has_issues" = false ]; then
        echo "✅ Workflow configurado correctamente"
    fi
    
    echo ""
    return $([ "$has_issues" = true ] && echo 1 || echo 0)
}

# Verificar archivo de workflow
if [ -f ".github/workflows/benchmarks.yml" ]; then
    if ! check_benchmark_workflow; then
        echo "❌ SE ENCONTRARON PROBLEMAS EN EL WORKFLOW"
        exit 1
    fi
else
    echo "❌ Archivo de workflow no encontrado"
    exit 1
fi

echo "📋 CONFIGURACIÓN ESPERADA DEL WORKFLOW"
echo "======================================"
echo "✅ Compilación manual de Veld (sin Maven dependency issues)"
echo "✅ Benchmark simple con Java puro"
echo "✅ Verificación de funcionalidad básica de Veld"
echo "✅ Sin uso de clases inexistentes"
echo "✅ Sin mvn -pl exec:java problemático"

echo ""
echo "🏁 Verificación completada exitosamente"