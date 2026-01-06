package io.github.yasmramos.veld.benchmark.features.validation;

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
public class ValidationBenchmark {

    @Benchmark
    public void measureValidationOverhead(Blackhole bh) {
        ValidatedBean bean = Veld.get(ValidatedBean.class);
        ValidatedBean data = new ValidatedBean();
        bean.processWithValidation(data);
        bh.consume(bean);
    }

    @Benchmark
    public void measureNoValidation(Blackhole bh) {
        PlainValidatedBean bean = Veld.get(PlainValidatedBean.class);
        PlainValidatedBean data = new PlainValidatedBean();
        bean.process(data);
        bh.consume(bean);
    }
}
