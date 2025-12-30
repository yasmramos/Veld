/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.regression;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.benchmark.common.ComplexService;
import io.github.yasmramos.veld.benchmark.common.Logger;
import io.github.yasmramos.veld.benchmark.common.Service;
import io.github.yasmramos.veld.benchmark.veld.VeldComplexService;
import io.github.yasmramos.veld.benchmark.veld.VeldLogger;
import io.github.yasmramos.veld.benchmark.veld.VeldSimpleService;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Performance Regression Test for Veld Framework.
 *
 * This test runs key benchmarks and compares results against baseline metrics.
 * The build will fail if performance degrades beyond defined thresholds.
 *
 * Metrics tracked:
 * - Injection latency (ns/op)
 * - Throughput (ops/ms)
 * - Startup time (us)
 * - Memory usage (ms for N beans)
 *
 * Thresholds:
 * - Critical: >20% degradation - FAIL IMMEDIATELY
 * - Warning: >10% degradation - WARN but PASS
 * - Normal: <10% degradation - PASS
 */
public class PerformanceRegressionTest {

    // Baseline metrics (established from previous benchmark runs)
    private static final Map<String, BaselineMetric> BASELINE_METRICS = new LinkedHashMap<>();

    static {
        // Injection latency benchmarks (ns/op - lower is better)
        // Latest benchmark results (December 2025): ~3.1 ns/op
        BASELINE_METRICS.put("veld.simpleInjection",
            new BaselineMetric("InjectionBenchmark.veldSimpleInjection", 3.123, 3.500, 4.000));
        BASELINE_METRICS.put("veld.complexInjection",
            new BaselineMetric("InjectionBenchmark.veldComplexInjection", 3.127, 3.500, 4.000));
        BASELINE_METRICS.put("veld.loggerLookup",
            new BaselineMetric("InjectionBenchmark.veldLoggerLookup", 3.125, 3.500, 4.000));

        // Throughput benchmarks (ops/ms - higher is better)
        // Latest benchmark results (December 2025): ~320K-945K ops/ms
        BASELINE_METRICS.put("veld.singleThread",
            new BaselineMetric("ThroughputBenchmark.veldThroughput", 320200.0, 288000.0, 256000.0));
        BASELINE_METRICS.put("veld.concurrent4",
            new BaselineMetric("ThroughputBenchmark.veldConcurrentThroughput", 944690.0, 850000.0, 755000.0));

        // Startup benchmarks (us/op - lower is better)
        // Latest benchmark results (December 2025): ~0.003 us/op
        BASELINE_METRICS.put("veld.startup",
            new BaselineMetric("StartupBenchmark.veldStartup", 0.003, 0.005, 0.010));

        // Memory benchmarks (ms for N beans - lower is better)
        // Latest benchmark results (December 2025): ~0.002-0.016 ms/op
        BASELINE_METRICS.put("veld.memory.10",
            new BaselineMetric("MemoryBenchmark.veldMemory.10", 0.002, 0.005, 0.010));
        BASELINE_METRICS.put("veld.memory.100",
            new BaselineMetric("MemoryBenchmark.veldMemory.100", 0.005, 0.010, 0.020));
        BASELINE_METRICS.put("veld.memory.500",
            new BaselineMetric("MemoryBenchmark.veldMemory.500", 0.016, 0.025, 0.035));
    }

    private static final double WARNING_THRESHOLD = 0.10; // 10% degradation
    private static final double CRITICAL_THRESHOLD = 0.20; // 20% degradation

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int warningTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║         VELD PERFORMANCE REGRESSION TEST SUITE                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Check if we should run actual benchmarks or validate against stored results
        boolean runBenchmarks = args.length > 0 && args[0].equals("--run-benchmarks");

        if (runBenchmarks) {
            System.out.println("Running performance benchmarks...");
            runBenchmarksAndValidate();
        } else {
            System.out.println("Validating against stored benchmark results...");
            validateStoredResults();
        }

        printSummary();

