# 🚀 Sistema de Benchmarks Automatizado - Veld DI Framework

Este sistema ejecuta automáticamente todos los benchmarks del proyecto Veld DI Framework y genera reportes detallados tanto localmente como en CI/CD.

---

## 🎯 Características del Sistema

### ✅ **Automatización Completa**
- Ejecución automática de todos los tipos de benchmarks
- Generación de reportes en markdown
- Análisis de tendencias de performance
- Detección automática de regressions
- Notificaciones en pull requests

### 📊 **Tipos de Benchmarks**
1. **JMH Benchmarks**: Performance extrema con Java Microbenchmark Harness
2. **Simple Benchmarks**: Pruebas básicas de funcionalidad
3. **Strategic Benchmarks**: Análisis de casos de uso específicos
4. **JMH Standalone**: Benchmarks independientes
5. **Unit Tests**: Verificación de funcionalidad

### 🤖 **CI/CD Integration**
- GitHub Actions workflow completo
- Ejecución en múltiples versiones de Java (11, 17)
- Artifacts guardados por 30 días
- Reportes consolidados automáticos

---

## 🚀 Uso del Sistema

### **Ejecución Local**

```bash
cd /workspace/Veld

# Ejecutar todos los benchmarks
./run_all_benchmarks.sh

# O ejecutar manualmente cada componente
./mvnw -pl veld-benchmark -am clean compile
java -cp veld-benchmark/target/classes:... com.veld.benchmark.SimpleBenchmark
```

### **CI/CD Automático**

El sistema se ejecuta automáticamente en:

- **Push a main branch**
- **Pull Requests**
- **Ejecución diaria** (02:00 UTC)

### **Configuración Manual**

```bash
# Clonar y configurar
git clone https://github.com/yasmramos/Veld.git
cd Veld

# Instalar dependencias
./mvnw clean install

# Ejecutar benchmarks
./run_all_benchmarks.sh
```

---

## 📁 Estructura de Archivos

```
Veld/
├── .github/workflows/
│   └── benchmarks.yml              # Workflow de CI/CD
├── benchmark-reports/              # Reportes generados
│   ├── benchmark-report.md         # Reporte principal
│   ├── consolidated-report.md      # Reporte consolidado
│   ├── performance-analysis.md     # Análisis de performance
│   ├── execution-summary.md        # Resumen de ejecución
│   ├── history/                    # Histórico de ejecuciones
│   └── analysis/                   # Análisis detallados
├── veld-benchmark/                 # Módulo de benchmarks
│   ├── src/main/java/
│   │   └── com/veld/benchmark/
│   │       ├── Phase1OptimizationBenchmark.java
│   │       ├── SimpleBenchmark.java
│   │       └── StrategicBenchmark.java
│   └── run-strategic-benchmarks.sh # Script estratégico
├── Scripts de Generación/
│   ├── generate_benchmark_report.py
│   ├── consolidate_benchmark_reports.py
│   └── performance_analysis.py
└── run_all_benchmarks.sh           # Script principal local
```

---

## 📊 Tipos de Reportes

### 1. **Reporte Principal** (`benchmark-report.md`)
- Resumen ejecutivo de benchmarks
- Métricas de rendimiento clave
- Comparación con frameworks tradicionales
- Análisis técnico de optimizaciones
- Recomendaciones de producción

### 2. **Reporte Consolidado** (`consolidated-report.md`)
- Comparación entre Java 11 y Java 17
- Resultados de múltiples ejecuciones
- Análisis comparativo de versiones
- Artifacts y archivos generados

### 3. **Análisis de Performance** (`performance-analysis.md`)
- Tendencias de rendimiento
- Detección de regressions
- Alertas automáticas
- Recomendaciones de optimización

### 4. **Resumen de Ejecución** (`execution-summary.md`)
- Estado de cada componente
- Archivos generados
- Próximos pasos recomendados

---

## 🔧 Configuración de CI/CD

