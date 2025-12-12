#!/bin/bash

echo "🔧 COMPILACIÓN MANUAL DE VELD - SIN DEPENDENCIAS MAVEN"
echo "======================================================"
echo ""

# Variables
JAVA_HOME=/workspace/jdk-11.0.2
BUILD_DIR=/workspace/Veld/manual-build
ANNOTATIONS_SRC=/workspace/Veld/veld-annotations/src/main/java
RUNTIME_SRC=/workspace/Veld/veld-runtime/src/main/java

# Función para imprimir mensajes
print_status() {
    echo "✅ $1"
}

print_error() {
    echo "❌ $1"
    exit 1
}

# Crear directorio de build
mkdir -p $BUILD_DIR
print_status "Directorio de build creado"

# Compilar annotations
echo ""
echo "🔧 Compilando veld-annotations..."
cd $ANNOTATIONS_SRC
$JAVA_HOME/bin/javac -d $BUILD_DIR/veld-annotations io/github/yasmramos/annotation/*.java

if [ $? -eq 0 ]; then
    print_status "veld-annotations compilado"
    annotation_count=$(find $BUILD_DIR/veld-annotations -name "*.class" | wc -l)
    print_status "Archivos .class generados: $annotation_count"
else
    print_error "Error compilando veld-annotations"
fi

# Compilar runtime
echo ""
echo "🔧 Compilando veld-runtime..."
cd $RUNTIME_SRC
$JAVA_HOME/bin/javac -d $BUILD_DIR/veld-runtime -cp $BUILD_DIR/veld-annotations $(find . -name "*.java" ! -name "module-info.java")

if [ $? -eq 0 ]; then
    print_status "veld-runtime compilado"
    runtime_count=$(find $BUILD_DIR/veld-runtime -name "*.class" | wc -l)
    print_status "Archivos .class generados: $runtime_count"
else
    print_error "Error compilando veld-runtime"
fi

# Crear JARs
echo ""
echo "📦 Creando archivos JAR..."

cd $BUILD_DIR/veld-annotations
jar cf ../../veld-annotations.jar .
if [ $? -eq 0 ]; then
    print_status "veld-annotations.jar creado"
else
    print_error "Error creando veld-annotations.jar"
fi

cd ../veld-runtime
jar cf ../../veld-runtime.jar .
if [ $? -eq 0 ]; then
    print_status "veld-runtime.jar creado"
else
    print_error "Error creando veld-runtime.jar"
fi

# Verificar JARs
echo ""
echo "📁 Verificando archivos generados..."
cd /workspace/Veld
if [ -f "veld-annotations.jar" ]; then
    size=$(du -h veld-annotations.jar | cut -f1)
    print_status "veld-annotations.jar ($size) creado"
fi

if [ -f "veld-runtime.jar" ]; then
    size=$(du -h veld-runtime.jar | cut -f1)
    print_status "veld-runtime.jar ($size) creado"
fi

# Listar contenido de los JARs
echo ""
echo "📋 Contenido de veld-annotations.jar:"
jar tf veld-annotations.jar | head -10

echo ""
echo "📋 Contenido de veld-runtime.jar (primeras 10 clases):"
jar tf veld-runtime.jar | grep "\.class" | head -10

echo ""
echo "🎉 COMPILACIÓN MANUAL COMPLETADA"
echo "================================="
echo ""
echo "✅ Archivos JAR creados:"
echo "  📦 veld-annotations.jar - Annotations de Veld"
echo "  📦 veld-runtime.jar - Runtime de Veld"
echo ""
echo "🚀 Ahora puedes usar estos JARs para compilar otros módulos:"
echo "  javac -cp veld-annotations.jar:veld-runtime.jar [archivos]"
echo ""
echo "📁 Ubicación: /workspace/Veld/"

print_status "¡Proceso completado exitosamente!"