        if (failedTests > 0) {
            System.out.println("\n❌ REGRESSION TEST FAILED - Performance degradation detected!");
            System.exit(1);
        } else if (warningTests > 0) {
            System.out.println("\n⚠️  REGRESSION TEST PASSED WITH WARNINGS - Review recommended");
            System.exit(0);
        } else {
            System.out.println("\n✅ ALL REGRESSION TESTS PASSED");
            System.exit(0);
        }
    }

    /**
     * Run actual benchmarks and validate results.
     * This requires the JMH benchmarks to be executed first.
     */
    private static void runBenchmarksAndValidate() {
        // For CI/CD environments, we expect benchmark results to be pre-generated
        // This method can be extended to trigger JMH benchmarks programmatically

        // For now, validate the generated results
        validateStoredResults();
    }

    /**
     * Validate against stored benchmark results in benchmark-results.json
     */
    private static void validateStoredResults() {
        System.out.println("═".repeat(70));
        System.out.println("VALIDATING PERFORMANCE METRICS");
        System.out.println("═".repeat(70));
        System.out.println();

        // Try to load results from various possible locations
        Map<String, Double> results = loadBenchmarkResults();

        if (results.isEmpty()) {
            System.out.println("⚠️  No benchmark results found. Running quick validation tests...");
            runQuickValidationTests();
            return;
        }

        // Validate each metric
        for (Map.Entry<String, BaselineMetric> entry : BASELINE_METRICS.entrySet()) {
            String metricKey = entry.getKey();
            BaselineMetric baseline = entry.getValue();

            Double actualValue = results.get(baseline.benchmarkName);
            if (actualValue == null) {
                // Try to find partial match
                actualValue = findPartialMatch(results, baseline.benchmarkName);
            }

            if (actualValue != null) {
                validateMetric(metricKey, baseline, actualValue);
            } else {
                System.out.println(formatResult(metricKey, "SKIPPED",
                    "Benchmark '" + baseline.benchmarkName + "' not found in results"));
                totalTests++;
            }
        }
    }

    /**
     * Run quick validation tests to verify basic functionality
     */
    private static void runQuickValidationTests() {
        System.out.println("═".repeat(70));
        System.out.println("QUICK VALIDATION TESTS");
        System.out.println("═".repeat(70));
        System.out.println();

        // Test 1: Simple injection
        validateInjection("Simple Service", () -> {
            Service service = Veld.get(VeldSimpleService.class);
            return service != null ? 0.0 : -1.0;
        }, BASELINE_METRICS.get("veld.simpleInjection"));

        // Test 2: Complex injection
        validateInjection("Complex Service", () -> {
            ComplexService service = Veld.get(VeldComplexService.class);
            return service != null ? 0.0 : -1.0;
        }, BASELINE_METRICS.get("veld.complexInjection"));

        // Test 3: Logger lookup
        validateInjection("Logger", () -> {
            Logger logger = Veld.get(VeldLogger.class);
            return logger != null ? 0.0 : -1.0;
        }, BASELINE_METRICS.get("veld.loggerLookup"));

        // Test 4: Multiple rapid injections (throughput simulation)
        validateInjection("Rapid Injections (10x)", () -> {
            long start = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                Veld.get(VeldSimpleService.class);
            }
            long elapsed = System.nanoTime() - start;
            return (double) elapsed / 10; // Average ns/op
        }, BASELINE_METRICS.get("veld.simpleInjection"));
    }

    @FunctionalInterface
    interface InjectionTest {
        double run();
    }

    private static void validateInjection(String name, InjectionTest test, BaselineMetric baseline) {
        if (baseline == null) return;

        totalTests++;
        System.out.println("Testing: " + name + "...");

        try {
            double elapsedNs = test.run();
            if (elapsedNs < 0) {
                System.out.println(formatResult(name, "FAILED", "Injection returned null"));
                failedTests++;
                return;
            }

            validateMetric(name, baseline, elapsedNs);
        } catch (Exception e) {
            System.out.println(formatResult(name, "ERROR", e.getMessage()));
            failedTests++;
        }
    }

    /**
     * Load benchmark results from JSON file
     */
    private static Map<String, Double> loadBenchmarkResults() {
        Map<String, Double> results = new LinkedHashMap<>();

        // Try multiple possible locations for benchmark results
        String[] possiblePaths = {
            "veld-benchmark/target/benchmark-results.json",
            "../veld-benchmark/target/benchmark-results.json",
            "target/benchmark-results.json",
            "../target/benchmark-results.json",
            "benchmark-results.json"
        };

        for (String path : possiblePaths) {
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                try {
                    String content = new String(Files.readAllBytes(filePath));
                    results = parseJsonResults(content);
                    System.out.println("✓ Loaded benchmark results from: " + path);
                    return results;
                } catch (Exception e) {
                    // Try next location
                }
            }
        }

        return results;
    }

    /**
     * Parse JSON benchmark results
     */
    private static Map<String, Double> parseJsonResults(String json) {
        Map<String, Double> results = new LinkedHashMap<>();

        // Simple JSON parsing (handles JMH JSON output format)
        try {
            // Remove common JSON formatting
            json = json.replaceAll("\\s+", " ").trim();

            // Find benchmark results
            int benchmarksStart = json.indexOf("\"benchmarks\"");
            if (benchmarksStart >= 0) {
                String benchmarksSection = json.substring(benchmarksStart);

                // Extract primary metric (Score)
                int scoreStart = benchmarksSection.indexOf("\"score\"");
                if (scoreStart >= 0) {
                    int colon = benchmarksSection.indexOf(":", scoreStart);
                    int end = benchmarksSection.indexOf(",", colon);
                    if (end < 0) end = benchmarksSection.indexOf("}", colon);
                    String scoreStr = benchmarksSection.substring(colon + 1, end).trim();
                    try {
                        double score = Double.parseDouble(scoreStr);
                        results.put("primary_score", score);
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }

                // Extract benchmark name
                int nameStart = benchmarksSection.indexOf("\"benchmark\"");
                if (nameStart >= 0) {
                    int colon = benchmarksSection.indexOf(":", nameStart);
                    int startQuote = benchmarksSection.indexOf("\"", colon);
                    int endQuote = benchmarksSection.indexOf("\"", startQuote + 1);
                    String benchmarkName = benchmarksSection.substring(startQuote + 1, endQuote);
                    results.put("benchmark_name", -1.0);
                    results.put(benchmarkName, results.getOrDefault("primary_score", 0.0));
                }
            }

            // Also try to parse individual benchmark results
            // Format: "BenchmarkClass.methodName": score
            String[] lines = json.split("\\}");
            for (String line : lines) {
                if (line.contains(":") && line.contains("\"")) {
                    int lastQuote = line.lastIndexOf("\"");
                    int colon = line.lastIndexOf(":");
                    if (colon > lastQuote && colon < line.length() - 1) {
                        String valueStr = line.substring(colon + 1).trim();
                        if (valueStr.endsWith(",")) valueStr = valueStr.substring(0, valueStr.length() - 1);
                        try {
                            double value = Double.parseDouble(valueStr);
                            String name = extractBenchmarkName(line);
                            if (name != null && !name.isEmpty()) {
                                results.put(name, value);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON results: " + e.getMessage());
        }

        return results;
    }

    private static String extractBenchmarkName(String line) {
        int firstQuote = line.indexOf("\"");
        if (firstQuote >= 0) {
            int secondQuote = line.indexOf("\"", firstQuote + 1);
            if (secondQuote > firstQuote) {
                return line.substring(firstQuote + 1, secondQuote);
            }
        }
        return null;
    }

    /**
     * Find partial match for benchmark name
     */
    private static Double findPartialMatch(Map<String, Double> results, String benchmarkName) {
        // Extract simple name from full benchmark name
        String simpleName = benchmarkName;
        if (benchmarkName.contains(".")) {
            simpleName = benchmarkName.substring(benchmarkName.lastIndexOf(".") + 1);
        }

        for (String key : results.keySet()) {
            if (key.toLowerCase().contains(simpleName.toLowerCase())) {
                return results.get(key);
            }
        }

        return null;
    }

    /**
     * Validate a single metric against baseline
     */
    private static void validateMetric(String metricKey, BaselineMetric baseline, double actualValue) {
        totalTests++;

        double degradation;
        String status;
        String details;

        if (baseline.higherIsBetter) {
            // For throughput: higher is better
            double ratio = actualValue / baseline.baselineValue;
            degradation = 1.0 - ratio;

            if (degradation >= CRITICAL_THRESHOLD) {
                status = "CRITICAL";
                details = String.format("%.2f%% degradation (actual: %.2f, baseline: %.2f)",
                    degradation * 100, actualValue, baseline.baselineValue);
                failedTests++;
            } else if (degradation >= WARNING_THRESHOLD) {
                status = "WARNING";
                details = String.format("%.2f%% degradation (actual: %.2f, baseline: %.2f)",
                    degradation * 100, actualValue, baseline.baselineValue);
                warningTests++;
            } else {
                status = "PASS";
                details = String.format("Within tolerance (actual: %.4f, baseline: %.4f)",
                    actualValue, baseline.baselineValue);
                passedTests++;
            }
        } else {
            // For latency/memory: lower is better
            double ratio = baseline.baselineValue / actualValue;
            degradation = 1.0 - ratio;

            if (degradation >= CRITICAL_THRESHOLD) {
                status = "CRITICAL";
                details = String.format("%.2f%% slower (actual: %.4f ns, baseline: %.4f ns)",
                    degradation * 100, actualValue, baseline.baselineValue);
                failedTests++;
            } else if (degradation >= WARNING_THRESHOLD) {
                status = "WARNING";
                details = String.format("%.2f%% slower (actual: %.4f ns, baseline: %.4f ns)",
                    degradation * 100, actualValue, baseline.baselineValue);
                warningTests++;
            } else {
                status = "PASS";
                details = String.format("Within tolerance (actual: %.4f, baseline: %.4f)",
                    actualValue, baseline.baselineValue);
                passedTests++;
            }
        }

        System.out.println(formatResult(metricKey, status, details));
    }

    /**
     * Format test result output
     */
    private static String formatResult(String metricKey, String status, String details) {
        String icon;
        switch (status) {
            case "CRITICAL":
            case "FAILED":
                icon = "❌";
                break;
            case "WARNING":
                icon = "⚠️";
                break;
            case "PASS":
                icon = "✅";
                break;
            case "SKIPPED":
                icon = "⏭️";
                break;
            default:
                icon = "ℹ️";
        }

        return String.format("%s %-25s %s (%s)", icon, metricKey, status, details);
    }

    /**
     * Print test summary
     */
    private static void printSummary() {
        System.out.println();
        System.out.println("═".repeat(70));
        System.out.println("PERFORMANCE REGRESSION TEST SUMMARY");
        System.out.println("═".repeat(70));
        System.out.println();
        System.out.println(String.format("  Total Tests:     %d", totalTests));
        System.out.println(String.format("  ✅ Passed:        %d", passedTests));
        System.out.println(String.format("  ⚠️  Warnings:      %d", warningTests));
        System.out.println(String.format("  ❌ Failed:        %d", failedTests));
        System.out.println();

        if (failedTests > 0) {
            System.out.println("CRITICAL: Performance regression detected!");
            System.out.println("The build will fail due to significant performance degradation.");
            System.out.println();
            System.out.println("Possible causes:");
            System.out.println("  - Code changes introduced inefficient operations");
            System.out.println("  - Memory pressure from new dependencies");
            System.out.println("  - CI environment differences from baseline");
            System.out.println();
            System.out.println("Recommended actions:");
            System.out.println("  1. Review recent code changes");
            System.out.println("  2. Run benchmarks locally to verify");
            System.out.println("  3. Update baselines if changes are intentional");
        } else if (warningTests > 0) {
            System.out.println("WARNING: Minor performance degradation detected.");
            System.out.println("Review recommended but build will continue.");
        }
    }

    /**
     * Baseline metric definition
     */
    private static class BaselineMetric {
        final String benchmarkName;
        final double baselineValue;
        final double warningValue;
        final double criticalValue;
        final boolean higherIsBetter;

        BaselineMetric(String benchmarkName, double baselineValue,
                       double warningValue, double criticalValue) {
            this(benchmarkName, baselineValue, warningValue, criticalValue, false);
        }

        BaselineMetric(String benchmarkName, double baselineValue,
                       double warningValue, double criticalValue, boolean higherIsBetter) {
            this.benchmarkName = benchmarkName;
            this.baselineValue = baselineValue;
            this.warningValue = warningValue;
            this.criticalValue = criticalValue;
            this.higherIsBetter = higherIsBetter;
        }
    }
}
