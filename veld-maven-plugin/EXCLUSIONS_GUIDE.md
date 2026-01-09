# Veld Maven Plugin - Funcionalidad de Exclusiones

## Resumen

El `veld-maven-plugin` ahora soporta la exclusión de clases o paquetes específicos del procesamiento de Veld durante la compilación y el weaving de bytecode.

## Configuración

Agrega el parámetro `excludes` en la configuración del plugin en tu `pom.xml`:

```xml
<plugin>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-maven-plugin</artifactId>
    <version>1.0.2</version>
    <configuration>
        <excludes>
            <exclude>com/example/legacy/**</exclude>
            <exclude>**/generated/**</exclude>
            <exclude>com/third/party/**/*.class</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Patrones Soportados

La funcionalidad utiliza patrones estilo Ant con las siguientes características:

### Ejemplos de Patrones

| Patrón | Descripción | Ejemplos de archivos excluidos |
|--------|-------------|----------------------------------|
| `**/*Test*` | Todas las clases de test | `com/example/MyTest.class`, `util/TestHelper.class` |
| `com/legacy/**` | Todo el paquete legacy | `com/legacy/OldService.class`, `com/legacy/dao/LegacyDao.class` |
| `**/generated/**` | Código generado automáticamente | `target/generated/MyGenerated.class` |
| `com/example/Test*.class` | Clases que empiecen con "Test" en paquete específico | `com/example/TestUtils.class` |
| `**/*IT.class` | Tests de integración | `com/example/UserServiceIT.class` |

### Metacaracteres

- `*` - Coincide con cero o más caracteres (excluyendo separadores de directorio)
- `**` - Coincide con cero o más directorios
- `?` - Coincide con exactamente un caracter

## Casos de Uso Comunes

### 1. Excluir Tests

```xml
<excludes>
    <exclude>**/*Test*.class</exclude>
    <exclude>**/*IT.class</exclude>
    <exclude>**/test/**</exclude>
</excludes>
```

### 2. Excluir Código Legacy

```xml
<excludes>
    <exclude>com/company/legacy/**</exclude>
    <exclude>org/old/framework/**</exclude>
</excludes>
```

### 3. Excluir Código Generado

```xml
<excludes>
    <exclude>**/generated/**</exclude>
    <exclude>**/*Generated.class</exclude>
    <exclude>**/target/generated-sources/**</exclude>
</excludes>
```

### 4. Excluir Librerías de Terceros

```xml
<excludes>
    <exclude>com/fasterxml/**</exclude>
    <exclude>org/springframework/**</exclude>
    <exclude>io/netty/**</exclude>
</excludes>
```

## Comportamiento

### Durante la Compilación
- Los archivos `.java` que coincidan con los patrones de exclusión **NO** serán procesados por el procesador de anotaciones de Veld
- Estos archivos seguirán siendo compilados por el compilador Java estándar
- Solo se excluye el procesamiento específico de Veld

### Durante el Weaving
- Los archivos `.class` que coincidan con los patrones **NO** serán modificados por el weaver de bytecode de Veld
- Las clases excluidas no tendrán los métodos setter automáticos inyectados

### Logging
- Con la opción `verbose=true`, el plugin mostrará qué archivos fueron excluidos:

```bash
[INFO]   Excluded from compilation: com/example/legacy/OldService.java
[INFO]   Excluded from weaving: com/example/legacy/OldService
[INFO]   3 class(es) enhanced
[INFO]   2 class(es) excluded from weaving
```

## Configuración Avanzada

### Combinando con Otras Opciones

```xml
<plugin>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-maven-plugin</artifactId>
    <version>1.0.2</version>
    <configuration>
        <!-- Habilitar logging detallado -->
        <verbose>true</verbose>
        
        <!-- Exclusiones -->
        <excludes>
            <exclude>**/*Test*</exclude>
            <exclude>com/legacy/**</exclude>
            <exclude>**/generated/**</exclude>
        </excludes>
        
        <!-- Argumentos adicionales del compilador -->
        <compilerArgs>
            <arg>-Xlint:all</arg>
        </compilerArgs>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Saltarse Todo el Procesamiento

```xml
<configuration>
    <!-- Saltarse completamente el plugin -->
    <skip>true</skip>
</configuration>
```

O usar propiedades del sistema:

```bash
mvn compile -Dveld.skip=true
```

## Consideraciones Importantes

1. **Orden de Patrones**: Los patrones se evalúan en orden. El primer patrón que coincida determinará si el archivo es excluido.

2. **Performance**: Usar exclusiones puede mejorar el tiempo de build al evitar procesar clases innecesarias.

3. **Compatibilidad**: Las exclusiones afectan tanto la compilación como el weaving. Si una clase es excluida de la compilación, automáticamente también se excluye del weaving.

4. **Debugging**: Usa `verbose=true` para ver exactamente qué archivos están siendo excluidos y verificar que tus patrones funcionan como esperas.

## Ejemplos Completos

### Proyecto con Módulos Legacy

```xml
<plugin>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-maven-plugin</artifactId>
    <version>1.0.2</version>
    <configuration>
        <excludes>
            <!-- Excluir módulo legacy completo -->
            <exclude>com/company/legacy/**</exclude>
            
            <!-- Excluir tests -->
            <exclude>**/*Test*.class</exclude>
            <exclude>**/*IT.class</exclude>
            
            <!-- Excluir configuraciones Spring -->
            <exclude>**/config/**/*Config.class</exclude>
            
            <!-- Excluir DTOs (no necesitan inyección) -->
            <exclude>**/dto/**</exclude>
            <exclude>**/model/**/*DTO.class</exclude>
        </excludes>
        <verbose>true</verbose>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Migración Gradual

Para proyectos que están migrando gradualmente a Veld:

```xml
<excludes>
    <!-- Mantener código Spring existente sin tocar -->
    <exclude>**/spring/**</exclude>
    <exclude>**/*Controller.class</exclude>
    <exclude>**/*Service.class</exclude>
    
    <!-- Solo procesar nuevas clases Veld -->
    <!-- (no se excluye nada del paquete com/company/veld) -->
</excludes>
```

Este enfoque permite adoptar Veld gradualmente sin afectar el código existente.