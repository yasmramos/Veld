# GraalVM Native Image Support

This document describes how to build and run the Veld Framework as a GraalVM Native Image.

## Benefits of Native Image

Building the Veld Framework as a GraalVM Native Image provides several significant advantages:

| Metric | JVM | Native Image | Improvement |
|--------|-----|--------------|-------------|
| Startup Time | 50-200ms | 5-20ms | 10-40x faster |
| Memory Usage | 50-100MB | 10-30MB | 3-5x less |
| Peak Performance | Warm-up needed | Instant | Immediate |
| Executable Size | N/A (JAR) | 10-30MB | Portable binary |

## Prerequisites

### 1. Install GraalVM JDK

Download and install GraalVM JDK 17 or later from the official sources:

- **Official Release**: https://github.com/graalvm/graalvm-ce-builds/releases
- **SDKMAN (Recommended)**: `sdk install java 17.0.9-graal`

```bash
# Using SDKMAN (Linux/macOS)
sdk install java 17.0.9-graal

# Verify installation
java -version
# Should show: openjdk version "17.0.x" GraalVM
```

### 2. Install Native Image Component

```bash
# Using gu (GraalVM Updater)
gu install native-image

# Verify installation
native-image --version
```

## Building the Native Image

### Quick Build

```bash
# Build the project first
mvn clean package -DskipTests

# Build native image
native-image -jar veld-runtime/target/veld-runtime-1.0.3.jar \
    -H:Name=veld-runtime \
    --no-fallback \
    -H:ConfigurationFileDirectories=src/main/resources/META-INF/native-image
```

### Using the Build Script

```bash
# Make the script executable (Linux/macOS)
chmod +x veld-runtime/build-native.sh

# Build
./veld-runtime/build-native.sh

# Build without JNI support
./veld-runtime/build-native.sh --without-jni

# Build with all security services enabled
./veld-runtime/build-native.sh --enable-all-security-services
```

### Maven Plugin (Alternative)

```bash
# Build with Maven plugin (requires GraalVM)
mvn package -Pnative -DskipTests
```

Add this profile to your `pom.xml`:
```xml
<profile>
    <id>native</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.nativeimage</groupId>
                <artifactId>native-image-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</profile>
```

## Configuration Files

The framework includes pre-configured GraalVM metadata files:

| File | Purpose |
|------|---------|
| `META-INF/native-image/reflect-config.json` | Reflection configuration for runtime introspection |
| `META-INF/native-image/resource-config.json` | Resource inclusion patterns |
| `META-INF/native-image/proxy-config.json` | Proxy interface configuration |
| `META-INF/native-image/jni-config.json` | JNI bindings configuration |
| `META-INF/native-image/native-image.properties` | Build properties and hints |

## Running the Native Image

```bash
# Run the native executable
./veld-runtime

# With arguments
./veld-runtime arg1 arg2

# Set environment variables
VELD_DEBUG=true ./veld-runtime
```

## Troubleshooting

### Common Issues

#### 1. Reflection not working

**Problem**: Classes not being reflected at runtime.

**Solution**: Add entries to `reflect-config.json`:
```json
{
  "name": "com.example.MyClass",
  "allDeclaredConstructors": true,
  "allPublicConstructors": true,
  "allDeclaredMethods": true,
  "allPublicMethods": true
}
```

#### 2. Class initialization errors

**Problem**: Static initializers failing at runtime.

**Solution**: Use `--initialize-at-run-time`:
```bash
native-image -jar veld-runtime.jar \
    --initialize-at-run-time=com.example.ClassWithStaticInitializer
```

#### 3. Missing resources

**Problem**: Resource files not included.

**Solution**: Configure in `resource-config.json`:
```json
{
  "resources": {
    "includes": [
      {"pattern": "**/*.properties"},
      {"pattern": "**/*.xml"}
    ]
  }
}
```

#### 4. Proxy creation failing

**Problem**: Dynamic proxy classes not being generated.

**Solution**: Add interfaces to `proxy-config.json`:
```json
{
  "interfaces": [
    {"interface": "com.example.MyInterface"}
  ]
}
```

### Debugging with Agent

Use the native-image agent to automatically generate configuration:

```bash
java -agentlib:native-image-agent=config-output-dir=target/native-image-config \
    -jar veld-runtime.jar
```

This will generate configuration files based on actual runtime usage.

## Performance Tuning

### Build Options

```bash
# Release build with optimizations
native-image -O3 -jar veld-runtime.jar

# Smaller binary (trades off some features)
native-image -Ob -jar veld-runtime.jar

# Full optimizations (slower build, faster runtime)
native-image -O3 --march=native -jar veld-runtime.jar
```

### Runtime Options

```bash
# Enable verbose output
./veld-runtime -H:+PrintClassInitialization

# Enable heap statistics
./veld-runtime -H:HeapDump=/tmp/heap.hprof

# Set maximum heap size
./veld-runtime -J-Xmx64m
```

## Best Practices

1. **Test thoroughly**: Native Image may behave differently than JVM
2. **Use reflection sparingly**: Minimize dynamic reflection
3. **Prefer composition**: Use interfaces over heavy inheritance
4. **Profile before optimizing**: Measure before and after changes
5. **Keep metadata updated**: Add new reflection entries when adding features

## Known Limitations

- Some reflection-heavy libraries may require additional configuration
- Dynamic class loading (Class.forName) may not work as expected
- Security managers are not fully supported
- Some Java agents may not be compatible

## Further Reading

- [GraalVM Native Image Documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Native Image Compatibility Guide](https://www.graalvm.org/latest/reference-manual/compatibility/)
- [Reflection in Native Image](https://www.graalvm.org/latest/reference-manual/reflection/)
