#!/bin/bash

echo "🔍 VERIFICACIÓN DE CONSTRUCCIÓN COMPLETA DE VELD BENCHMARK"
echo "========================================================="
echo ""

# Función para verificar construcción completa
check_complete_build() {
    local has_issues=false
    
    echo "📋 Verificando construcción del proyecto Veld..."
    echo "----------------------------------------------"
    
    # Verificar módulos core
    for module in veld-annotations veld-runtime veld-processor; do
        if [ -f "$module/target/$module-*.jar" ]; then
            echo "✅ $module: JAR generado correctamente"
        else
            echo "❌ $module: JAR no encontrado"
            has_issues=true
        fi
        
        if [ -d "$module/target/classes" ]; then
            class_count=$(find "$module/target/classes" -name "*.class" 2>/dev/null | wc -l)
            echo "ℹ️  $module: $class_count clases compiladas"
        else
            echo "⚠️  $module: No hay clases compiladas"
        fi
    done
    
    # Verificar benchmark module
    echo ""
    echo "📊 Verificando módulo benchmark..."
    echo "----------------------------------"
    if [ -f "veld-benchmark/target/veld-benchmark-*.jar" ]; then
        echo "✅ veld-benchmark: JAR generado correctamente"
    else
        echo "❌ veld-benchmark: JAR no encontrado"
        has_issues=true
    fi
    
    if [ -f "veld-benchmark/benchmark-results.json" ]; then
        echo "✅ Benchmark results: benchmark-results.json generado"
        echo "📊 Tamaño del archivo: $(wc -c < veld-benchmark/benchmark-results.json) bytes"
    else
        echo "⚠️  Benchmark results: benchmark-results.json no encontrado"
    fi
    
    if [ -f "veld-benchmark/startup-results.json" ]; then
        echo "✅ Startup results: startup-results.json generado"
    else
        echo "⚠️  Startup results: startup-results.json no encontrado"
    fi
    
    if [ -f "veld-benchmark/throughput-results.json" ]; then
        echo "✅ Throughput results: throughput-results.json generado"
    else
        echo "⚠️  Throughput results: throughput-results.json no encontrado"
    fi
    
    echo ""
    if [ "$has_issues" = false ]; then
        echo "✅ Construcción completa verificada exitosamente"
    else
        echo "❌ Se encontraron problemas en la construcción"
    fi
    
    echo ""
    return $([ "$has_issues" = true ] && echo 1 || echo 0)
}

# Verificar estructura del proyecto
echo "🔧 Verificando estructura del proyecto..."
echo "---------------------------------------"
if [ -f "pom.xml" ]; then
    echo "✅ pom.xml encontrado"
else
    echo "❌ pom.xml no encontrado"
    exit 1
fi

for module in veld-annotations veld-runtime veld-processor veld-benchmark; do
    if [ -d "$module" ]; then
        echo "✅ Módulo $module encontrado"
        if [ -f "$module/pom.xml" ]; then
            echo "✅  $module/pom.xml encontrado"
        else
            echo "❌  $module/pom.xml no encontrado"
        fi
    else
        echo "❌ Módulo $module no encontrado"
    fi
done

echo ""
echo "🚀 Verificando dependencias de Maven..."
echo "-------------------------------------"
# Verificar si Maven puede resolver dependencias
if command -v mvn >/dev/null 2>&1; then
    echo "✅ Maven disponible"
    mvn --version
else
    echo "❌ Maven no está disponible"
    exit 1
fi

echo ""
echo "📊 VERIFICACIÓN FINAL"
echo "==================="
check_complete_build

echo ""
echo "🎯 RECOMENDACIONES PARA CONSTRUCCIÓN COMPLETA"
echo "============================================="
echo "✅ Usar: mvn install -pl [módulos] -am -DskipTests"
echo "✅ Esto instala todos los módulos con sus dependencias"
echo "✅ El benchmark se construye con todas las dependencias resueltas"
echo "✅ Verificar que todos los JARs se generen correctamente"

echo ""
echo "🏁 Verificación de construcción completa completada"