/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.veld.benchmark;

import com.veld.annotation.Component;
import com.veld.annotation.Inject;
import com.veld.annotation.Singleton;
import com.veld.processor.VeldProcessor;
import com.veld.processor.OptimizedVeldProcessor;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 🚀 JMH BENCHMARK - Veld Phase 1 Optimizations
 * 
 * Este benchmark JMH compara el rendimiento del procesador original vs optimizado
 * para demostrar las mejoras de la Fase 1:
 * - Cache de Annotation Processing
 * - Weaving Paralelo  
 * - Generación Incremental
 * 
 * RESULTADO ESPERADO: 50x más rápido que el procesador original
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m", "-XX:+UseG1GC"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class Phase1OptimizationJMHBenchmark {
    
    private static final String OUTPUT_DIR = "target/test-classes";
    private static final String SOURCE_DIR = "target/test-sources";
    
    private JavaCompiler compiler;
    private StandardJavaFileManager fileManager;
    
    // Processors under test
    private VeldProcessor originalProcessor;
    private OptimizedVeldProcessor optimizedProcessor;
    
    @Setup(Level.Trial)
    public void setup() {
        System.out.println("🚀 Configurando benchmark JMH - Veld Phase 1 Optimizations");
        
        // Initialize JavaCompiler
        compiler = ToolProvider.getSystemJavaCompiler();
        fileManager = compiler.getStandardFileManager(null, null, null);
        
        // Initialize processors
        originalProcessor = new VeldProcessor();
        optimizedProcessor = new OptimizedVeldProcessor();
        
        // Create directories
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            Files.createDirectories(Paths.get(SOURCE_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directories", e);
        }
        
        System.out.println("✅ Setup completado");
    }
    
    @TearDown(Level.Trial)
    public void teardown() {
        try {
            if (fileManager != null) {
                fileManager.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing file manager: " + e.getMessage());
        }
    }
    
    // ==================== SMALL PROJECT (25 COMPONENTS) ====================
    
    @Benchmark
    public void originalProcessorSmallProject(Blackhole bh) throws Exception {
        generateTestComponents(25);
        long duration = benchmarkProcessor(originalProcessor);
        bh.consume(duration);
    }
    
    @Benchmark
    public void optimizedProcessorSmallProject(Blackhole bh) throws Exception {
        generateTestComponents(25);
        long duration = benchmarkProcessor(optimizedProcessor);
        bh.consume(duration);
    }
    
    // ==================== MEDIUM PROJECT (150 COMPONENTS) ====================
    
    @Benchmark
    public void originalProcessorMediumProject(Blackhole bh) throws Exception {
        generateTestComponents(150);
        long duration = benchmarkProcessor(originalProcessor);
        bh.consume(duration);
    }
    
    @Benchmark
    public void optimizedProcessorMediumProject(Blackhole bh) throws Exception {
        generateTestComponents(150);
        long duration = benchmarkProcessor(optimizedProcessor);
        bh.consume(duration);
    }
    
    // ==================== LARGE PROJECT (500 COMPONENTS) ====================
    
    @Benchmark
    public void originalProcessorLargeProject(Blackhole bh) throws Exception {
        generateTestComponents(500);
        long duration = benchmarkProcessor(originalProcessor);
        bh.consume(duration);
    }
    
    @Benchmark
    public void optimizedProcessorLargeProject(Blackhole bh) throws Exception {
        generateTestComponents(500);
        long duration = benchmarkProcessor(optimizedProcessor);
        bh.consume(duration);
    }
    
    // ==================== INCREMENTAL BUILD (200 COMPONENTS) ====================
    
    @Benchmark
    public void originalProcessorIncremental(Blackhole bh) throws Exception {
        generateTestComponents(200);
        // Simulate incremental changes
        modifySomeComponents(10);
        long duration = benchmarkProcessor(originalProcessor);
        bh.consume(duration);
    }
    
    @Benchmark
    public void optimizedProcessorIncremental(Blackhole bh) throws Exception {
        generateTestComponents(200);
        // Simulate incremental changes
        modifySomeComponents(10);
        long duration = benchmarkProcessor(optimizedProcessor);
        bh.consume(duration);
    }
    
    /**
     * Genera componentes de prueba con dependencias realistas
     */
    private void generateTestComponents(int count) throws Exception {
        // Limpiar directorio anterior
        Path sourcePath = Paths.get(SOURCE_DIR);
        if (Files.exists(sourcePath)) {
            Files.walk(sourcePath)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        // Ignorar errores de limpieza
                    }
                });
        }
        
        // Generar interfaces base
        generateBaseInterfaces();
        
        // Generar componentes con dependencias realistas
        Random random = new Random(42); // Seed fijo para reproducibilidad
        
        for (int i = 0; i < count; i++) {
            String packageName = "com.example.test.component" + (i / 50);
            String className = "TestComponent" + i;
            
            List<String> dependencies = generateDependencies(random, i, count);
            
            String componentCode = generateComponentCode(packageName, className, dependencies);
            
            Path filePath = Paths.get(SOURCE_DIR, packageName.replace('.', '/'), className + ".java");
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, componentCode.getBytes());
        }
    }
    
    /**
     * Genera interfaces base para dependencias
     */
    private void generateBaseInterfaces() throws Exception {
        String[] interfaces = {
            "Repository", "Service", "Controller", "DAO", "Manager",
            "Provider", "Factory", "Handler", "Processor", "Adapter"
        };
        
        for (String iface : interfaces) {
            String code = String.format(
                "package com.example.test.base;\n" +
                "public interface %s<T> {\n" +
                "    T findById(String id);\n" +
                "    void save(T entity);\n" +
                "}\n", iface);
            
            Path filePath = Paths.get(SOURCE_DIR, "com/example/test/base", iface + ".java");
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, code.getBytes());
        }
    }
    
    /**
     * Genera dependencias realistas para un componente
     */
    private List<String> generateDependencies(Random random, int componentIndex, int totalComponents) {
        List<String> dependencies = new ArrayList<>();
        
        // Cada componente depende de 2-5 otros componentes
        int dependencyCount = 2 + random.nextInt(4);
        
        for (int i = 0; i < dependencyCount; i++) {
            int depIndex = random.nextInt(totalComponents);
            if (depIndex != componentIndex) { // Evitar auto-dependencia
                String depClass = "TestComponent" + depIndex;
                dependencies.add(depClass);
            }
        }
        
        return dependencies;
    }
    
    /**
     * Genera código Java para un componente
     */
    private String generateComponentCode(String packageName, String className, List<String> dependencies) {
        StringBuilder constructorParams = new StringBuilder();
        StringBuilder constructorBody = new StringBuilder();
        StringBuilder fieldDeclarations = new StringBuilder();
        
        for (int i = 0; i < dependencies.size(); i++) {
            String depClass = dependencies.get(i);
            String paramName = "dep" + i;
            String fieldName = "_" + paramName;
            
            fieldDeclarations.append(String.format(
                "    @Inject\n" +
                "    private %s %s;\n\n", depClass, fieldName));
            
            constructorParams.append(i > 0 ? ", " : "").append(depClass).append(" ").append(paramName);
            constructorBody.append(String.format(
                "        this.%s = %s;\n", fieldName, paramName));
        }
        
        return String.format(
            "package %s;\n" +
            "\n" +
            "import com.veld.annotation.*;\n" +
            "import com.example.test.base.*;\n" +
            "\n" +
            "@Component\n" +
            "@Singleton\n" +
            "public class %s {\n" +
            "%s" +
            "    @Inject\n" +
            "    public %s(%s) {\n" +
            "%s" +
            "    }\n" +
            "\n" +
            "    public void doSomething() {\n" +
            "        // Lógica de negocio\n" +
            "    }\n" +
            "}\n",
            packageName, className, fieldDeclarations.toString(), 
            className, constructorParams.toString(), constructorBody.toString()
        );
    }
    
    /**
     * Modifica algunos componentes para simular un build incremental
     */
    private void modifySomeComponents(int modifiedCount) throws Exception {
        Random random = new Random(123); // Seed diferente para cambios
        
        for (int i = 0; i < modifiedCount; i++) {
            int componentIndex = random.nextInt(200);
            String className = "TestComponent" + componentIndex;
            
            // Leer archivo existente
            Path filePath = findComponentFile(className);
            if (filePath != null) {
                String content = new String(Files.readAllBytes(filePath));
                
                // Agregar comentario para simular cambio
                content = content.replace(
                    "public class " + className + " {",
                    "public class " + className + " { // Modified in incremental build"
                );
                
                Files.write(filePath, content.getBytes());
            }
        }
    }
    
    /**
     * Encuentra el archivo de un componente
     */
    private Path findComponentFile(String className) throws IOException {
        return Files.walk(Paths.get(SOURCE_DIR))
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(className + ".java"))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Ejecuta benchmark para un procesador específico
     */
    private long benchmarkProcessor(AbstractProcessor processor) throws Exception {
        // Limpiar directorio de salida
        cleanOutputDirectory();
        
        long startTime = System.nanoTime();
        
        // Recopilar archivos fuente
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(
            Files.walk(Paths.get(SOURCE_DIR))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .collect(java.util.stream.Collectors.toList())
        );
        
        // Configurar opciones de compilación
        List<String> options = Arrays.asList(
            "-d", OUTPUT_DIR,
            "-cp", System.getProperty("java.class.path"),
            "-processor", processor.getClass().getName(),
            "-s", SOURCE_DIR
        );
        
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, diagnostics, options, null, compilationUnits
        );
        
        boolean success = task.call();
        
        long endTime = System.nanoTime();
        
        if (!success) {
            System.err.println("Compilación falló para " + processor.getClass().getSimpleName());
            diagnostics.getDiagnostics().forEach(d -> 
                System.err.println(d.getMessage(null)));
        }
        
        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }
    
    /**
     * Limpia el directorio de salida
     */
    private void cleanOutputDirectory() throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR);
        if (Files.exists(outputPath)) {
            Files.walk(outputPath)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        // Ignorar errores de limpieza
                    }
                });
        }
    }
}