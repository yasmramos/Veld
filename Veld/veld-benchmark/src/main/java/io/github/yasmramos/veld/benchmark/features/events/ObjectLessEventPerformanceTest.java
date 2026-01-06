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

/**
 * Simple performance test for object-less events vs object-based events.
 *
 * <p>This test can be run directly as a Java program to verify the
 * performance improvements of the object-less event implementation.</p>
 *
 * <p>Usage: java ObjectLessEventPerformanceTest</p>
 *
 * @author Veld Framework Team
 */
public class ObjectLessEventPerformanceTest {

    private static final int WARMUP_ITERATIONS = 100_000;
    private static final int MEASUREMENT_ITERATIONS = 1_000_000;
    private static final int TEST_EVENT_ID = 1001;

    public static void main(String[] args) {
        System.out.println("=== Object-Less Event Performance Test ===\n");

        // Warmup
        System.out.println("Warming up...");
        warmup();

        // Run tests
        System.out.println("\nRunning performance tests...\n");

        // Test 1: Object-less single listener
        testObjectLessSingleListener();

        // Test 2: Object-based single listener
        testObjectBasedSingleListener();

        // Test 3: Object-less multiple listeners
        testObjectLessMultipleListeners();

        // Test 4: Object-based multiple listeners
        testObjectBasedMultipleListeners();

        // Test 5: Object-less no listeners
        testObjectLessNoListeners();

        // Test 6: Object-based no listeners
        testObjectBasedNoListeners();

        System.out.println("\n=== Test Complete ===");
    }

    private static void warmup() {
        EventBus eventBus = EventBus.getInstance();
        eventBus.clear();

        ObjectLessEventBus.ObjectLessListener listener = payload -> {};

        // Warmup object-less events
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            eventBus.register(TEST_EVENT_ID, listener);
            eventBus.publish(TEST_EVENT_ID, "warmup");
            eventBus.unregister(TEST_EVENT_ID, listener);
        }

        // Warmup object-based events
        EventBus.EventListener objectBasedListener = event -> {};
        TestEvent event = new TestEvent("warmup");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            eventBus.register(objectBasedListener, TestEvent.class);
            eventBus.publish(event);
            eventBus.unregister(objectBasedListener);
        }

        eventBus.clear();
    }

    private static void testObjectLessSingleListener() {
        EventBus eventBus = EventBus.getInstance();
        eventBus.clear();

        ObjectLessEventBus.ObjectLessListener listener = payload -> {
            // Simulate minimal work
            doMinimalWork();
        };

        eventBus.register(TEST_EVENT_ID, listener);

        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            eventBus.publish(TEST_EVENT_ID, "test");
        }
        long endTime = System.nanoTime();

        double avgTime = (endTime - startTime) / (double) MEASUREMENT_ITERATIONS;

        eventBus.unregister(TEST_EVENT_ID, listener);

        System.out.printf("Object-Less Single Listener:  %.2f ns/op%n", avgTime);
    }

    private static void testObjectBasedSingleListener() {
        EventBus eventBus = EventBus.getInstance();
        eventBus.clear();

        EventBus.EventListener listener = event -> {
            doMinimalWork();
        };

        eventBus.register(listener, TestEvent.class);

        TestEvent event = new TestEvent("test");

        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            eventBus.publish(event);
        }
        long endTime = System.nanoTime();

        double avgTime = (endTime - startTime) / (double) MEASUREMENT_ITERATIONS;

        eventBus.unregister(listener);

        System.out.printf("Object-Based Single Listener: %.2f ns/op%n", avgTime);
    }

    private static void testObjectLessMultipleListeners() {
        EventBus eventBus = EventBus.getInstance();
        eventBus.clear();

        ObjectLessEventBus.ObjectLessListener l1 = payload -> doMinimalWork();
        ObjectLessEventBus.ObjectLessListener l2 = payload -> doMinimalWork();
        ObjectLessEventBus.ObjectLessListener l3 = payload -> doMinimalWork();
        ObjectLessEventBus.ObjectLessListener l4 = payload -> doMinimalWork();

        eventBus.register(TEST_EVENT_ID, l1);
        eventBus.register(TEST_EVENT_ID, l2);
        eventBus.register(TEST_EVENT_ID, l3);
        eventBus.register(TEST_EVENT_ID, l4);

        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            eventBus.publish(TEST_EVENT_ID, "test");
        }
        long endTime = System.nanoTime();

        double avgTime = (endTime - startTime) / (double) MEASUREMENT_ITERATIONS;

        eventBus.unregister(TEST_EVENT_ID, l1);
        eventBus.unregister(TEST_EVENT_ID, l2);
        eventBus.unregister(TEST_EVENT_ID, l3);
        eventBus.unregister(TEST_EVENT_ID, l4);

        System.out.printf("Object-Less 4 Listeners:       %.2f ns/op%n", avgTime);
    }

    private static void testObjectBasedMultipleListeners() {
        EventBus eventBus = EventBus.getInstance();
        eventBus.clear();

        EventBus.EventListener l1 = event -> doMinimalWork();
        EventBus.EventListener l2 = event -> doMinimalWork();
        EventBus.EventListener l3 = event -> doMinimalWork();
        EventBus.EventListener l4 = event -> doMinimalWork();

        eventBus.register(l1, TestEvent.class);
        eventBus.register(l2, TestEvent.class);
        eventBus.register(l3, TestEvent.class);
        eventBus.register(l4, TestEvent.class);

        TestEvent event = new TestEvent("test");

        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            eventBus.publish(event);
        }
        long endTime = System.nanoTime();

        double avgTime = (endTime - startTime) / (double) MEASUREMENT_ITERATIONS;

        eventBus.unregister(l1);
        eventBus.unregister(l2);
        eventBus.unregister(l3);
        eventBus.unregister(l4);

        System.out.printf("Object-Based 4 Listeners:      %.2f ns/op%n", avgTime);
    }

    private static void testObjectLessNoListeners() {
        EventBus eventBus = EventBus.getInstance();
        eventBus.clear();

        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            eventBus.publish(TEST_EVENT_ID, "test");
        }
        long endTime = System.nanoTime();

        double avgTime = (endTime - startTime) / (double) MEASUREMENT_ITERATIONS;

        System.out.printf("Object-Less No Listeners:      %.2f ns/op%n", avgTime);
    }

    private static void testObjectBasedNoListeners() {
        EventBus eventBus = EventBus.getInstance();
        eventBus.clear();

        TestEvent event = new TestEvent("test");

        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            eventBus.publish(event);
        }
        long endTime = System.nanoTime();

        double avgTime = (endTime - startTime) / (double) MEASUREMENT_ITERATIONS;

        System.out.printf("Object-Based No Listeners:     %.2f ns/op%n", avgTime);
    }

    private static void doMinimalWork() {
        // Prevent dead code elimination
        int result = 0;
        for (int j = 0; j < 3; j++) {
            result += j;
        }
        if (result < 0) {
            throw new IllegalStateException("Should not happen");
        }
    }

    /**
     * Simple test event class for object-based tests.
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
