#!/usr/bin/env python3
"""
Strategic Benchmark Results Analyzer
Analyzes Veld Framework strategic validation benchmark results and generates insights.
"""

import json
import sys
import os
from datetime import datetime
from typing import Dict, List, Any

class StrategicBenchmarkAnalyzer:
    def __init__(self):
        self.results = {}
        self.insights = []
        
    def load_results(self, filepath: str) -> Dict[str, Any]:
        """Load benchmark results from JSON file."""
        try:
            with open(filepath, 'r') as f:
                return json.load(f)
        except FileNotFoundError:
            print(f"Warning: {filepath} not found")
            return {}
        except json.JSONDecodeError:
            print(f"Warning: Invalid JSON in {filepath}")
            return {}

    def analyze_scalability(self, results: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze scalability performance."""
        if 'benchmarks' not in results:
            return {}
            
        benchmarks = results['benchmarks']
        concurrent_data = None
        single_data = None
        
        # Find concurrent and single thread results
        for bench in benchmarks:
            if 'concurrentLookup' in bench['benchmark']:
                concurrent_data = bench
            elif 'singleThreadLookup' in bench['benchmark']:
                single_data = bench
                
        if not concurrent_data or not single_data:
            return {}
            
        # Calculate efficiency ratio
        concurrent_avg = concurrent_data['primaryMetric']['score']
        single_avg = single_data['primaryMetric']['score']
        efficiency = concurrent_avg / (single_avg * 4)  # 4 threads
        
        return {
            'concurrent_avg_ns': concurrent_avg,
            'single_avg_ns': single_avg,
            'efficiency_ratio': efficiency,
            'efficiency_percentage': efficiency * 100,
            'passed': efficiency > 0.8,  # Target: >80% efficiency
            'status': 'PASS' if efficiency > 0.8 else 'FAIL'
        }

    def analyze_contention(self, results: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze lazy initialization contention."""
        if 'benchmarks' not in results:
            return {}
            
        benchmarks = results['benchmarks']
        lazy_data = None
        
        for bench in benchmarks:
            if 'getLazyService' in bench['benchmark']:
                lazy_data = bench
                break
                
        if not lazy_data:
            return {}
            
        avg_time = lazy_data['primaryMetric']['score']
        
        # Analyze performance with 8 threads
        return {
            'avg_lazy_lookup_ns': avg_time,
            'threads': 8,
            'passed': avg_time < 1000,  # Target: <1μs per lookup
            'status': 'PASS' if avg_time < 1000 else 'FAIL'
        }

    def analyze_memory_overhead(self, results: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze memory overhead and ThreadLocal behavior."""
        if 'benchmarks' not in results:
            return {}
            
        benchmarks = results['benchmarks']
        memory_data = None
        cache_data = None
        
        for bench in benchmarks:
            if 'memoryOverhead' in bench['benchmark']:
                memory_data = bench
            elif 'threadLocalCacheBehavior' in bench['benchmark']:
                cache_data = bench
                
        memory_mb = 0
        cache_mb = 0
        
        if memory_data:
            memory_mb = memory_data['primaryMetric']['score'] / (1024 * 1024)
            
        if cache_data:
            cache_mb = cache_data['primaryMetric']['score'] / (1024 * 1024)
            
        return {
            'memory_overhead_mb': memory_mb,
            'threadlocal_cache_mb': cache_mb,
            'total_memory_mb': memory_mb + cache_mb,
            'passed': (memory_mb + cache_mb) < 10,  # Target: <10MB overhead
            'status': 'PASS' if (memory_mb + cache_mb) < 10 else 'FAIL'
        }

    def analyze_hash_collision(self, results: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze hash collision impact."""
        if 'benchmarks' not in results:
            return {}
            
        benchmarks = results['benchmarks']
        collision_data = None
        
        for bench in benchmarks:
            if 'worstCaseHashCollision' in bench['benchmark']:
                collision_data = bench
                break
                
        if not collision_data:
            return {}
            
        avg_time = collision_data['primaryMetric']['score']
        
        return {
            'worst_case_lookup_ns': avg_time,
            'analysis': 'Tests current O(n) array search with clustered access patterns',
            'passed': avg_time < 500,  # Target: <500ns worst case
            'status': 'PASS' if avg_time < 500 else 'FAIL'
        }

    def analyze_efficiency(self, results: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze efficiency ratio calculation."""
        if 'benchmarks' not in results:
            return {}
            
        benchmarks = results['benchmarks']
        concurrent_eff = None
        single_eff = None
        
        for bench in benchmarks:
            if 'concurrentEfficiency' in bench['benchmark']:
                concurrent_eff = bench
            elif 'singleEfficiency' in bench['benchmark']:
                single_eff = bench
                
        if not concurrent_eff or not single_eff:
            return {}
            
        concurrent_avg = concurrent_eff['primaryMetric']['score']
        single_avg = single_eff['primaryMetric']['score']
        efficiency = concurrent_avg / (single_avg * 4)
        
        return {
            'concurrent_efficiency_ns': concurrent_avg,
            'single_efficiency_ns': single_avg,
            'efficiency_ratio': efficiency,
            'efficiency_percentage': efficiency * 100,
            'passed': efficiency > 0.75,  # Target: >75% efficiency
            'status': 'PASS' if efficiency > 0.75 else 'FAIL'
        }

    def analyze_load_factor(self, results: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze load factor validation."""
        if 'benchmarks' not in results:
            return {}
            
        benchmarks = results['benchmarks']
        load_factor_data = None
        
        for bench in benchmarks:
            if 'loadFactorValidation' in bench['benchmark']:
                load_factor_data = bench
                break
                
        if not load_factor_data:
            return {}
            
        load_factor = load_factor_data['primaryMetric']['score']
        
        return {
            'current_load_factor': load_factor,
            'target_load_factor': 0.7,
            'analysis': f'Current: {load_factor:.2f}, Target: 0.70 (Power-of-2 capacity)',
            'passed': load_factor < 0.7,
            'status': 'PASS' if load_factor < 0.7 else 'FAIL'
        }

    def generate_insights(self, analysis_results: Dict[str, Any]):
        """Generate strategic insights from analysis results."""
        
        # Scalability insights
        if 'scalability' in analysis_results:
            scal = analysis_results['scalability']
            if scal.get('passed', False):
                self.insights.append(f"✅ SCALABILITY: Excellent efficiency at {scal['efficiency_percentage']:.1f}%")
            else:
                self.insights.append(f"⚠️ SCALABILITY: Poor efficiency at {scal['efficiency_percentage']:.1f}% (target: >80%)")
                
        # Contention insights
        if 'contention' in analysis_results:
            cont = analysis_results['contention']
            if cont.get('passed', False):
                self.insights.append(f"✅ CONTENTION: Low contention latency {cont['avg_lazy_lookup_ns']:.0f}ns")
            else:
                self.insights.append(f"⚠️ CONTENTION: High contention latency {cont['avg_lazy_lookup_ns']:.0f}ns")
                
        # Memory insights
        if 'memory' in analysis_results:
            mem = analysis_results['memory']
            if mem.get('passed', False):
                self.insights.append(f"✅ MEMORY: Low overhead {mem['total_memory_mb']:.1f}MB")
            else:
                self.insights.append(f"⚠️ MEMORY: High overhead {mem['total_memory_mb']:.1f}MB")
                
        # Hash collision insights
        if 'hash_collision' in analysis_results:
            hash_col = analysis_results['hash_collision']
            if hash_col.get('passed', False):
                self.insights.append(f"✅ HASH COLLISION: Acceptable worst-case {hash_col['worst_case_lookup_ns']:.0f}ns")
            else:
                self.insights.append(f"⚠️ HASH COLLISION: Poor worst-case {hash_col['worst_case_lookup_ns']:.0f}ns")
                
        # Overall recommendations
        total_passed = sum(1 for result in analysis_results.values() if result.get('passed', False))
        total_tests = len([r for r in analysis_results.values() if r])
        
        self.insights.append(f"\n📊 OVERALL: {total_passed}/{total_tests} tests passed")
        
        if total_passed == total_tests:
            self.insights.append("🎉 ALL STRATEGIC TESTS PASSED - Framework ready for production!")
        elif total_passed >= total_tests * 0.8:
            self.insights.append("✅ MOSTLY READY - Minor optimizations recommended")
        else:
            self.insights.append("⚠️ NEEDS OPTIMIZATION - Several critical issues found")

    def generate_report(self, analysis_results: Dict[str, Any]) -> str:
        """Generate comprehensive analysis report."""
        
        report = []
        report.append("# VELD FRAMEWORK - STRATEGIC VALIDATION REPORT")
        report.append("=" * 50)
        report.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append("")
        
        # Executive Summary
        report.append("## EXECUTIVE SUMMARY")
        report.append("")
        total_passed = sum(1 for result in analysis_results.values() if result.get('passed', False))
        total_tests = len([r for r in analysis_results.values() if r])
        report.append(f"**Tests Passed:** {total_passed}/{total_tests} ({total_passed/total_tests*100:.1f}%)")
        report.append("")
        
        # Detailed Results
        report.append("## DETAILED ANALYSIS")
        report.append("")
        
        for category, results in analysis_results.items():
            if not results:
                continue
                
            report.append(f"### {category.replace('_', ' ').title()}")
            report.append("")
            
            for key, value in results.items():
                if key == 'analysis':
                    continue
                elif key == 'passed':
                    continue
                elif key == 'status':
                    report.append(f"**Status:** {value}")
                elif isinstance(value, float):
                    report.append(f"**{key.replace('_', ' ').title()}:** {value:.3f}")
                else:
                    report.append(f"**{key.replace('_', ' ').title()}:** {value}")
            report.append("")
        
        # Insights and Recommendations
        report.append("## STRATEGIC INSIGHTS")
        report.append("")
        for insight in self.insights:
            report.append(insight)
        report.append("")
        
        # Technical Recommendations
        report.append("## TECHNICAL RECOMMENDATIONS")
        report.append("")
        
        if not analysis_results.get('scalability', {}).get('passed', False):
            report.append("- **Scalability:** Consider implementing hash-based lookup for better concurrent performance")
            
        if not analysis_results.get('hash_collision', {}).get('passed', False):
            report.append("- **Hash Collision:** Current O(n) array search may degrade with 20+ services")
            
        if not analysis_results.get('memory', {}).get('passed', False):
            report.append("- **Memory:** High memory overhead detected - investigate ThreadLocal cache behavior")
            
        if not analysis_results.get('load_factor', {}).get('passed', False):
            report.append("- **Load Factor:** Consider power-of-2 array capacity for future hash implementation")
            
        report.append("")
        report.append("## CONCLUSION")
        report.append("")
        report.append("This strategic validation provides critical insights into Veld Framework's")
        report.append("performance characteristics and identifies areas for optimization.")
        
        return "\n".join(report)

    def run_analysis(self, results_dir: str = "results"):
        """Run complete analysis on all benchmark results."""
        
        # Load all result files
        result_files = [
            ("scalability", f"{results_dir}/scalability-results.json"),
            ("contention", f"{results_dir}/contention-results.json"),
            ("memory", f"{results_dir}/memory-results.json"),
            ("hash_collision", f"{results_dir}/hash-collision-results.json"),
            ("efficiency", f"{results_dir}/efficiency-results.json"),
            ("load_factor", f"{results_dir}/load-factor-results.json")
        ]
        
        analysis_results = {}
        
        for category, filepath in result_files:
            print(f"📊 Analyzing {category} results...")
            raw_results = self.load_results(filepath)
            
            if category == "scalability":
                analysis_results[category] = self.analyze_scalability(raw_results)
            elif category == "contention":
                analysis_results[category] = self.analyze_contention(raw_results)
            elif category == "memory":
                analysis_results[category] = self.analyze_memory_overhead(raw_results)
            elif category == "hash_collision":
                analysis_results[category] = self.analyze_hash_collision(raw_results)
            elif category == "efficiency":
                analysis_results[category] = self.analyze_efficiency(raw_results)
            elif category == "load_factor":
                analysis_results[category] = self.analyze_load_factor(raw_results)
        
        # Generate insights and report
        self.generate_insights(analysis_results)
        report = self.generate_report(analysis_results)
        
        # Save report
        report_file = f"{results_dir}/strategic-analysis-report.md"
        with open(report_file, 'w') as f:
            f.write(report)
            
        print(f"\n📋 Analysis report saved to: {report_file}")
        print("\n" + "="*50)
        print("STRATEGIC VALIDATION SUMMARY")
        print("="*50)
        for insight in self.insights:
            print(insight)
            
        return analysis_results

if __name__ == "__main__":
    analyzer = StrategicBenchmarkAnalyzer()
    results = analyzer.run_analysis()
    
    # Exit with error code if any critical tests failed
    critical_failures = sum(1 for result in results.values() 
                           if result and not result.get('passed', False))
    
    if critical_failures > 0:
        print(f"\n❌ {critical_failures} critical tests failed")
        sys.exit(1)
    else:
        print(f"\n✅ All strategic tests passed!")
        sys.exit(0)