package io.github.yasmramos.veld.benchmark.features.event;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.annotation.Subscribe;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.runtime.event.Event;
import io.github.yasmramos.veld.runtime.event.EventBus;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark suite completo para EventBus optimization.
 * Mide cada componente del stack de eventos para identificar bottlenecks específicos.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, warmups = 0)
public class EventBusBaselineBenchmark {

    // ========================================
    // Benchmark State
    // ========================================

    private EventBus eventBus;
    private SimpleListener simpleListener;
    private MultiMethodListener multiMethodListener;
    private SimpleEvent simpleEvent;
    private ComplexEvent complexEvent;
    private List<Event> eventBatch;

    @Param({"1", "5", "10", "25", "50"})
    private int listenerCount;

    @Setup
    public void setup() {
        eventBus = EventBus.getInstance();
        eventBus.clear();

        // Get instances from Veld
        simpleListener = Veld.simpleListener();
        multiMethodListener = Veld.multiMethodListener();

        // Create test events
        simpleEvent = new SimpleEvent("test-data");
        complexEvent = new ComplexEvent("id-123", "user.created", 5, List.of("tag1", "tag2"));

        // Create event batch for batch benchmarks
        eventBatch = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            eventBatch.add(new SimpleEvent("batch-" + i));
        }
    }

    @TearDown
    public void teardown() {
        eventBus.clear();
    }

    // ========================================
    // Baseline Benchmarks - Event Creation
    // ========================================

    @Benchmark
    public SimpleEvent baselineEventCreationSimple() {
        return new SimpleEvent("test");
    }

    @Benchmark
    public ComplexEvent baselineEventCreationComplex() {
        return new ComplexEvent("id", "type", 5, List.of("a", "b"));
    }

    // ========================================
    // Baseline Benchmarks - Direct Dispatch
    // ========================================

    @Benchmark
    public void benchmarkDirectMethodCall() {
        // Sin EventBus, llamada directa
        simpleListener.handleSimpleEvent(simpleEvent);
    }

    // ========================================
    // Main Benchmarks - Current EventBus
    // ========================================

    @Benchmark
    public int benchmarkEventPublishSimple() {
        return eventBus.publish(new SimpleEvent("test-publish"));
    }

    @Benchmark
    public int benchmarkEventPublishComplex() {
        return eventBus.publish(new ComplexEvent("id-456", "order.created", 3, List.of("order")));
    }

    @Benchmark
    public int benchmarkEventPublishWithExistingEvent() {
        // Reuse existing event to measure publish overhead only
        return eventBus.publish(simpleEvent);
    }

    // ========================================
    // Scalability Benchmarks
    // ========================================

    @Benchmark
    public int benchmarkPublishWithNListeners() {
        // Registrar listeners temporales
        List<SimpleListener> tempListeners = new ArrayList<>();
        for (int i = 0; i < listenerCount; i++) {
            SimpleListener temp = new SimpleListener();
            tempListeners.add(temp);
            eventBus.register(temp);
        }

        try {
            return eventBus.publish(simpleEvent);
        } finally {
            for (Object listener : tempListeners) {
                eventBus.unregister(listener);
            }
        }
    }

    @Benchmark
    public int benchmarkPublishWithMultipleMethodListener() {
        multiMethodListener.reset();
        return eventBus.publish(simpleEvent);
    }

    // ========================================
    // Batch Benchmarks
    // ========================================

    @Benchmark
    public long benchmarkPublishBatch10() {
        long start = System.nanoTime();
        int delivered = 0;
        for (int i = 0; i < 10; i++) {
            delivered += eventBus.publish(new SimpleEvent("batch-" + i));
        }
        return System.nanoTime() - start;
    }

    @Benchmark
    public long benchmarkPublishBatch100() {
        long start = System.nanoTime();
        int delivered = 0;
        for (int i = 0; i < 100; i++) {
            delivered += eventBus.publish(new SimpleEvent("batch-" + i));
        }
        return System.nanoTime() - start;
    }

    @Benchmark
    public long benchmarkPublishBatchList() {
        long start = System.nanoTime();
        int delivered = 0;
        for (Event event : eventBatch) {
            delivered += eventBus.publish(event);
        }
        return System.nanoTime() - start;
    }

    // ========================================
    // Concurrency Benchmarks
    // ========================================

    @Benchmark
    @Threads(2)
    public int benchmarkConcurrentPublishThread2() {
        return eventBus.publish(new SimpleEvent("concurrent"));
    }

    @Benchmark
    @Threads(4)
    public int benchmarkConcurrentPublishThread4() {
        return eventBus.publish(new SimpleEvent("concurrent"));
    }

    @Benchmark
    @Threads(8)
    public int benchmarkConcurrentPublishThread8() {
        return eventBus.publish(new SimpleEvent("concurrent"));
    }

    // ========================================
    // Async Dispatch Benchmarks
    // ========================================

    @Benchmark
    public CompletableFuture<Integer> benchmarkPublishAsync() {
        return eventBus.publishAsync(new SimpleEvent("async"));
    }

    // ========================================
    // Statistics Benchmarks
    // ========================================

    @Benchmark
    public long benchmarkGetStatistics() {
        return eventBus.getPublishedCount();
    }

    @Benchmark
    public int benchmarkGetSubscriberCount() {
        return eventBus.getSubscriberCount();
    }
}
