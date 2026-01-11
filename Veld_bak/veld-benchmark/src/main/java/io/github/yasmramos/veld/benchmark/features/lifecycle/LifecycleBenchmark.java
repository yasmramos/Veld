package io.github.yasmramos.veld.benchmark.features.lifecycle;

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
public class LifecycleBenchmark {

    @Benchmark
    public void measureLifecycleBean(Blackhole bh) {
        LifecycleBean bean = Veld.get(LifecycleBean.class);
        bean.doWork();
        bh.consume(bean);
    }

    @Benchmark
    public void measurePlainBean(Blackhole bh) {
        PlainLifecycleBean bean = Veld.get(PlainLifecycleBean.class);
        bean.doWork();
        bh.consume(bean);
    }
}
