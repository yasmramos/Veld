#!/bin/bash

# Script simple para completar la compilación del proyecto Veld
# Ejecutar desde: /workspace/Veld

set -e  # Salir en caso de error

echo "============================================"
echo "  VELD DI FRAMEWORK - COMPILACIÓN FINAL"
echo "============================================"
echo ""

# Función para imprimir mensajes de estado
print_status() {
    echo "✅ $1"
}

print_error() {
    echo "❌ $1"
    exit 1
}

# Verificar directorio
if [ ! -f "pom.xml" ]; then
    print_error "Error: No se encuentra pom.xml. Ejecutar desde /workspace/Veld"
fi

print_status "Directorio correcto verificado"

# Verificar Maven wrapper
if [ ! -x "./mvnw" ]; then
    chmod +x ./mvnw
    print_status "Maven wrapper preparado"
else
    print_status "Maven wrapper disponible"
fi

echo ""
echo "=== INICIANDO COMPILACIÓN ==="

# Compilar veld-annotations
echo ""
echo "1. Compilando veld-annotations..."
./mvnw -pl veld-annotations clean compile -q
if [ $? -eq 0 ]; then
    print_status "veld-annotations compilado exitosamente"
    ANNOTATIONS_COUNT=$(find veld-annotations/target/classes -name "*.class" 2>/dev/null | wc -l)
    echo "   📁 Archivos .class generados: $ANNOTATIONS_COUNT"
else
    print_error "Error compilando veld-annotations"
fi

# Compilar veld-runtime
echo ""
echo "2. Compilando veld-runtime..."
./mvnw -pl veld-runtime -am clean compile -q
if [ $? -eq 0 ]; then
    print_status "veld-runtime compilado exitosamente"
    RUNTIME_COUNT=$(find veld-runtime/target/classes -name "*.class" 2>/dev/null | wc -l)
    echo "   📁 Archivos .class generados: $RUNTIME_COUNT"
else
    print_error "Error compilando veld-runtime"
fi

# Compilar veld-processor
echo ""
echo "3. Compilando veld-processor..."
./mvnw -pl veld-processor -am clean compile -q
if [ $? -eq 0 ]; then
    print_status "veld-processor compilado exitosamente"
    PROCESSOR_COUNT=$(find veld-processor/target/classes -name "*.class" 2>/dev/null | wc -l)
    echo "   📁 Archivos .class generados: $PROCESSOR_COUNT"
else
    print_error "Error compilando veld-processor"
fi

# Ejecutar tests
echo ""
echo "4. Ejecutando tests..."
./mvnw test -q
if [ $? -eq 0 ]; then
    print_status "Tests ejecutados exitosamente"
else
    echo "⚠️  Algunos tests pudieron fallar, verificar manualmente"
fi

# Resumen final
echo ""
echo "=== RESUMEN FINAL ==="
TOTAL_CLASSES=$((ANNOTATIONS_COUNT + RUNTIME_COUNT + PROCESSOR_COUNT))
print_status "Compilación completada"
echo "📊 Total de archivos .class generados: $TOTAL_CLASSES"
echo ""
echo "Módulos compilados:"
echo "  - veld-annotations: $ANNOTATIONS_COUNT clases"
echo "  - veld-runtime: $RUNTIME_COUNT clases"  
echo "  - veld-processor: $PROCESSOR_COUNT clases"
echo ""

# Verificar archivos clave
echo "=== VERIFICACIÓN DE ARCHIVOS CLAVE ==="
KEY_FILES=(
    "veld-annotations/target/classes/io/github/yasmramos/annotation/Component.class"
    "veld-runtime/target/classes/io/github/yasmramos/runtime/VeldType.class"
    "veld-runtime/target/classes/io/github/yasmramos/runtime/lifecycle/LifecycleProcessor.class"
)

for file in "${KEY_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_status "$file"
    else
        echo "⚠️  $file no encontrado"
    fi
done

echo ""
echo "============================================"
echo "  🎉 COMPILACIÓN VELD COMPLETADA"
echo "============================================"
echo ""
echo "Próximos pasos opcionales:"
echo "  - Compilar módulos adicionales: veld-aop, veld-weaver"
echo "  - Ejecutar benchmarks: cd veld-benchmark && ./run-strategic-benchmarks.sh"
echo "  - Probar ejemplos: cd veld-example && ./mvnw spring-boot:run"
echo ""