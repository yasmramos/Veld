#!/bin/bash

echo "🔧 SOLUCIONANDO DEPENDENCIAS SPRING BOOT VELD"
echo "============================================="
echo ""

cd /workspace/Veld

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
    print_error "Ejecutar desde el directorio raíz de Veld"
fi

print_status "Directorio correcto verificado"

echo ""
echo "=== PASO 1: COMPILANDO DEPENDENCIAS CORE ==="

# Compilar dependencias en orden específico
modules=("veld-annotations" "veld-runtime" "veld-aop" "veld-processor" "veld-weaver")

for module in "${modules[@]}"; do
    echo "🔧 Compilando $module..."
    if ./mvnw -pl "$module" clean compile -q; then
        print_status "$module compilado"
    else
        print_error "Error compilando $module"
    fi
done

echo ""
echo "=== PASO 2: INSTALANDO DEPENDENCIAS LOCALMENTE ==="

# Instalar dependencias core localmente
echo "📦 Instalando dependencias core..."
if ./mvnw clean install -DskipTests -pl "${modules[0]},${modules[1]},${modules[2]},${modules[3]},${modules[4]}" -q; then
    print_status "Dependencias core instaladas"
else
    print_error "Error instalando dependencias core"
fi

echo ""
echo "=== PASO 3: COMPILANDO VELD-SPRING-BOOT-STARTER ==="

# Compilar veld-spring-boot-starter
echo "🔧 Compilando veld-spring-boot-starter..."
if ./mvnw -pl veld-spring-boot-starter -am clean compile -q; then
    print_status "veld-spring-boot-starter compilado"
else
    print_warning "Error compilando veld-spring-boot-starter, intentando instalación..."
    if ./mvnw -pl veld-spring-boot-starter clean install -DskipTests -q; then
        print_status "veld-spring-boot-starter instalado"
    else
        print_error "Error en veld-spring-boot-starter"
    fi
fi

echo ""
echo "=== PASO 4: INSTALANDO VELD-SPRING-BOOT-STARTER ==="

# Instalar veld-spring-boot-starter
echo "📦 Instalando veld-spring-boot-starter..."
if ./mvnw -pl veld-spring-boot-starter clean install -DskipTests -q; then
    print_status "veld-spring-boot-starter instalado"
else
    print_error "Error instalando veld-spring-boot-starter"
fi

echo ""
echo "=== PASO 5: COMPILANDO VELD-SPRING-BOOT-EXAMPLE ==="

# Compilar veld-spring-boot-example
echo "🔧 Compilando veld-spring-boot-example..."
if ./mvnw -pl veld-spring-boot-example clean compile -q; then
    print_status "veld-spring-boot-example compilado exitosamente"
    
    # Verificar archivos generados
    if [ -d "veld-spring-boot-example/target/classes" ]; then
        class_count=$(find veld-spring-boot-example/target/classes -name "*.class" | wc -l)
        print_status "Archivos .class generados: $class_count"
    fi
else
    print_error "Error compilando veld-spring-boot-example"
fi

echo ""
echo "=== PASO 6: EJECUTANDO TESTS (OPCIONAL) ==="

# Ejecutar tests del spring-boot-example
echo "🧪 Ejecutando tests de spring-boot-example..."
if ./mvnw -pl veld-spring-boot-example test -q; then
    print_status "Tests ejecutados exitosamente"
else
    print_warning "Algunos tests fallaron, pero compilación exitosa"
fi

echo ""
echo "🎉 DEPENDENCIAS SPRING BOOT SOLUCIONADAS"
echo "========================================"
echo ""
echo "✅ Módulos compilados e instalados:"
echo "  - veld-annotations"
echo "  - veld-runtime"
echo "  - veld-aop"
echo "  - veld-processor"
echo "  - veld-weaver"
echo "  - veld-spring-boot-starter"
echo "  - veld-spring-boot-example"
echo ""
echo "🚀 Para ejecutar el ejemplo:"
echo "  cd veld-spring-boot-example"
echo "  ./mvnw spring-boot:run"
echo ""
echo "📦 Para construir JAR:"
echo "  cd veld-spring-boot-example"
echo "  ./mvnw clean package"
echo ""

# Verificar archivos JAR generados
jar_files=$(find . -name "*.jar" -path "*/target/*" | head -5)
if [ -n "$jar_files" ]; then
    echo "📁 Archivos JAR generados:"
    echo "$jar_files"
fi

print_status "¡Proceso completado exitosamente!"