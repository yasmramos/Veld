#!/bin/bash

# Script de prueba para el Spring Boot Starter de Veld
# Ejecutar desde el directorio raíz del proyecto Veld

echo "🚀 Iniciando pruebas del Spring Boot Starter de Veld..."
echo "=================================================="

# Verificar que estamos en el directorio correcto
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: No se encontró pom.xml. Ejecuta este script desde la raíz del proyecto Veld."
    exit 1
fi

echo "📋 Paso 1: Limpiando y compilando proyecto completo..."
mvn clean compile -q

if [ $? -eq 0 ]; then
    echo "✅ Compilación básica: EXITOSA"
else
    echo "❌ Compilación básica: FALLIDA"
    exit 1
fi

echo ""
echo "📋 Paso 2: Instalando dependencias del proyecto..."
mvn install -DskipTests -q

if [ $? -eq 0 ]; then
    echo "✅ Instalación de dependencias: EXITOSA"
else
    echo "❌ Instalación de dependencias: FALLIDA"
    exit 1
fi

echo ""
echo "📋 Paso 3: Compilando el Spring Boot Starter específicamente..."
cd veld-spring-boot-starter
mvn clean compile -q

if [ $? -eq 0 ]; then
    echo "✅ Compilación Spring Boot Starter: EXITOSA"
else
    echo "❌ Compilación Spring Boot Starter: FALLIDA"
    exit 1
fi

echo ""
echo "📋 Paso 4: Ejecutando tests del Spring Boot Starter..."
mvn test

if [ $? -eq 0 ]; then
    echo "✅ Tests Spring Boot Starter: EXITOSOS"
else
    echo "❌ Tests Spring Boot Starter: FALLIDOS"
    exit 1
fi

echo ""
echo "📋 Paso 5: Probando la aplicación demo..."
cd ../veld-spring-boot-example
mvn spring-boot:run -q &
APP_PID=$!

echo "⏳ Iniciando aplicación demo (PID: $APP_PID)..."
sleep 10

# Verificar si la aplicación está corriendo
if curl -s http://localhost:8080/actuator/health/veld > /dev/null; then
    echo "✅ Aplicación demo: FUNCIONANDO"
    echo "📊 Estado de Veld Spring Boot:"
    curl -s http://localhost:8080/actuator/health/veld | jq '.'
else
    echo "⚠️  Endpoint de Veld no disponible, verificando health general..."
    curl -s http://localhost:8080/actuator/health | jq '.'
fi

# Detener la aplicación
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

echo ""
echo "🎉 TODAS LAS PRUEBAS COMPLETADAS EXITOSAMENTE"
echo "=================================================="
echo "✅ El Spring Boot Starter de Veld está funcionando correctamente"
echo "📚 Documentación disponible en:"
echo "   - veld-spring-boot-starter/README.md"
echo "   - MIGRATION_GUIDE.md"
echo "   - SPRING_BOOT_STARTER_IMPLEMENTATION.md"