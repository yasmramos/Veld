# Instrucciones de Compilacion - Proyecto Veld

## Estado Actual

### Archivos Java Corregidos
- ✅ `VeldType.java` - Constructor y referencias actualizadas
- ✅ `LifecycleProcessor.java` - Constructor package-private para testing
- ✅ `Main.java` - No usa LifecycleProcessor directamente

### Compilacion Exitosa
- ✅ `veld-annotations` - Compilado correctamente
- ❓ `veld-runtime` - Pendiente (depende de veld-annotations)

## Comandos de Compilacion

### Usar Maven Wrapper (Recomendado)
```bash
cd /workspace/Veld

# Compilar todo el proyecto
./mvnw clean compile

# Ejecutar tests
./mvnw test

# Compilar solo un modulo
./mvnw -pl veld-runtime -am clean compile test
```

### Usar Maven Local
```bash
cd /workspace/Veld

# Usar Maven local
/workspace/apache-maven-3.9.4/bin/mvn clean compile test

# O con variables de entorno
export JAVA_HOME=/workspace/jdk-11.0.2
export MAVEN_HOME=/workspace/apache-maven-3.9.4
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
mvn clean compile test
```

### Compilacion por Pasos
```bash
# 1. Limpiar
./mvnw clean

# 2. Compilar modulos base
./mvnw -pl veld-annotations clean compile

# 3. Compilar runtime
./mvnw -pl veld-runtime -am clean compile

# 4. Compilar processor
./mvnw -pl veld-processor -am clean compile

# 5. Ejecutar tests
./mvnw test
```

## Verificacion

### Archivos Compilados Esperados
```
veld-annotations/target/classes/com/veld/annotation/
├── Aspect.class
├── Component.class
├── Inject.class
└── Singleton.class

veld-runtime/target/classes/io/github/yasmramos/runtime/
├── Veld.class
├── VeldType.class
├── VeldConcurrentRegistry.class
└── lifecycle/LifecycleProcessor.class
```

### Test Reports Esperados
```
**/target/surefire-reports/*.xml
**/target/failsafe-reports/*.xml
```

## Solucion de Problemas

### Error: LifecycleProcessor() has private access
**Solucion**: ✅ Ya corregido - constructor es package-private

### Error: VeldTypeOptimized not found
**Solucion**: ✅ Ya corregido - archivo renombrado a VeldType

### Error: Main.java uses LifecycleProcessor directly
**Solucion**: ✅ Ya corregido - usa Veld.get() en su lugar

### Error: Java/Maven not found
```bash
# Verificar instalaciones
/workspace/jdk-11.0.2/bin/java -version
/workspace/apache-maven-3.9.4/bin/mvn --version
```

## Comandos de Desarrollo

### Compilacion Rapida (sin tests)
```bash
./mvnw clean compile -DskipTests
```

### Solo Tests
```bash
./mvnw test
```

### Tests de un modulo especifico
```bash
./mvnw -pl veld-runtime test
```

### Verificar estilo de codigo
```bash
./mvnw spotless:check
```

## Scripts de Automatizacion

### build.sh
```bash
#!/bin/bash
cd /workspace/Veld
./mvnw clean compile test
```

### quick-build.sh  
```bash
#!/bin/bash
cd /workspace/Veld
./mvnw clean compile -DskipTests
```

## Notas Importantes

1. **Orden de dependencias**: veld-annotations → veld-runtime → veld-processor
2. **Constructor testing**: LifecycleProcessor es package-private para tests
3. **Integracion automatica**: LifecycleProcessor se integra via bytecode generado
4. **Optimizaciones**: Todas aplicadas en clases originales, no duplicadas

## Estado de Git

```bash
git status  # Debe estar clean
git log --oneline -5  # Ver ultimos commits
```

Todos los cambios han sido committed y pushed exitosamente.