#!/bin/bash
#
# Veld Performance Regression Test Runner
# ==========================================
# This script runs performance regression tests for the Veld framework.
#
# Usage:
#   ./run-regression-tests.sh           # Run quick regression tests
#   ./run-regression-tests.sh --full    # Run full regression suite
#   ./run-regression-tests.sh --help    # Show help
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default settings
MODE="quick"
THRESHOLD_MODE="normal"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --full)
            MODE="full"
            shift
            ;;
        --quick)
            MODE="quick"
            shift
            ;;
        --strict)
            THRESHOLD_MODE="strict"
            shift
            ;;
        --lenient)
            THRESHOLD_MODE="lenient"
            shift
            ;;
        --help|-h)
            echo "Veld Performance Regression Test Runner"
            echo "========================================"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --quick     Run quick regression tests (default)"
            echo "  --full      Run full regression suite"
            echo "  --strict    Use strict thresholds (5%/10%)"
            echo "  --lenient   Use lenient thresholds (15%/25%)"
            echo "  --help      Show this help message"
            echo ""
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         VELD PERFORMANCE REGRESSION TEST RUNNER               ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Navigate to benchmark directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${BLUE}Mode:${NC} $MODE"
echo -e "${BLUE}Threshold Mode:${NC} $THRESHOLD_MODE"
echo ""

# Check Java version
echo -e "${YELLOW}Checking environment...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed or not in PATH${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java found:$(NC} $(java -version 2>&1 | head -1)"

# Check if benchmark JAR exists
if [ ! -f "target/veld-benchmark.jar" ]; then
    echo -e "${YELLOW}⚠️  Benchmark JAR not found. Building...${NC}"
    echo ""

    # Build the benchmark module
    echo -e "${BLUE}Building benchmark module...${NC}"
    if ! mvn clean package -DskipTests -q; then
        echo -e "${RED}❌ Failed to build benchmark module${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Benchmark module built successfully${NC}"
fi

# Create results directory
mkdir -p target/regression-results

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  PHASE 1: Running Benchmarks                                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Run benchmarks based on mode
if [ "$MODE" == "quick" ]; then
    echo -e "${YELLOW}Running quick benchmarks (1 fork, 1 warmup, 2 iterations)...${NC}"

    # Run key benchmarks
    java -Xmx1g -jar target/veld-benchmark.jar ".*Injection.*" \
        -f 1 -wi 1 -i 2 -bs 1 \
        -rf json \
        -rff target/regression-results/injection-results.json

    java -Xmx1g -jar target/veld-benchmark.jar ".*Startup.*" \
        -f 1 -wi 1 -i 2 -bs 1 \
        -rf json \
        -rff target/regression-results/startup-results.json

    java -Xmx1g -jar target/veld-benchmark.jar ".*Throughput.*" \
        -f 1 -wi 1 -i 2 -bs 1 \
        -rf json \
        -rff target/regression-results/throughput-results.json

    java -Xmx1g -jar target/veld-benchmark.jar ".*Memory.*" \
        -f 1 -wi 1 -i 2 -bs 1 \
        -rf json \
        -rff target/regression-results/memory-results.json

elif [ "$MODE" == "full" ]; then
    echo -e "${YELLOW}Running full benchmarks (5 forks, 5 warmup, 10 iterations)...${NC}"

    java -Xmx2g -jar target/veld-benchmark.jar \
        -f 5 -wi 5 -i 10 \
        -rf json \
        -rff target/regression-results/full-results.json
fi

echo ""
echo -e "${GREEN}✓ Benchmarks completed${NC}"

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  PHASE 2: Validating Regression                               ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Combine results into a single file
echo -e "${YELLOW}Combining benchmark results...${NC}"

# Create combined results file
cat > target/regression-results/combined-results.json << 'JSON_START'
{
  "benchmarks": {
JSON_START

# Add injection results (without outer braces)
tail -n +2 target/regression-results/injection-results.json 2>/dev/null | head -n -1 >> target/regression-results/combined-results.json
echo ',' >> target/regression-results/combined-results.json

# Add startup results
tail -n +2 target/regression-results/startup-results.json 2>/dev/null | head -n -1 >> target/regression-results/combined-results.json
echo ',' >> target/regression-results/combined-results.json

# Add throughput results
tail -n +2 target/regression-results/throughput-results.json 2>/dev/null | head -n -1 >> target/regression-results/combined-results.json
echo ',' >> target/regression-results/combined-results.json

# Add memory results
tail -n +2 target/regression-results/memory-results.json 2>/dev/null | head -n -1 >> target/regression-results/combined-results.json

cat >> target/regression-results/combined-results.json << 'JSON_END'
  }
}
JSON_END

echo -e "${GREEN}✓ Results combined${NC}"

# Copy to expected location for regression test
cp target/regression-results/combined-results.json target/benchmark-results.json 2>/dev/null || true

# Run regression validation
echo ""
echo -e "${YELLOW}Running regression validation...${NC}"

# Compile and run the regression test
javac -cp "target/veld-benchmark.jar:target/classes" \
    src/main/java/io/github/yasmramos/veld/benchmark/regression/PerformanceRegressionTest.java

# Run the regression test
java -cp "target/veld-benchmark.jar:target/classes:src/main/java" \
    io.github.yasmramos.veld.benchmark.regression.PerformanceRegressionTest \
    --run-benchmarks

EXIT_CODE=$?

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  PHASE 3: Summary                                             ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Show results summary
if [ -d "target/regression-results" ]; then
    echo -e "${GREEN}Results saved to: target/regression-results/${NC}"
    ls -la target/regression-results/
fi

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    if grep -q "WARNINGS" target/regression-results/*.txt 2>/dev/null; then
        echo -e "${YELLOW}⚠️  REGRESSION TESTS PASSED WITH WARNINGS${NC}"
        echo -e "${YELLOW}   Review the output above for details${NC}"
    else
        echo -e "${GREEN}✅ ALL REGRESSION TESTS PASSED${NC}"
    fi
else
    echo -e "${RED}❌ REGRESSION TESTS FAILED${NC}"
    echo -e "${RED}   Performance degradation detected beyond thresholds${NC}"
    echo ""
    echo -e "${RED}Possible causes:${NC}"
    echo "  - Recent code changes introduced performance regression"
    echo "  - Memory pressure or system load during test"
    echo "  - Environment differences from baseline"
    echo ""
    echo -e "${RED}Recommended actions:${NC}"
    echo "  1. Review recent code changes"
    echo "  2. Run tests again to rule out environment issues"
    echo "  3. Update baselines if changes are intentional"
fi

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "For detailed results, see: target/regression-results/"
echo "════════════════════════════════════════════════════════════════"

exit $EXIT_CODE
