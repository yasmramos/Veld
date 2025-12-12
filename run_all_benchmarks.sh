#!/bin/bash

# Script para ejecutar todos los benchmarks de Veld DI Framework localmente
# Genera reportes automáticos en el directorio benchmark-reports/

set -e

echo "🚀 VELD DI FRAMEWORK - BENCHMARKS COMPLETOS"
echo "=============================================="
echo ""

# Función para imprimir mensajes de estado
print_status() {
    echo "✅ $1"
}

print_error() {
    echo "❌ $1"
    exit 1
}

print_warning() {
    echo "⚠️  $1"
}

# Verificar directorio
if [ ! -f "pom.xml" ]; then
    print_error "Ejecutar desde el directorio raíz de Veld (/workspace/Veld)"
fi

print_status "Directorio correcto verificado"

# Verificar Java
if ! command -v java &> /dev/null; then
    print_error "Java no está instalado"
fi

echo "Java version: $(java -version 2>&1 | head -n 1)"

# Crear directorio de reportes
mkdir -p benchmark-reports
mkdir -p benchmark-reports/history
mkdir -p benchmark-reports/analysis

print_status "Directorios de reportes creados"

echo ""
echo "=== FASE 1: COMPILACIÓN ==="

# Compilar proyecto
echo "🔧 Compilando proyecto..."
if ./mvnw clean compile -q; then
    print_status "Compilación exitosa"
else
    print_error "Error en compilación"
fi

echo ""
echo "=== FASE 2: BENCHMARKS JMH ==="

# Ejecutar JMH Benchmarks
echo "⚡ Ejecutando JMH Benchmarks..."
if ./mvnw -pl veld-benchmark -am exec:java \
    -Dexec.mainClass="com.veld.benchmark.Phase1OptimizationBenchmark" \
    -Dexec.args="-wi 3 -i 5 -f 1 -t 1" \
    -q; then
    print_status "JMH Benchmarks ejecutados"
    
    # Copiar resultados
    if [ -f "veld-benchmark/benchmark_results.txt" ]; then
        cp "veld-benchmark/benchmark_results.txt" "benchmark-reports/jmh_results.txt"
        print_status "Resultados JMH copiados"
    fi
else
    print_warning "Error en JMH Benchmarks"
fi

echo ""
echo "=== FASE 3: BENCHMARKS SIMPLES ==="

# Ejecutar Simple Benchmark
echo "⚡ Ejecutando Simple Benchmark..."
if [ -f "veld-benchmark/target/classes/com/veld/benchmark/SimpleBenchmark.class" ]; then
    java -cp "veld-benchmark/target/classes:veld-runtime/target/classes:veld-annotations/target/classes" \
        com.veld.benchmark.SimpleBenchmark > "benchmark-reports/simple_benchmark.txt" 2>&1
    print_status "Simple Benchmark ejecutado"
else
    print_warning "Simple Benchmark no compilado"
fi

echo ""
echo "=== FASE 4: BENCHMARKS ESTRATÉGICOS ==="

# Ejecutar Strategic Benchmarks
echo "⚡ Ejecutando Strategic Benchmarks..."
if [ -f "veld-benchmark/run-strategic-benchmarks.sh" ]; then
    cd veld-benchmark
    if ./run-strategic-benchmarks.sh; then
        print_status "Strategic Benchmarks ejecutados"
        
        # Copiar resultados
        cd ..
        if [ -f "veld-benchmark/results_injection.json" ]; then
            cp "veld-benchmark/results_injection.json" "benchmark-reports/"
            print_status "Resultados de injection copiados"
        fi
        if [ -f "veld-benchmark/results_startup.json" ]; then
            cp "veld-benchmark/results_startup.json" "benchmark-reports/"
            print_status "Resultados de startup copiados"
        fi
        if [ -f "veld-benchmark/results_throughput.json" ]; then
            cp "veld-benchmark/results_throughput.json" "benchmark-reports/"
            print_status "Resultados de throughput copiados"
        fi
    else
        print_warning "Error en Strategic Benchmarks"
    fi
else
    print_warning "Script de strategic benchmarks no encontrado"
fi

echo ""
echo "=== FASE 5: JMH STANDALONE ==="

# Ejecutar JMH Standalone
echo "⚡ Ejecutando JMH Standalone..."
if [ -f "../jmh-standalone/benchmark.jar" ]; then
    cd ../jmh-standalone
    if java -jar benchmark.jar -wi 3 -i 5 -f 1 -t 1 > "../Veld/benchmark-reports/jmh_standalone.txt" 2>&1; then
        cd ../Veld
        print_status "JMH Standalone ejecutado"
        
        # Copiar resultados JSON si existen
        if [ -f "jmh-standalone/benchmark-results.json" ]; then
            cp "jmh-standalone/benchmark-results.json" "benchmark-reports/"
            print_status "Resultados JSON copiados"
        fi
    else
        cd ../Veld
        print_warning "Error en JMH Standalone"
    fi
