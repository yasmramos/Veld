#!/bin/bash

echo "=== COMPILANDO TODO EL PROYECTO ==="
cd /workspace/Veld

echo "1. Limpieza..."
./mvnw clean

echo "2. Compilación..."
./mvnw compile -DskipTests

echo "3. Ejecución de tests..."
./mvnw test

echo "=== BUILD Y TESTS COMPLETADOS ==="