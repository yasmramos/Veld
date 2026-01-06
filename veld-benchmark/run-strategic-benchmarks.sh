#!/bin/bash

# Strategic Validation Benchmark Runner
# This script runs comprehensive strategic benchmarks for Veld Framework

echo "🚀 VELD FRAMEWORK - STRATEGIC VALIDATION BENCHMARKS"
echo "=================================================="
echo "Date: $(date)"
echo "Java: $(java -version 2>&1 | head -1)"
echo "Maven: $(mvn -version | head -1)"
echo ""

# Navigate to benchmark module
cd "$(dirname "$0")/.." || exit 1

# Clean and compile
echo "📦 Compiling benchmark module..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo "✅ Compilation successful!"
echo ""

# Run strategic validation benchmarks
echo "🔬 Running Strategic Validation Benchmarks..."
echo "This will take several minutes..."
echo ""

# Build the benchmark JAR
echo "🔨 Building benchmark JAR..."
mvn package -DskipTests -q
if [ $? -ne 0 ]; then
    echo "❌ JAR build failed!"
    exit 1
fi

echo "✅ Benchmark JAR built successfully!"
echo ""

# Run specific strategic benchmarks
echo "🏃 Running benchmarks..."

# 1. Pure Scalability Test
echo "1️⃣ Pure Scalability Benchmark (Concurrent vs Single-thread)..."
java -jar target/veld-benchmark.jar ".*concurrentLookup.*" -f 2 -wi 3 -i 5 -rf json -rff results/scalability-results.json

# 2. Specific Contention Test  
echo "2️⃣ Lazy Initialization Contention Benchmark..."
java -jar target/veld-benchmark.jar ".*getLazyService.*" -f 2 -wi 3 -i 5 -rf json -rff results/contention-results.json

# 3. Memory Overhead Test
echo "3️⃣ Memory Overhead Validation..."
java -jar target/veld-benchmark.jar ".*memoryOverhead.*" -f 2 -wi 3 -i 5 -rf json -rff results/memory-results.json

# 4. Hash Collision Impact Test
echo "4️⃣ Hash Collision Impact Benchmark..."
java -jar target/veld-benchmark.jar ".*worstCaseHashCollision.*" -f 2 -wi 3 -i 5 -rf json -rff results/hash-collision-results.json

# 5. Efficiency Calculation
echo "5️⃣ Efficiency Ratio Calculation..."
java -jar target/veld-benchmark.jar ".*efficiency.*" -f 2 -wi 3 -i 5 -rf json -rff results/efficiency-results.json

# 6. Load Factor Validation
echo "6️⃣ Load Factor Validation..."
java -jar target/veld-benchmark.jar ".*loadFactorValidation.*" -f 2 -wi 3 -i 5 -rf json -rff results/load-factor-results.json

# Run all strategic benchmarks together
echo ""
echo "🎯 Running Complete Strategic Validation Suite..."
java -jar target/veld-benchmark.jar -f 2 -wi 3 -i 5 -rf json -rff results/strategic-validation-complete.json

echo ""
echo "✅ All strategic benchmarks completed!"
echo ""
echo "📊 Results saved to:"
echo "   - results/scalability-results.json"
echo "   - results/contention-results.json" 
echo "   - results/memory-results.json"
echo "   - results/hash-collision-results.json"
echo "   - results/efficiency-results.json"
echo "   - results/load-factor-results.json"
echo "   - results/strategic-validation-complete.json"
echo ""

# Generate analysis report
echo "📈 Generating Analysis Report..."
python3 scripts/analyze-strategic-results.py

echo ""
echo "🎉 Strategic Validation Complete!"
echo "Check the analysis report for detailed insights."
