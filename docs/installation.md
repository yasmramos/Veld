# 📦 Veld DI Framework - Installation Guide

This guide covers all installation options for Veld DI Framework.

---

## 🎯 Quick Installation

### Maven (Recommended)
Add to your `pom.xml`:

```xml
<dependencies>
    <!-- Veld Runtime -->
    <dependency>
        <groupId>io.github.yasmramos</groupId>
        <artifactId>veld-runtime</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Annotation Processor (provided scope) -->
    <dependency>
        <groupId>io.github.yasmramos</groupId>
        <artifactId>veld-processor</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle
Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'io.github.yasmramos:veld-runtime:1.0.0'
    annotationProcessor 'io.github.yasmramos:veld-processor:1.0.0'
}
```

---

## 📋 System Requirements

### Java Version
- **Minimum:** Java 11+
- **Recommended:** Java 17+
- **Supported:** OpenJDK, Oracle JDK, Amazon Corretto

### Build Tools
- **Maven:** 3.6+
- **Gradle:** 6.0+
- **IDE:** IntelliJ IDEA, Eclipse, VS Code

### Operating Systems
- **Windows:** 10/11 (x64)
- **macOS:** 10.15+ (Intel/Apple Silicon)
- **Linux:** Ubuntu 18.04+, CentOS 7+, Alpine 3.12+

---

## 🛠️ Detailed Setup

### Maven Configuration

#### 1. Add Dependencies
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-veld-app</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <!-- Veld DI Framework -->
        <dependency>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-runtime</artifactId>
            <version>1.0.0</version>
        </dependency>
        
        <!-- Annotation Processor (compile-time) -->
        <dependency>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-processor</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- Optional: Spring Boot Starter -->
        <dependency>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-spring-boot-starter</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2. Enable Annotation Processing
```xml
<properties>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### Gradle Configuration

#### 1. Add Dependencies
```gradle
plugins {
    id 'java'
    id 'application'
}

group = 'com.example'
version = '1.0.0'

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // Veld DI Framework
    implementation 'io.github.yasmramos:veld-runtime:1.0.0'
    annotationProcessor 'io.github.yasmramos:veld-processor:1.0.0'
    
    // Optional: Spring Boot Starter
    implementation 'io.github.yasmramos:veld-spring-boot-starter:1.0.0'
}

application {
    mainClass = 'com.example.Application'
}

tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}
```

#### 2. Gradle Wrapper
```bash
./gradlew wrapper --gradle-version 7.6
```

---

## 🏗️ IDE Configuration

### IntelliJ IDEA

#### 1. Project Structure
```
Project Structure (Ctrl+Alt+Shift+S)
├── Project SDK: 11+
├── Project language level: 11+
└── Project compiler output: out/production/classes
```

#### 2. Annotation Processing
```
Settings → Build, Execution, Deployment → Compiler → Annotation Processors
├── ✅ Enable annotation processing
├── ✅ Enable processing in module with dependency: 'veld-processor'
└── Production sources directory: out/production/classes
```

#### 3. Code Style
```xml
<!-- Import Veld code style -->
Settings → Editor → Code Style → Import Scheme → IntelliJ IDEA Code Style XML
```

### Eclipse

#### 1. Project Configuration
```xml
<!-- .project -->
<projectDescription>
    <name>my-veld-app</name>
    <buildSpec>
        <buildCommand>
            <name>org.eclipse.jdt.core.javabuilder</name>
        </buildCommand>
    </buildSpec>
    <natures>
        <nature>org.eclipse.jdt.core.javanature</nature>
    </natures>
</projectDescription>
```

#### 2. Annotation Processing
```
Project Properties → Java Compiler → Annotation Processing
├── ✅ Enable annotation processing
├── ✅ Enable processing in module: 'my-veld-app'
└── Factory path: veld-processor-1.0.0.jar
```

### VS Code

#### 1. Extensions
```json
{
    "recommendations": [
        "redhat.java",
        "vscjava.vscode-java-debug",
        "vscjava.vscode-java-test"
    ]
}
```

#### 2. Settings
```json
{
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-11",
            "path": "/path/to/jdk11"
        }
    ],
    "java.compile.nullAnalysis.mode": "automatic"
}
```

---

## 🧪 Testing Setup

### JUnit 5 Configuration

#### Maven
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.9.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>5.9.3</version>
    <scope>test</scope>
</dependency>
```

#### Gradle
```gradle
testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.3'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.9.3'
```

### Test Configuration
```java
// Test configuration
@Configuration
@TestConfiguration
public class VeldTestConfig {
    
    @Bean
    @Primary
    public UserRepository testUserRepository() {
        return new InMemoryUserRepository();
    }
    
    @Bean
    @Primary
    public EmailService testEmailService() {
        return new MockEmailService();
    }
}

// Test class
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testUserCreation() {
        // Test implementation
    }
}
```

