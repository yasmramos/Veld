# Veld Weaver

The `veld-weaver` module provides bytecode weaving capabilities for the Veld Framework.

## Features

- Compile-time bytecode weaving
- Aspect integration
- Method interception
- Low runtime overhead

## How It Works

The weaver modifies bytecode during the build process to inject aspect logic, dependency injection code, and other enhancements directly into your classes.

## Maven Plugin Integration

The weaver is typically used through the Veld Maven Plugin:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-maven-plugin</artifactId>
            <version>1.0.4</version>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Weaving Options

### Include/Exclude Patterns

You can configure which classes to weave:

```xml
<configuration>
    <includes>
        <include>com/example/**/*.class</include>
    </includes>
    <excludes>
        <exclude>com/example/excluded/**/*.class</exclude>
    </excludes>
</configuration>
```

### Debug Mode

Enable debug output to see what's being woven:

```xml
<configuration>
    <debug>true</debug>
</configuration>
```

## Performance

The weaver is designed for minimal performance impact:
- All weaving happens at compile time
- No runtime reflection
- Optimized bytecode generation

## Troubleshooting

### Weaving Failures

If weaving fails, check:
1. Class path configuration
2. Aspect definitions
3. Exclusion patterns

### Performance Issues

If build times are slow:
1. Reduce the scope of included classes
2. Enable incremental compilation
3. Check for circular dependencies
