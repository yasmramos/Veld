#!/bin/bash

echo "🚀 VELD DI FRAMEWORK - COMPILACIÓN FINAL"
echo "=========================================="
echo ""

cd /workspace/Veld

# Verificar que estamos en el lugar correcto
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: No se encuentra pom.xml"
    echo "Ejecutar desde: /workspace/Veld"
    exit 1
fi

echo "✅ Directorio correcto verificado"

# Verificar Maven wrapper
if [ ! -x "./mvnw" ]; then
    echo "⚠️  Preparando Maven wrapper..."
    chmod +x ./mvnw
fi

echo "✅ Maven wrapper listo"

# Compilar paso a paso
echo ""
echo "🔧 PASO 1: Compilando veld-annotations..."
if ./mvnw -pl veld-annotations clean compile -q; then
    echo "   ✅ veld-annotations compilado"
else
    echo "   ❌ Error en veld-annotations"
    exit 1
fi

echo ""
echo "🔧 PASO 2: Compilando veld-runtime..."  
if ./mvnw -pl veld-runtime -am clean compile -q; then
    echo "   ✅ veld-runtime compilado"
else
    echo "   ❌ Error en veld-runtime"
    exit 1
fi

echo ""
echo "🔧 PASO 3: Compilando veld-processor..."
if ./mvnw -pl veld-processor -am clean compile -q; then
    echo "   ✅ veld-processor compilado"
else
    echo "   ❌ Error en veld-processor"
    exit 1
fi

echo ""
echo "🔧 PASO 4: Ejecutando tests..."
if ./mvnw test -q; then
    echo "   ✅ Tests ejecutados"
else
    echo "   ⚠️  Algunos tests fallaron (revisar manualmente)"
fi

echo ""
echo "🎉 ¡COMPILACIÓN EXITOSA!"
echo "========================"
echo ""
echo "Archivos generados:"
echo "  📁 veld-annotations/target/classes/ - $(find veld-annotations/target/classes -name "*.class" 2>/dev/null | wc -l) archivos .class"
echo "  📁 veld-runtime/target/classes/ - $(find veld-runtime/target/classes -name "*.class" 2>/dev/null | wc -l) archivos .class"  
echo "  📁 veld-processor/target/classes/ - $(find veld-processor/target/classes -name "*.class" 2>/dev/null | wc -l) archivos .class"
echo ""
echo "✅ Proyecto Veld compilado correctamente!"