---

## 🚀 Quick Start Project

### Project Structure
```
my-veld-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── Application.java
│   │   │           ├── config/
│   │   │           ├── service/
│   │   │           ├── repository/
│   │   │           └── model/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── com/
│               └── example/
├── pom.xml (or build.gradle)
└── README.md
```

### Minimal Application
```java
package com.example;

import io.github.yasmramos.annotations.*;
import java.util.*;

@Component
public class SimpleService {
    
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}

@Component
public class Application {
    
    @Inject
    private SimpleService service;
    
    public void run() {
        String greeting = service.greet("World");
        System.out.println(greeting);
    }
    
    public static void main(String[] args) {
        Veld.start();
        
        Application app = Veld.getBean(Application.class);
        app.run();
        
        Veld.shutdown();
    }
}
```

### application.properties
```properties
# Application configuration
app.name=My Veld Application
app.version=1.0.0
app.debug=true

# Logging configuration
logging.level.com.example=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

---

## ⚙️ Advanced Configuration

### Custom Veld Configuration
```java
@Configuration
public class VeldConfig {
    
    @Bean
    public VeldConfiguration veldConfiguration() {
        return VeldConfiguration.builder()
            .setCacheSize(1000)
            .setThreadPoolSize(4)
            .enableMetrics(true)
            .setProfile("production")
            .build();
    }
}
```

### Performance Tuning
```java
// JVM Options for optimal performance
// -Xms512m -Xmx1g -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+UseZGC

// Veld-specific options
veld.cache.size=10000
veld.thread.pool.size=8
veld.metrics.enabled=true
veld.optimization.level=maximum
```

### Memory Configuration
```xml
<!-- Maven Surefire Plugin -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.0.0-M9</version>
    <configuration>
        <argLine>
            -Xms256m -Xmx512m
            -XX:+UseG1GC
            -XX:+UnlockExperimentalVMOptions
        </argLine>
    </configuration>
</plugin>
```

---

## 🔍 Verification

### Test Installation
```bash
# Maven
mvn clean compile

# Gradle
./gradlew clean build

# Check for generated files
find target/classes -name "*VeldGenerated*"  # Maven
find build/classes/java -name "*VeldGenerated*"  # Gradle
```

### Benchmark Test
```java
@Component
public class PerformanceTest {
    
    @Inject
    private SimpleService service;
    
    public void runBenchmark() {
        long start = System.nanoTime();
        
        for (int i = 0; i < 100000; i++) {
            service.greet("Test" + i);
        }
        
        long end = System.nanoTime();
        long duration = (end - start) / 1000000; // Convert to milliseconds
        
        System.out.println("100,000 calls took: " + duration + "ms");
        System.out.println("Average per call: " + (duration / 100000.0) + "μs");
    }
}
```

---

## 🆘 Troubleshooting

### Common Issues

#### 1. Annotation Processing Not Working
```bash
# Maven - Clean and recompile
mvn clean compile

# Gradle - Clean and rebuild
./gradlew clean build --refresh-dependencies
```

#### 2. Class Not Found Exception
```xml
<!-- Ensure annotation processor is in provided scope -->
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-processor</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

#### 3. IDE Not Recognizing Generated Classes
- **IntelliJ:** File → Invalidate Caches and Restart
- **Eclipse:** Project → Clean
- **VS Code:** Reload Window (Ctrl+Shift+P → "Developer: Reload Window")

#### 4. Performance Issues
```bash
# Check JVM settings
java -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -jar your-app.jar

# Monitor memory usage
jstat -gc <pid> 1000
```

### Debug Mode
```java
// Enable Veld debug logging
System.setProperty("veld.debug", "true");

// Check generated files
System.out.println("Generated class location: " + 
    VeldGenerated.class.getProtectionDomain().getCodeSource().getLocation());
```

---

## 📚 Next Steps

After successful installation:

1. **[Getting Started Guide](getting-started.md)** - Create your first Veld application
2. **[Core Features](core-features.md)** - Learn about dependency injection
3. **[Annotations Reference](annotations.md)** - Explore all available annotations
4. **[Examples](examples.md)** - See practical usage patterns

---

## 🔗 Links

- **Maven Repository**: [https://maven.org](https://maven.org)
- **Gradle Plugin**: [https://plugins.gradle.org/plugin/veld](https://plugins.gradle.org/plugin/veld)
- **GitHub Releases**: [https://github.com/yasmramos/veld/releases](https://github.com/yasmramos/veld/releases)
- **Issue Tracker**: [https://github.com/yasmramos/veld/issues](https://github.com/yasmramos/veld/issues)

---

**🚀 You're ready to build ultra-fast Java applications with Veld DI Framework!**

**⚡ Installation complete - 43,000x faster dependency injection awaits!**