# Veld Maven Plugin

The `veld-maven-plugin` module provides Maven integration for the Veld Framework.

## Features

- Compile-time code generation
- Bytecode weaving
- Dependency validation
- Build optimization

## Installation

Add the plugin to your `pom.xml`:

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

## Goals

### compile

The `compile` goal runs the Veld annotation processor and weaver during the build:

```bash
mvn veld:compile
```

### validate

The `validate` goal checks your configuration and dependencies:

```bash
mvn veld:validate
```

## Configuration

### Basic Configuration

```xml
<plugin>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-maven-plugin</artifactId>
    <version>1.0.4</version>
    <configuration>
        <debug>false</debug>
        <skip>false</skip>
    </configuration>
</plugin>
```

### Advanced Configuration

```xml
<configuration>
    <debug>true</debug>
    <includes>
        <include>com/example/**/*.class</include>
    </includes>
    <excludes>
        <exclude>com/example/test/**/*.class</exclude>
    </excludes>
    <outputDirectory>${project.build.directory}/veld</outputDirectory>
</configuration>
```

## Properties

| Property | Description | Default |
|----------|-------------|---------|
| `debug` | Enable debug output | `false` |
| `skip` | Skip plugin execution | `false` |
| `includes` | Patterns to include | All classes |
| `excludes` | Patterns to exclude | None |
| `outputDirectory` | Output directory for generated files | `${project.build.directory}/veld` |

## Troubleshooting

### Plugin Not Running

Ensure the plugin is bound to a lifecycle phase or explicitly called.

### Generation Failures

Check the Maven logs for detailed error messages. Enable debug mode for more information.

### Performance Issues

If builds are slow, consider:
- Excluding unnecessary packages
- Using incremental compilation
- Checking for circular dependencies
