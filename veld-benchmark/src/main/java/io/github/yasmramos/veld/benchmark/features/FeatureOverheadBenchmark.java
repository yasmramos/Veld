package io.github.yasmramos.veld.benchmark.features;

import io.github.yasmramos.veld.Veld;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, warmups = 0)
public class FeatureOverheadBenchmark {

    // --- Benchmark Methods ---

    @Benchmark
    public void measureAopExecution(Blackhole bh) {
        AopTargetBean bean = Veld.aopTargetBean();
        bean.doWork();
        bh.consume(bean);
    }

    @Benchmark
    public void measureAopBaseline(Blackhole bh) {
        PlainBean bean = Veld.plainBean();
        bean.method();
        bh.consume(bean);
    }
}
