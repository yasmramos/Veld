# Veld Benchmark Suite

Performance benchmarks comparing **Veld** with other popular DI frameworks.

## Frameworks Compared

| Framework | Version | Type |
|-----------|---------|------|
| **Veld** | 1.0.0-alpha.6 | Compile-time (ASM bytecode) |
| Spring | 6.1.14 | Runtime (Reflection) |
| Guice | 7.0.0 | Runtime (Reflection) |
| Dagger | 2.52 | Compile-time (Code generation) |

## Benchmarks

### 1. StartupBenchmark
Measures container initialization time. Critical for:
- Application startup performance
- Serverless cold starts
- Microservices

### 2. InjectionBenchmark
Measures dependency lookup time for:
- Simple injection (1 dependency)
- Complex injection (3 dependencies)
- Deep injection (nested dependencies)
- Raw lookup (singleton retrieval)

### 3. PrototypeBenchmark
Measures new instance creation time. Important for:
- Request-scoped dependencies
- Factory patterns
- High-churn scenarios

### 4. ThroughputBenchmark
Measures operations per second:
- Single-threaded throughput
- Concurrent throughput (4 threads)

### 5. MemoryBenchmark
Measures memory footprint:
- Container memory overhead
- Scalability with multiple containers

## Quick Start

### Build

```bash
# From project root
mvn clean package -pl veld-benchmark -am -DskipTests
```

### Run All Benchmarks

```bash
java -jar target/veld-benchmark.jar
```

### Run Specific Benchmark

```bash
# Startup only
java -jar target/veld-benchmark.jar Startup

# Injection only
java -jar target/veld-benchmark.jar Injection

# Throughput only
java -jar target/veld-benchmark.jar Throughput
```

### Quick Mode (for development)

```bash
java -jar target/veld-benchmark.jar -f 1 -wi 1 -i 2
```

### Full Mode (for publication)

```bash
java -jar target/veld-benchmark.jar -f 5 -wi 5 -i 10 -rf json -rff results.json
```

## Maven Profiles

```bash
# Quick benchmarks (fast, less accurate)
mvn exec:exec -pl veld-benchmark -Pquick

# Full benchmarks (slow, accurate)
mvn exec:exec -pl veld-benchmark -Pfull
```

## JMH Options

| Option | Description | Default |
|--------|-------------|---------|
| `-f <n>` | Forks | 2 |
| `-wi <n>` | Warmup iterations | 3 |
| `-i <n>` | Measurement iterations | 5 |
| `-t <n>` | Threads | 1 |
| `-rf json` | Result format | text |
| `-rff <file>` | Result file | stdout |
| `-prof gc` | GC profiler | - |
| `-prof stack` | Stack profiler | - |

## Expected Results

Based on design characteristics:

| Metric | Veld | Dagger | Guice | Spring |
|--------|------|--------|-------|--------|
| Startup | Fast | Fast | Medium | Slow |
| Lookup | Fast | Fastest | Medium | Medium |
| Memory | Low | Low | Medium | High |
| Features | Medium | Low | High | Highest |

### Why Veld is Fast

1. **No Reflection** - Pure bytecode generation
2. **Compile-time Resolution** - Dependencies resolved at build time
3. **Direct Method Calls** - No dynamic dispatch overhead
4. **Minimal Runtime** - Lightweight container

### Trade-offs

- **Veld/Dagger**: Faster but less runtime flexibility
- **Spring/Guice**: Slower but more features (AOP, proxies, etc.)

## Interpreting Results

### Sample Output

```
Benchmark                          Mode  Cnt    Score    Error  Units
StartupBenchmark.veldStartup       avgt    5   12.345 ±  0.123  us/op
StartupBenchmark.springStartup     avgt    5  456.789 ± 12.345  us/op
StartupBenchmark.guiceStartup      avgt    5  123.456 ±  1.234  us/op
StartupBenchmark.daggerStartup     avgt    5   10.123 ±  0.098  us/op
```

- **Score**: Average time per operation (lower is better)
- **Error**: 99.9% confidence interval
- **Units**: Microseconds (us) or nanoseconds (ns)

## Contributing

To add new benchmarks:

1. Create a new `*Benchmark.java` class
2. Annotate with JMH annotations
3. Follow naming convention: `{framework}{Scenario}`
4. Document in this README

## License

Apache License 2.0