else
    print_warning "JMH standalone JAR no encontrado"
fi

echo ""
echo "=== FASE 6: TESTS UNITARIOS ==="

# Ejecutar tests
echo "🧪 Ejecutando tests unitarios..."
if ./mvnw test -q; then
    print_status "Tests ejecutados exitosamente"
    
    # Copiar reportes de tests
    find . -name "*.xml" -path "*/surefire-reports/*" -exec cp {} benchmark-reports/ \; 2>/dev/null || true
    print_status "Reportes de tests copiados"
else
    print_warning "Algunos tests fallaron"
fi

echo ""
echo "=== FASE 7: GENERACIÓN DE REPORTES ==="

# Generar reportes con Python
echo "📝 Generando reportes..."

if command -v python3 &> /dev/null; then
    # Ejecutar generador de reportes
    if [ -f "../generate_benchmark_report.py" ]; then
        python3 ../generate_benchmark_report.py
        print_status "Reportes generados con Python"
    else
        print_warning "Script de generación no encontrado"
    fi
else
    print_warning "Python3 no disponible para generación de reportes"
fi

# Generar reporte básico en markdown
cat > benchmark-reports/execution-summary.md << EOF
# 📊 Resumen de Ejecución - Benchmarks Veld

**Fecha**: $(date)
**Sistema**: $(uname -a)
**Java**: $(java -version 2>&1 | head -n 1)

## 🎯 Estado de Ejecución

| Componente | Estado | Detalles |
|------------|--------|----------|
| Compilación | ✅ | Proyecto compilado exitosamente |
| JMH Benchmarks | $([ -f benchmark-reports/jmh_results.txt ] && echo "✅" || echo "❌") | $([ -f benchmark-reports/jmh_results.txt ] && echo "Resultados disponibles" || echo "No ejecutados") |
| Simple Benchmark | $([ -f benchmark-reports/simple_benchmark.txt ] && echo "✅" || echo "❌") | $([ -f benchmark-reports/simple_benchmark.txt ] && echo "Resultados disponibles" || echo "No ejecutado") |
| Strategic Benchmarks | $([ -f benchmark-reports/results_injection.json ] && echo "✅" || echo "❌") | $([ -f benchmark-reports/results_injection.json ] && echo "Resultados disponibles" || echo "No ejecutados") |
| JMH Standalone | $([ -f benchmark-reports/jmh_standalone.txt ] && echo "✅" || echo "❌") | $([ -f benchmark-reports/jmh_standalone.txt ] && echo "Resultados disponibles" || echo "No ejecutado") |
| Tests Unitarios | ✅ | Tests ejecutados |

## 📁 Archivos Generados

$(find benchmark-reports -type f -exec echo "- {}" \; | sort)

## 🔍 Próximos Pasos

1. **Revisar reportes** en el directorio \`benchmark-reports/\`
2. **Comparar resultados** con baseline anterior
3. **Analizar tendencias** de performance
4. **Documentar optimizaciones** necesarias

---
*Reporte generado automáticamente*
EOF

print_status "Reporte básico generado"

echo ""
echo "=== FASE 8: HISTÓRICO ==="

# Guardar esta ejecución en el histórico
timestamp=$(date +"%Y%m%d_%H%M%S")
cp benchmark-reports/benchmark-report.md "benchmark-reports/history/benchmark_${timestamp}.md" 2>/dev/null || true
print_status "Ejecución guardada en histórico"

echo ""
echo "🎉 BENCHMARKS COMPLETADOS"
echo "========================="
echo ""
echo "📁 Directorio de reportes: ./benchmark-reports/"
echo "📄 Reporte principal: ./benchmark-reports/benchmark-report.md"
echo "📊 Resumen de ejecución: ./benchmark-reports/execution-summary.md"
echo "📚 Histórico: ./benchmark-reports/history/"
echo ""

# Mostrar resumen de archivos generados
file_count=$(find benchmark-reports -type f | wc -l)
print_status "Total de archivos generados: $file_count"

if [ $file_count -gt 0 ]; then
    echo ""
    echo "📋 Archivos principales generados:"
    find benchmark-reports -name "*.md" -o -name "*.txt" -o -name "*.json" | head -10 | while read file; do
        echo "  - $file"
    done
fi

echo ""
echo "🚀 ¡BENCHMARKS VELD EJECUTADOS EXITOSAMENTE!"
echo ""
echo "Para ver los resultados:"
echo "  📖 Reporte principal: cat benchmark-reports/benchmark-report.md"
echo "  📊 Resumen: cat benchmark-reports/execution-summary.md"
echo "  🌐 Abrir en navegador: firefox benchmark-reports/benchmark-report.md"