### **GitHub Actions Workflow**

```yaml
name: Veld DI Framework - Benchmarks & Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 2 * * *'  # Diario a las 02:00 UTC

jobs:
  benchmarks:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java-version: [11, 17]
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: Set up JDK
      uses: actions/setup-java@v4
      with:
        java-version: ${{ matrix.java-version }}
    
    - name: Run all benchmarks
      run: ./run_all_benchmarks.sh
    
    - name: Upload results
      uses: actions/upload-artifact@v3
      with:
        name: benchmark-results
        path: benchmark-reports/
```

### **Variables de Entorno**

```bash
# Configurar en GitHub Secrets
GITHUB_TOKEN=ghp_...
MAVEN_SETTINGS=...
```

---

## 📈 Métricas Monitoreadas

### **Performance Core**
- **Throughput**: Operaciones por segundo
- **Latency**: Tiempo de respuesta
- **Memory Usage**: Uso de memoria heap
- **GC Pressure**: Presión del garbage collector

### **Framework-Specific**
- **Injection Speed**: Velocidad de inyección de dependencias
- **Startup Time**: Tiempo de inicialización
- **Reflection Elimination**: Porcentaje de reflection eliminado
- **Bytecode Generation**: Eficiencia de generación

### **Comparative Metrics**
- **vs Spring DI**: Comparación de velocidad
- **vs Guice**: Benchmarks relativos
- **vs Dagger**: Performance comparativa

---

## 🚨 Alertas y Notificaciones

### **Tipos de Alertas**
1. **Regressions**: Performance degradó > 10%
2. **Test Failures**: Tests fallaron > 5%
3. **Memory Leaks**: Aumento significativo de memoria
4. **Compilation Errors**: Errores de compilación

### **Canales de Notificación**
- **GitHub Comments**: En pull requests
- **GitHub Issues**: Para regressions críticas
- **Email**: Para alertas de alta severidad
- **Slack/Teams**: Configuración personalizada

---

## 🔧 Personalización

### **Configurar Benchmarks**

Editar `Phase1OptimizationBenchmark.java`:
```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Phase1OptimizationBenchmark {
    // Configurar benchmarks específicos
}
```

### **Agregar Nuevos Benchmarks**

1. Crear clase en `veld-benchmark/src/main/java/`
2. Anotar con `@Benchmark`
3. Implementar método benchmark
4. Agregar al workflow de CI/CD

### **Personalizar Reportes**

Modificar scripts de generación:
- `generate_benchmark_report.py`: Reporte principal
- `performance_analysis.py`: Análisis de tendencias
- `consolidate_benchmark_reports.py`: Consolidación

---

## 📚 Documentación Adicional

### **Guías Técnicas**
- [Benchmark Development Guide](./docs/benchmark-development.md)
- [Performance Tuning Guide](./docs/performance-tuning.md)
- [CI/CD Configuration](./docs/cicd-configuration.md)

### **APIs y Referencias**
- [JMH Documentation](https://openjdk.java.net/projects/code-tools/jmh/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Maven Documentation](https://maven.apache.org/)

---

## 🤝 Contribución

### **Agregar Benchmarks**
1. Fork del repositorio
2. Crear branch para nueva feature
3. Implementar benchmark
4. Agregar tests correspondientes
5. Actualizar documentación
6. Crear Pull Request

### **Reportar Issues**
- Performance regressions
- Benchmarks faltantes
- Errores en reportes
- Mejoras de CI/CD

---

## 📞 Soporte

- **Issues**: [GitHub Issues](https://github.com/yasmramos/Veld/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yasmramos/Veld/discussions)
- **Email**: yasmramos@github.com

---

## 📄 Licencia

Este sistema de benchmarks es parte del proyecto Veld DI Framework y está bajo la licencia Apache 2.0.

---

**Última actualización**: 2025-12-12 09:05:25  
**Versión**: 1.0.0  
**Autor**: MiniMax Agent