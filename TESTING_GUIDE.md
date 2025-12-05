# Comandos para probar el Spring Boot Starter de Veld

## Método 1: Script Automático
```bash
chmod +x test-spring-boot-starter.sh
./test-spring-boot-starter.sh
```

## Método 2: Comandos Manuales

### 1. Compilar proyecto base
```bash
mvn clean install -DskipTests
```

### 2. Compilar Spring Boot Starter
```bash
cd veld-spring-boot-starter
mvn clean compile test
```

### 3. Ejecutar aplicación demo
```bash
cd ../veld-spring-boot-example
mvn spring-boot:run
```

### 4. Probar endpoints (en otra terminal)
```bash
# Health check de Veld
curl http://localhost:8080/actuator/health/veld

# Status general
curl http://localhost:8080/actuator/health

# API endpoints de ejemplo
curl http://localhost:8080/api/veld/status
curl http://localhost:8080/api/veld/beans
curl http://localhost:8080/api/veld/test
```

## Método 3: Verificación Rápida

### Solo compilación (más rápido)
```bash
# Compilar todo sin tests
mvn clean compile -q

# Verificar que los JARs se crearon
ls -la veld-spring-boot-starter/target/
ls -la veld-spring-boot-example/target/
```

### Verificar estructura del proyecto
```bash
# Verificar que todos los archivos están presentes
find veld-spring-boot-starter -name "*.java" | wc -l
find veld-spring-boot-example -name "*.java" | wc -l

# Debe mostrar:
# - veld-spring-boot-starter: 5 archivos Java
# - veld-spring-boot-example: 3 archivos Java
```

## Verificación de Resultados Esperados

### ✅ Compilación Exitosa si ves:
- "BUILD SUCCESS" al final
- Archivos .class en target/
- JAR sin errores

### ✅ Tests Exitosos si ves:
- "Tests run: X in Ys"
- "BUILD SUCCESS"

### ✅ Aplicación Demo Funcional si:
- Se inicia en puerto 8080
- Endpoints responden JSON válido
- No hay errores en logs

## Troubleshooting

### Si falla la compilación:
1. Verificar Java 17+: `java -version`
2. Verificar Maven 3.6+: `mvn -version`
3. Verificar conectividad a internet (para dependencias)

### Si fallan los tests:
1. Revisar logs en `target/surefire-reports/`
2. Verificar que no hay conflictos de puertos

### Si la app demo no inicia:
1. Verificar puerto 8080 libre
2. Revisar `target/spring-boot.log`
3. Probar con `mvn spring-boot:run -X` para debug