package io.github.yasmramos.veld.benchmark.features.events;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.runtime.event.EventBus;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, warmups = 0)
public class EventBenchmark {

    private EventBus eventBus;
    private BenchmarkEventListener listener;

    @Setup
    public void setup() {
        eventBus = EventBus.getInstance();
        eventBus.clear();
        listener = Veld.benchmarkEventListener();
        eventBus.register(listener);
    }

    @TearDown
    public void teardown() {
        eventBus.clear();
    }

    @Benchmark
    public void measureEventPublish(Blackhole bh) {
        BenchmarkEvent event = new BenchmarkEvent();
        eventBus.publish(event);
        bh.consume(event);
    }

    @Benchmark
    public void measureMethodCall(Blackhole bh) {
        listener.onBenchmarkEvent(new BenchmarkEvent());
        bh.consume(listener);
    }
}
