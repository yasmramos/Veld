package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests specifically designed to cover remaining coverage gaps.
 * Focuses on public APIs only.
 */
@DisplayName("EventBus Public API Coverage Tests")
@Execution(ExecutionMode.SAME_THREAD)
class EventBusPublicAPICoverageTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.getInstance();
        eventBus.clear();
    }

    @AfterEach
    void tearDown() {
        eventBus.resetForTesting();
    }

    // ========== EventFilter Expression Tests ==========

    @Nested
    @DisplayName("EventFilter Expression Tests")
    class EventFilterExpressionTests {

        // Test event with various property types
        static class TestEvent extends Event {
            private final String type;
            private final int priority;
            private final double amount;
            private final boolean active;

            TestEvent(String type, int priority, double amount, boolean active) {
                super();
                this.type = type;
                this.priority = priority;
                this.amount = amount;
                this.active = active;
            }

            public String getType() { return type; }
            public int getPriority() { return priority; }
            public double getAmount() { return amount; }
            public boolean isActive() { return active; }
        }

        @Test
        @DisplayName("Should compare event properties in filter")
        void shouldCompareEventPropertiesInFilter() {
            // Test number comparison using event.propertyName format
            TestEvent intEvent = new TestEvent("TEST", 42, 100.0, true);
            assertTrue(EventFilter.evaluate("event.priority == 42", intEvent));
            assertFalse(EventFilter.evaluate("event.priority == 99", intEvent));

            // Test string comparison
            TestEvent strEvent = new TestEvent("hello", 5, 100.0, true);
            assertTrue(EventFilter.evaluate("event.type == 'hello'", strEvent));
            assertFalse(EventFilter.evaluate("event.type == 'world'", strEvent));

            // Test boolean comparison
            TestEvent boolEvent = new TestEvent("TEST", 5, 100.0, true);
            assertTrue(EventFilter.evaluate("event.active == true", boolEvent));
        }

        @Test
        @DisplayName("Should handle complex comparison expressions")
        void shouldHandleComplexComparisonExpressions() {
            TestEvent event = new TestEvent("TEST", 100, 150.0, true);

            assertTrue(EventFilter.evaluate("event.priority > 50", event));
            assertTrue(EventFilter.evaluate("event.priority >= 100", event));
            assertTrue(EventFilter.evaluate("event.priority < 200", event));
            assertTrue(EventFilter.evaluate("event.priority <= 100", event));
            assertTrue(EventFilter.evaluate("event.priority != 0", event));
        }

        @Test
        @DisplayName("Should handle null property value")
        void shouldHandleNullPropertyValue() {
            TestEvent event = new TestEvent(null, 5, 100.0, true);

            // Null property comparison should return false for equality
            assertFalse(EventFilter.evaluate("event.type == 'test'", event));
        }

        @Test
        @DisplayName("Should handle numeric property in filter")
        void shouldHandleNumericPropertyInFilter() {
            TestEvent event = new TestEvent("PREMIUM", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.priority > 3", event));
            assertTrue(EventFilter.evaluate("event.priority == 5", event));
            assertFalse(EventFilter.evaluate("event.priority > 10", event));
        }

        @Test
        @DisplayName("Should handle double property in filter")
        void shouldHandleDoublePropertyInFilter() {
            TestEvent event = new TestEvent("TEST", 5, 150.50, true);

            assertTrue(EventFilter.evaluate("event.amount > 100.0", event));
            assertTrue(EventFilter.evaluate("event.amount == 150.50", event));
        }

        @Test
        @DisplayName("Should handle boolean property in filter")
        void shouldHandleBooleanPropertyInFilter() {
            TestEvent trueEvent = new TestEvent("TEST", 5, 100.0, true);
            TestEvent falseEvent = new TestEvent("TEST", 5, 100.0, false);

            assertTrue(EventFilter.evaluate("event.active == true", trueEvent));
            assertTrue(EventFilter.evaluate("event.active == false", falseEvent));
        }

        @Test
        @DisplayName("Should handle single comparison expression")
        void shouldHandleSingleComparisonExpression() {
            TestEvent event = new TestEvent("PREMIUM", 10, 200.0, true);

            // Single AND condition
            assertTrue(EventFilter.evaluate("event.type == 'PREMIUM'", event));
            assertFalse(EventFilter.evaluate("event.type == 'STANDARD'", event));
        }

        @Test
        @DisplayName("Should handle empty and null expressions")
        void shouldHandleEmptyAndNullExpressions() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            // Empty/null expressions should return true (accept all)
            assertTrue(EventFilter.evaluate(null, event));
            assertTrue(EventFilter.evaluate("", event));
            assertTrue(EventFilter.evaluate("   ", event));
        }
    }

    // ========== EventBus Listener Registration Tests ==========

    @Nested
    @DisplayName("EventBus Listener Registration Tests")
    class ListenerRegistrationTests {

        @Test
        @DisplayName("Should register listener with priority")
        void shouldRegisterListenerWithPriority() {
            AtomicInteger count = new AtomicInteger(0);
            int eventId = 200;

            // Register with higher priority first
            eventBus.register(eventId, payload -> count.addAndGet(10), 10);
            eventBus.register(eventId, payload -> count.addAndGet(1), 1);

            eventBus.publish(eventId, "test");

            // Both should be called (priority affects order, not filtering)
            assertEquals(11, count.get());
        }

        @Test
        @DisplayName("Should register multiple listeners for same event")
        void shouldRegisterMultipleListenersForSameEvent() {
            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);
            int eventId = 201;

            eventBus.register(eventId, payload -> count1.incrementAndGet());
            eventBus.register(eventId, payload -> count2.incrementAndGet());

            eventBus.publish(eventId, "test");

            assertEquals(1, count1.get());
            assertEquals(1, count2.get());
        }

        @Test
        @DisplayName("Should unregister listener correctly")
        void shouldUnregisterListenerCorrectly() {
            AtomicInteger count = new AtomicInteger(0);
            int eventId = 202;
            ObjectLessEventBus.ObjectLessListener listener = payload -> count.incrementAndGet();

            eventBus.register(eventId, listener);
            eventBus.publish(eventId, "test");
            assertEquals(1, count.get());

            eventBus.unregister(eventId, listener);
            eventBus.publish(eventId, "test");
            assertEquals(1, count.get()); // Should still be 1
        }
    }

    // ========== EventBus Lifecycle Tests ==========

    @Nested
    @DisplayName("EventBus Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Should handle clear when empty")
        void shouldHandleClearWhenEmpty() {
            assertDoesNotThrow(() -> eventBus.clear());
            assertEquals(0, eventBus.getSubscriberCount());
        }

        @Test
        @DisplayName("Should handle statistics with empty bus")
        void shouldHandleStatisticsWithEmptyBus() {
            String stats = eventBus.getStatistics();

            assertNotNull(stats);
            assertTrue(stats.contains("EventBus Statistics"));
        }

        @Test
        @DisplayName("Should handle statistics after publishing")
        void shouldHandleStatisticsAfterPublishing() {
            eventBus.register(999, payload -> {});

            eventBus.publish(999, "test");
            String stats = eventBus.getStatistics();

            assertNotNull(stats);
        }

        @Test
        @DisplayName("Should handle shutdown gracefully")
        void shouldHandleShutdownGracefully() {
            // This tests that shutdown can be called without errors
            assertDoesNotThrow(() -> eventBus.shutdown());
        }

        @Test
        @DisplayName("Should return correct listener count")
        void shouldReturnCorrectListenerCount() {
            assertEquals(0, eventBus.getListenerCount());

            eventBus.register(100, payload -> {});
            eventBus.register(100, payload -> {});

            assertEquals(2, eventBus.getListenerCount());
        }
    }

    // ========== Dispatch Path Tests ==========

    @Nested
    @DisplayName("Dispatch Path Tests")
    class DispatchPathTests {

        @Test
        @DisplayName("Should dispatch to multiple listeners")
        void shouldDispatchToMultipleListeners() {
            AtomicInteger count = new AtomicInteger(0);
            int eventId = 300;

            eventBus.register(eventId, payload -> count.incrementAndGet());
            eventBus.register(eventId, payload -> count.incrementAndGet());
            eventBus.register(eventId, payload -> count.incrementAndGet());

            eventBus.publish(eventId, "test");

            assertEquals(3, count.get());
        }

        @Test
        @DisplayName("Should handle publish to large number of listeners")
        void shouldHandlePublishToLargeNumberOfListeners() {
            AtomicInteger count = new AtomicInteger(0);
            int eventId = 302;

            // Register 10 listeners
            for (int i = 0; i < 10; i++) {
                eventBus.register(eventId, payload -> count.incrementAndGet());
            }

            eventBus.publish(eventId, "test");

            assertEquals(10, count.get());
        }

        @Test
        @DisplayName("Should handle mixed sync and async listeners")
        void shouldHandleMixedSyncAndAsyncListeners() throws Exception {
            AtomicInteger syncCount = new AtomicInteger(0);
            CountDownLatch asyncLatch = new CountDownLatch(1);
            AtomicBoolean asyncCalled = new AtomicBoolean(false);
            int eventId = 303;

            eventBus.register(eventId, payload -> syncCount.incrementAndGet());
            eventBus.register(eventId, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    asyncCalled.set(true);
                    asyncLatch.countDown();
                }

                @Override
                public boolean isAsync() {
                    return true;
                }
            });

            eventBus.publish(eventId, "test");

            assertEquals(1, syncCount.get());
            assertTrue(asyncLatch.await(1, TimeUnit.SECONDS));
            assertTrue(asyncCalled.get());
        }
    }

    // ========== Edge Cases Tests ==========

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle publish with unregistered event ID")
        void shouldHandlePublishWithUnregisteredEventId() {
            int count = eventBus.publish(99999, "test");

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should handle publish with negative event ID")
        void shouldHandlePublishWithNegativeEventId() {
            // Negative event IDs should be handled gracefully
            // The behavior may vary - either throw exception or return 0
            try {
                int count = eventBus.publish(-1, "test");
                // If it doesn't throw, verify it returns 0 for safety
                assertEquals(0, count);
            } catch (IllegalArgumentException e) {
                // This is also valid behavior
                assertTrue(e.getMessage().contains("negative") || e.getMessage().contains("Event ID"));
            }
        }

        @Test
        @DisplayName("Should handle unregister of non-existent listener")
        void shouldHandleUnregisterOfNonExistentListener() {
            ObjectLessEventBus.ObjectLessListener listener = payload -> {};

            // Should not throw
            assertDoesNotThrow(() -> eventBus.unregister(100, listener));
        }

        @Test
        @DisplayName("Should handle null payload")
        void shouldHandleNullPayload() {
            AtomicBoolean called = new AtomicBoolean(false);
            int eventId = 400;

            eventBus.register(eventId, payload -> {
                called.set(true);
                assertNull(payload);
            });

            eventBus.publish(eventId, null);

            assertTrue(called.get());
        }

        @Test
        @DisplayName("Should handle various payload types")
        void shouldHandleVariousPayloadTypes() {
            AtomicInteger intCount = new AtomicInteger(0);
            AtomicInteger strCount = new AtomicInteger(0);
            AtomicInteger listCount = new AtomicInteger(0);
            int eventId = 401;

            eventBus.register(eventId, payload -> {
                if (payload instanceof Integer) intCount.incrementAndGet();
                else if (payload instanceof String) strCount.incrementAndGet();
                else if (payload instanceof List) listCount.incrementAndGet();
            });

            eventBus.publish(eventId, 42);
            eventBus.publish(eventId, "hello");
            eventBus.publish(eventId, new ArrayList<>());

            assertEquals(1, intCount.get());
            assertEquals(1, strCount.get());
            assertEquals(1, listCount.get());
        }

        @Test
        @DisplayName("Should handle rapid publish/unregister")
        void shouldHandleRapidPublishUnregister() {
            ObjectLessEventBus.ObjectLessListener listener = payload -> {};
            int eventId = 402;

            eventBus.register(eventId, listener);
            eventBus.publish(eventId, "test1");

            eventBus.unregister(eventId, listener);
            int count = eventBus.publish(eventId, "test2");

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should handle async publish returning correct count")
        void shouldHandleAsyncPublishReturningCorrectCount() throws Exception {
            AtomicInteger count = new AtomicInteger(0);
            int eventId = 403;

            for (int i = 0; i < 5; i++) {
                eventBus.register(eventId, payload -> count.incrementAndGet());
            }

            var future = eventBus.publishAsync(eventId, "test");
            Integer result = future.get(2, TimeUnit.SECONDS);

            assertEquals(5, result.intValue());
            assertEquals(5, count.get());
        }
    }

    // ========== EventChannel Tests ==========

    @Nested
    @DisplayName("EventChannel Tests")
    class EventChannelTests {

        @Test
        @DisplayName("Should get specialized channel")
        void shouldGetSpecializedChannel() {
            EventChannel channel1 = eventBus.getChannel("test-channel");
            EventChannel channel2 = eventBus.getChannel("test-channel");

            assertSame(channel1, channel2);
        }

        @Test
        @DisplayName("Should isolate listeners across channels")
        void shouldIsolateListenersAcrossChannels() {
            EventChannel channel1 = eventBus.getChannel("channel-1");
            EventChannel channel2 = eventBus.getChannel("channel-2");
            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            channel1.register(100, payload -> count1.incrementAndGet());
            channel2.register(100, payload -> count2.incrementAndGet());

            channel1.publish(100, "test");

            assertEquals(1, count1.get());
            assertEquals(0, count2.get());
        }

        @Test
        @DisplayName("Should return correct channel listener count")
        void shouldReturnCorrectChannelListenerCount() {
            EventChannel channel = eventBus.getChannel("stats");

            assertEquals(0, channel.getListenerCount());

            channel.register(100, payload -> {});
            channel.register(100, payload -> {});

            assertEquals(2, channel.getListenerCount());
        }
    }
}
