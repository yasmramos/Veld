# GraalVM Native Image - Veld Framework

Este documento describe la configuracion para compilar el framework Veld como una imagen nativa de GraalVM.

## Resumen de la Configuracion

El proyecto Veld ha sido configurado para soportar compilacion como imagen nativa de GraalVM. La arquitectura zero-reflection del framework, que utiliza `MethodHandles` en lugar de `Class.forName`, es naturalmente compatible con GraalVM y requiere minima configuracion adicional.

### Archivos Creados

La siguiente estructura de archivos fue agregada para soportar la compilacion nativa:

```
veld-example/
└── src/
    └── main/
        ├── java/
        │   └── io/
        │       └── github/
        │           └── yasmramos/
        │               └── veld/
        │                   └── example/
        │                       └── nativeimage/
        │                           └── NativeImageDemo.java
        └── resources/
            ├── META-INF/
            │   └── native-image/
            │       └── io.github.yasmramos/
            │           └── veld-example/
            │               ├── proxy-config.json
            │               ├── reflect-config.json
            │               └── resource-config.json
            └── native-image.properties
```

### Archivos de Configuracion de GraalVM

El directorio `META-INF/native-image/` contiene los archivos de metadatos que GraalVM necesita para entender como tratar las clases y recursos del framework:

- **resource-config.json**: Define los recursos del classpath que deben ser incluidos en la imagen nativa. Incluye archivos `.class`, `.properties` y `.xml`.

- **reflect-config.json**: Configuracion de reflexion. Esta configuracion es minima porque Veld utiliza `MethodHandles` en lugar de reflexion tradicional, lo que reduce significativamente la necesidad de metadatos de reflexion.

- **proxy-config.json**: Define las interfaces que requieren proxies dinamicos. Esta vacio porque Veld genera los proxies en tiempo de compilacion mediante ASM.

## Requisitos Previos

Para compilar una imagen nativa, se requiere lo siguiente:

1. **GraalVM JDK**: Version 17 o superior. Se puede descargar desde https://www.graalvm.org/downloads/

2. **Componente native-image**: Despues de instalar GraalVM, es necesario instalar el componente native-image:
   ```bash
   gu install native-image
   ```

3. **Maven**: Version 3.6 o superior.

## Compilacion de la Imagen Nativa

Para compilar el proyecto como imagen nativa, ejecute el siguiente comando desde la raiz del proyecto:

```bash
# Compilar solo el modulo veld-example con sus dependencias
mvn clean package -Pnative -pl veld-example -am
```

Este comando:
- Activa el perfil `native` en el pom.xml
- Compila todos los modulos requeridos (veld-annotations, veld-runtime, veld-aop, etc.)
- Genera la imagen nativa utilizando el plugin native-maven-plugin
- Produce un ejecutable llamado `veld-native-demo` en el directorio `target/`

### Compilacion con Mas Opciones

Para una compilacion mas verbosa con informacion de depuracion:

```bash
mvn clean package -Pnative -pl veld-example -am -Dnative.debug=true
```

## Ejecucion de la Imagen Nativa

Una vez compilada, la imagen nativa se puede ejecutar directamente:

```bash
./veld-example/target/veld-native-demo
```

Alternativamente, desde el directorio veld-example:

```bash
cd veld-example
mvn exec:exec -Pnative
```

### Salida Esperada

La ejecucion exitosa mostrara una salida similar a:

```
╔══════════════════════════════════════════════════════════╗
║        Veld Framework - Native Image Demo                ║
╚══════════════════════════════════════════════════════════╝

Inicializando Veld Framework...
Veld disponible: SI

Verificando inyeccion de dependencias:
  GreetingService obtuvo MessageRepository: SI

Ejecutando operaciones:
  Saludo: Hola, Mundo!
  Autor: Veld Framework

Verificando comportamiento singleton:
  Misma instancia de GreetingService: SI

╔══════════════════════════════════════════════════════════╗
║              Demo completada exitosamente!               ║
╚══════════════════════════════════════════════════════════╝

Tiempo de inicio: 0.045 ms
Este ejecutable es una imagen nativa de GraalVM.
No se requiere JVM para ejecutar esta aplicacion.
```

## Verificacion de la Arquitectura Zero-Reflection

Para verificar que la imagen nativa funciona correctamente sin dependencia excesiva de reflexion, puede revisar el archivo `reflect-config.json` en el directorio de recursos. Un archivo de configuracion para un framework verdaderamente zero-reflection tendria muy pocas o ninguna entrada de reflexion, ya que las clases se cargan y utilizan mediante `MethodHandles` en lugar de `Class.forName()` o `Class.getMethod()`.

La refactorizacion previa que reemplazo todas las instancias de `Class.forName()` con `MethodHandles.lookup().loadClass()` asegura que GraalVM pueda realizar su analisis estatico de manera efectiva y generar una imagen nativa pequena y de rapido inicio.

## Notas sobre Rendimiento

Las imagenes nativas de GraalVM ofrecen varias ventajas de rendimiento:

- **Tiempo de inicio**: Las imagenes nativas inician en milisegundos, comparado con segundos para aplicaciones JVM
- **Consumo de memoria**: Menor overhead de memoria debido a la ausencia del JVM
- **Tamanio**: El ejecutable puede ser pequeno, aunque incluye todo el codigo necesario
- **Sin warmup**: No hay fase de calentamiento JIT; el codigo esta optimizado desde el inicio

## Solucion de Problemas

### Error: native-image no encontrado

Si aparece el error `Error: native-image not found`, verifique que el componente native-image este instalado:

```bash
gu list
```

Si no aparece native-image, instalelo:

```bash
gu install native-image
```

### Error: ClassNotFoundException en tiempo de ejecucion

Si ocurre un `ClassNotFoundException` al ejecutar la imagen nativa, puede ser necesario agregar la clase al archivo `reflect-config.json`. Sin embargo, dado que el framework utiliza `MethodHandles`, este error deberia ser raro.

### Error: OutOfMemoryError durante la compilacion

La compilacion de imagenes nativas requiere memoria significativa. Aumente la memoria disponible para el proceso de compilacion:

```bash
export GRAALVM_HOME=/path/to/graalvm
export JAVA_HOME=$GRAALVM_HOME
native-image --no-fallback -J-Xmx8g ...
```

## Recursos Adicionales

- Documentacion oficial de GraalVM Native Image: https://www.graalvm.org/latest/reference-manual/native-image/
- Documentacion de Veld Framework: https://github.com/yasmramos/Veld
- Repositorio del proyecto: https://github.com/yasmramos/Veld
