/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.benchmark.features.events;

import io.github.yasmramos.veld.runtime.event.Event;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.event.EventChannel;
import io.github.yasmramos.veld.runtime.event.ObjectLessEventBus;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark for object-less event publishing performance.
 *
 * <p>This benchmark compares the performance of object-less events
 * (using event IDs) vs traditional object-based events.</p>
 *
 * <p><b>Expected Results:</b></p>
 * <ul>
 *   <li>Object-less events should be significantly faster due to zero allocation</li>
 *   <li>Single listener case should show the most improvement</li>
 *   <li>Multiple listeners should show diminishing returns but still improved</li>
 * </ul>
 *
 * @author Veld Framework Team
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, warmups = 2)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
public class ObjectLessEventBenchmark {

    private EventBus eventBus;
    private ObjectLessEventBus.ObjectLessListener objectLessListener;
    private EventBus.EventListener objectBasedListener;
    private Event testEvent;

    // Event ID constants for object-less events
    private static final int TEST_EVENT_ID = 1001;
    private static final int METRICS_EVENT_ID = 1002;
    private static final int LIFECYCLE_EVENT_ID = 1003;

    // Test payload
    private String testPayload;

    @Setup
    public void setup() {
        eventBus = EventBus.getInstance();
        eventBus.clear();

        testPayload = "test-payload-" + System.currentTimeMillis();
        testEvent = new TestEvent(testPayload);

        // Create a simple listener for object-less events
        objectLessListener = new ObjectLessEventBus.ObjectLessListener() {
            @Override
            public void onEvent(Object payload) {
                Blackhole.consumeCPU(1);
            }
        };

        // Create a simple listener for object-based events
        objectBasedListener = new EventBus.EventListener() {
            @Override
            public void onEvent(Event event) {
                Blackhole.consumeCPU(1);
            }
        };
    }

    @TearDown
    public void tearDown() {
        eventBus.clear();
    }

    // ==================== Object-Less Event Benchmarks ====================

    /**
     * Benchmark: Single object-less event publish with single listener.
     */
    @Benchmark
    public int objectLessSingleListener() {
        eventBus.register(TEST_EVENT_ID, objectLessListener);
        int delivered = eventBus.publish(TEST_EVENT_ID, testPayload);
        eventBus.unregister(TEST_EVENT_ID, objectLessListener);
        return delivered;
    }

    /**
     * Benchmark: Single object-less event publish with multiple listeners.
     */
    @Benchmark
    public int objectLessMultipleListeners() {
        ObjectLessEventBus.ObjectLessListener l1 = createListener(1);
        ObjectLessEventBus.ObjectLessListener l2 = createListener(2);
        ObjectLessEventBus.ObjectLessListener l3 = createListener(3);
        ObjectLessEventBus.ObjectLessListener l4 = createListener(4);

        eventBus.register(TEST_EVENT_ID, l1);
        eventBus.register(TEST_EVENT_ID, l2);
        eventBus.register(TEST_EVENT_ID, l3);
        eventBus.register(TEST_EVENT_ID, l4);

        int delivered = eventBus.publish(TEST_EVENT_ID, testPayload);

        eventBus.unregister(TEST_EVENT_ID, l1);
        eventBus.unregister(TEST_EVENT_ID, l2);
        eventBus.unregister(TEST_EVENT_ID, l3);
        eventBus.unregister(TEST_EVENT_ID, l4);

        return delivered;
    }

    /**
     * Benchmark: Object-less event with no listeners (null case).
     */
    @Benchmark
    public int objectLessNoListeners() {
        return eventBus.publish(TEST_EVENT_ID, testPayload);
    }

    /**
     * Benchmark: Object-less event with different event IDs.
     */
    @Benchmark
    public void objectLessDifferentEventIds() {
        eventBus.publish(TEST_EVENT_ID, testPayload);
        eventBus.publish(METRICS_EVENT_ID, testPayload);
        eventBus.publish(LIFECYCLE_EVENT_ID, testPayload);
    }

    /**
     * Benchmark: Object-less event with priority listeners.
     */
    @Benchmark
    public void objectLessPriorityListeners() {
        ObjectLessEventBus.ObjectLessListener highPriority = new ObjectLessEventBus.ObjectLessListener() {
            @Override
            public void onEvent(Object payload) {
                Blackhole.consumeCPU(1);
            }

            @Override
            public int getPriority() {
                return 100;
            }
        };

        ObjectLessEventBus.ObjectLessListener lowPriority = new ObjectLessEventBus.ObjectLessListener() {
            @Override
            public void onEvent(Object payload) {
                Blackhole.consumeCPU(1);
            }

            @Override
            public int getPriority() {
                return -100;
            }
        };

        eventBus.register(TEST_EVENT_ID, lowPriority, -100);
        eventBus.register(TEST_EVENT_ID, highPriority, 100);

        eventBus.publish(TEST_EVENT_ID, testPayload);

        eventBus.unregister(TEST_EVENT_ID, lowPriority);
        eventBus.unregister(TEST_EVENT_ID, highPriority);
    }

    // ==================== Object-Based Event Benchmarks (for comparison) ====================

    /**
     * Benchmark: Single object-based event publish with single listener.
     */
    @Benchmark
    public int objectBasedSingleListener() {
        eventBus.register(objectBasedListener, TestEvent.class);
        int delivered = eventBus.publish(testEvent);
        eventBus.unregister(objectBasedListener);
        return delivered;
    }

    /**
     * Benchmark: Single object-based event publish with multiple listeners.
     */
    @Benchmark
    public int objectBasedMultipleListeners() {
        EventBus.EventListener l1 = createObjectBasedListener(1);
        EventBus.EventListener l2 = createObjectBasedListener(2);
        EventBus.EventListener l3 = createObjectBasedListener(3);
        EventBus.EventListener l4 = createObjectBasedListener(4);

        eventBus.register(l1, TestEvent.class);
        eventBus.register(l2, TestEvent.class);
        eventBus.register(l3, TestEvent.class);
        eventBus.register(l4, TestEvent.class);

        int delivered = eventBus.publish(testEvent);

        eventBus.unregister(l1);
        eventBus.unregister(l2);
        eventBus.unregister(l3);
        eventBus.unregister(l4);

        return delivered;
    }

    /**
     * Benchmark: Object-based event with no listeners (null case).
     */
    @Benchmark
    public int objectBasedNoListeners() {
        return eventBus.publish(testEvent);
    }

    // ==================== Helper Methods ====================

    private ObjectLessEventBus.ObjectLessListener createListener(int id) {
        return new ObjectLessEventBus.ObjectLessListener() {
            @Override
            public void onEvent(Object payload) {
                Blackhole.consumeCPU(1);
            }
        };
    }

    private EventBus.EventListener createObjectBasedListener(int id) {
        return new EventBus.EventListener() {
            @Override
            public void onEvent(Event event) {
                Blackhole.consumeCPU(1);
            }
        };
    }

    /**
     * Simple test event class for object-based benchmarks.
     */
    public static class TestEvent extends Event {
        private final String data;

        public TestEvent(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }
}
