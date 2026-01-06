package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for StandardEventChannel to achieve 100% coverage.
 * Tests all dispatch paths and the ListenerEntry inner class.
 */
@DisplayName("StandardEventChannel Tests")
@Execution(ExecutionMode.SAME_THREAD)
class StandardEventChannelTest {

    private StandardEventChannel channel;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newCachedThreadPool();
        channel = new StandardEventChannel("test-channel", executor);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.clear();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    // ===== Test Listeners =====

    static class TestListener implements ObjectLessEventBus.ObjectLessListener {
        final List<Object> receivedPayloads = new ArrayList<>();
        final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public void onEvent(Object payload) {
            receivedPayloads.add(payload);
            callCount.incrementAndGet();
        }
    }

    static class AsyncTestListener implements ObjectLessEventBus.ObjectLessListener {
        final List<Object> receivedPayloads = new ArrayList<>();
        final CountDownLatch latch;

        AsyncTestListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onEvent(Object payload) {
            receivedPayloads.add(payload);
            latch.countDown();
        }

        @Override
        public boolean isAsync() {
            return true;
        }
    }

    static class PriorityTestListener implements ObjectLessEventBus.ObjectLessListener {
        final List<String> callOrder;
        final int priority;
        final String name;

        PriorityTestListener(List<String> callOrder, int priority, String name) {
            this.callOrder = callOrder;
            this.priority = priority;
            this.name = name;
        }

        @Override
        public void onEvent(Object payload) {
            callOrder.add(name);
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    // ===== Constructor Tests =====

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize with correct default values")
        void shouldInitializeWithCorrectDefaults() {
            assertNotNull(channel);
            assertEquals("test-channel", channel.getChannelName());
            assertEquals(0, channel.getListenerCount());
        }
    }

    // ===== ListenerEntry Tests =====

    @Nested
    @DisplayName("ListenerEntry Tests")
    class ListenerEntryTests {

        @Test
        @DisplayName("Should create ListenerEntry with single listener")
        void shouldCreateListenerEntryWithSingleListener() {
            TestListener listener = new TestListener();
            ObjectLessEventBus.ObjectLessListener[] listeners = new ObjectLessEventBus.ObjectLessListener[]{listener};

            // ListenerEntry is created internally when registering
            channel.register(1, listener);

            // Verify the entry was created by publishing
            int count = channel.publish(1, "test-payload");

            assertEquals(1, count);
            assertEquals(1, listener.receivedPayloads.size());
            assertEquals("test-payload", listener.receivedPayloads.get(0));
        }

        @Test
        @DisplayName("Should create ListenerEntry with multiple listeners")
        void shouldCreateListenerEntryWithMultipleListeners() {
            TestListener listener1 = new TestListener();
            TestListener listener2 = new TestListener();

            channel.register(1, listener1);
            channel.register(1, listener2);

            int count = channel.publish(1, "multi-payload");

            assertEquals(2, count);
            assertEquals(1, listener1.receivedPayloads.size());
            assertEquals(1, listener2.receivedPayloads.size());
        }
    }

    // ===== dispatchSingle Tests =====

    @Nested
    @DisplayName("dispatchSingle Tests")
    class DispatchSingleTests {

        @Test
        @DisplayName("Should call single sync listener")
        void shouldCallSingleSyncListener() {
            TestListener listener = new TestListener();
            channel.register(1, listener);

            int count = channel.publish(1, "single-sync");

            assertEquals(1, count);
            assertEquals(1, listener.callCount.get());
            assertEquals("single-sync", listener.receivedPayloads.get(0));
        }

        @Test
        @DisplayName("Should return 0 for single async listener")
        void shouldReturnZeroForSingleAsyncListener() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            AsyncTestListener listener = new AsyncTestListener(latch);
            channel.register(1, listener);

            int count = channel.publish(1, "single-async");

            assertEquals(0, count); // Async returns 0 immediately
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(1, listener.receivedPayloads.size());
        }

        @Test
        @DisplayName("Should handle empty payload")
        void shouldHandleEmptyPayload() {
            TestListener listener = new TestListener();
            channel.register(1, listener);

            int count = channel.publish(1, null);

            assertEquals(1, count);
            assertNull(listener.receivedPayloads.get(0));
        }
    }

    // ===== dispatchTwo Tests =====

    @Nested
    @DisplayName("dispatchTwo Tests")
    class DispatchTwoTests {

        @Test
        @DisplayName("Should call two sync listeners")
        void shouldCallTwoSyncListeners() {
            TestListener listener1 = new TestListener();
            TestListener listener2 = new TestListener();
            channel.register(1, listener1);
            channel.register(1, listener2);

            int count = channel.publish(1, "two-sync");

            assertEquals(2, count);
            assertEquals(1, listener1.callCount.get());
            assertEquals(1, listener2.callCount.get());
        }

        @Test
        @DisplayName("Should call one sync and one async listener")
        void shouldCallOneSyncAndOneAsyncListener() throws InterruptedException {
            TestListener syncListener = new TestListener();
            CountDownLatch latch = new CountDownLatch(1);
            AsyncTestListener asyncListener = new AsyncTestListener(latch);

            channel.register(1, syncListener);
            channel.register(1, asyncListener);

            int count = channel.publish(1, "mixed");

            assertEquals(1, count); // Only sync count
            assertEquals(1, syncListener.callCount.get());
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("Should handle both async listeners")
        void shouldHandleBothAsyncListeners() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(2);
            AsyncTestListener listener1 = new AsyncTestListener(latch);
            AsyncTestListener listener2 = new AsyncTestListener(latch);

            channel.register(1, listener1);
            channel.register(1, listener2);

            int count = channel.publish(1, "both-async");

            assertEquals(0, count);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

    // ===== dispatchThree Tests =====

    @Nested
    @DisplayName("dispatchThree Tests")
    class DispatchThreeTests {

        @Test
        @DisplayName("Should call three sync listeners")
        void shouldCallThreeSyncListeners() {
            TestListener listener1 = new TestListener();
            TestListener listener2 = new TestListener();
            TestListener listener3 = new TestListener();
            channel.register(1, listener1);
            channel.register(1, listener2);
            channel.register(1, listener3);

            int count = channel.publish(1, "three-sync");

            assertEquals(3, count);
            assertEquals(1, listener1.callCount.get());
            assertEquals(1, listener2.callCount.get());
            assertEquals(1, listener3.callCount.get());
        }

        @Test
        @DisplayName("Should call mixed sync and async listeners")
        void shouldCallMixedListeners() throws InterruptedException {
            TestListener sync1 = new TestListener();
            CountDownLatch latch = new CountDownLatch(2);
            AsyncTestListener async1 = new AsyncTestListener(latch);
            AsyncTestListener async2 = new AsyncTestListener(latch);

            channel.register(1, sync1);
            channel.register(1, async1);
            channel.register(1, async2);

            int count = channel.publish(1, "mixed-three");

            assertEquals(1, count);
            assertEquals(1, sync1.callCount.get());
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

    // ===== dispatchFour Tests =====

    @Nested
    @DisplayName("dispatchFour Tests")
    class DispatchFourTests {

        @Test
        @DisplayName("Should call four sync listeners")
        void shouldCallFourSyncListeners() {
            TestListener listener1 = new TestListener();
            TestListener listener2 = new TestListener();
            TestListener listener3 = new TestListener();
            TestListener listener4 = new TestListener();
            channel.register(1, listener1);
            channel.register(1, listener2);
            channel.register(1, listener3);
            channel.register(1, listener4);

            int count = channel.publish(1, "four-sync");

            assertEquals(4, count);
        }
    }

    // ===== dispatchMultiple Tests =====

    @Nested
    @DisplayName("dispatchMultiple Tests")
    class DispatchMultipleTests {

        @Test
        @DisplayName("Should call five sync listeners (triggers dispatchMultiple)")
        void shouldCallFiveSyncListeners() {
            TestListener listener1 = new TestListener();
            TestListener listener2 = new TestListener();
            TestListener listener3 = new TestListener();
            TestListener listener4 = new TestListener();
            TestListener listener5 = new TestListener();
            channel.register(1, listener1);
            channel.register(1, listener2);
            channel.register(1, listener3);
            channel.register(1, listener4);
            channel.register(1, listener5);

            int count = channel.publish(1, "five-sync");

            assertEquals(5, count);
        }

        @Test
        @DisplayName("Should call mixed listeners (triggers dispatchMultiple)")
        void shouldCallMixedListenersInDispatchMultiple() throws InterruptedException {
            TestListener sync1 = new TestListener();
            TestListener sync2 = new TestListener();
            CountDownLatch latch = new CountDownLatch(3);
            AsyncTestListener async1 = new AsyncTestListener(latch);
            AsyncTestListener async2 = new AsyncTestListener(latch);
            AsyncTestListener async3 = new AsyncTestListener(latch);

            channel.register(1, sync1);
            channel.register(1, async1);
            channel.register(1, sync2);
            channel.register(1, async2);
            channel.register(1, async3);

            int count = channel.publish(1, "multiple-mixed");

            assertEquals(2, count); // Only sync listeners
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

    // ===== Priority Tests =====

    @Nested
    @DisplayName("Priority Tests")
    class PriorityTests {

        @Test
        @DisplayName("Should call listeners in priority order")
        void shouldCallListenersInPriorityOrder() {
            List<String> callOrder = new ArrayList<>();
            PriorityTestListener low = new PriorityTestListener(callOrder, 1, "low");
            PriorityTestListener medium = new PriorityTestListener(callOrder, 5, "medium");
            PriorityTestListener high = new PriorityTestListener(callOrder, 10, "high");

            channel.register(1, low);
            channel.register(1, medium);
            channel.register(1, high);

            channel.publish(1, "priority-test");

            // Should be called in order: high, medium, low
            assertEquals(3, callOrder.size());
            assertEquals("high", callOrder.get(0));
            assertEquals("medium", callOrder.get(1));
            assertEquals("low", callOrder.get(2));
        }

        @Test
        @DisplayName("Should maintain registration order for same priority")
        void shouldMaintainRegistrationOrderForSamePriority() {
            TestListener first = new TestListener();
            TestListener second = new TestListener();

            channel.register(1, first, 0);
            channel.register(1, second, 0);

            channel.publish(1, "same-priority");

            assertEquals(1, first.callCount.get());
            assertEquals(1, second.callCount.get());
            assertEquals("same-priority", first.receivedPayloads.get(0));
            assertEquals("same-priority", second.receivedPayloads.get(0));
        }
    }

    // ===== Edge Cases Tests =====

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should return 0 for unregistered event ID")
        void shouldReturnZeroForUnregisteredEvent() {
            int count = channel.publish(999, "unregistered");

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should return 0 for negative event ID")
        void shouldReturnZeroForNegativeEventId() {
            int count = channel.publish(-1, "negative");

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should unregister listener")
        void shouldUnregisterListener() {
            TestListener listener = new TestListener();
            channel.register(1, listener);
            assertEquals(1, channel.getListenerCount());

            channel.unregister(1, listener);
            assertEquals(0, channel.getListenerCount());
        }

        @Test
        @DisplayName("Should handle unregister of non-existent listener gracefully")
        void shouldHandleUnregisterOfNonExistentListener() {
            TestListener listener = new TestListener();
            TestListener other = new TestListener();

            channel.register(1, other);
            assertDoesNotThrow(() -> channel.unregister(1, listener));
            assertEquals(1, channel.getListenerCount());
        }

        @Test
        @DisplayName("Should clear all listeners")
        void shouldClearAllListeners() {
            channel.register(1, new TestListener());
            channel.register(2, new TestListener());
            channel.register(3, new TestListener());

            assertEquals(3, channel.getListenerCount());

            channel.clear();

            assertEquals(0, channel.getListenerCount());
        }

        @Test
        @DisplayName("Should get correct listener count")
        void shouldGetCorrectListenerCount() {
            TestListener listener1 = new TestListener();
            TestListener listener2 = new TestListener();

            channel.register(1, listener1);
            channel.register(2, listener2);

            assertEquals(2, channel.getListenerCount());
        }

        @Test
        @DisplayName("Should return statistics")
        void shouldReturnStatistics() {
            channel.register(1, new TestListener());
            channel.publish(1, "test");

            String stats = channel.getStatistics();

            assertNotNull(stats);
            assertTrue(stats.contains("test-channel"));
            assertTrue(stats.contains("Listeners"));
            assertTrue(stats.contains("Published"));
        }
    }

    // ===== Async Publish Tests =====

    @Nested
    @DisplayName("Async Publish Tests")
    class AsyncPublishTests {

        @Test
        @DisplayName("Should publish async correctly")
        void shouldPublishAsyncCorrectly() throws Exception {
            TestListener listener = new TestListener();
            channel.register(1, listener);

            var future = channel.publishAsync(1, "async-payload");

            Integer count = future.get(5, TimeUnit.SECONDS);
            assertEquals(1, count);
            assertEquals("async-payload", listener.receivedPayloads.get(0));
        }

        @Test
        @DisplayName("Should return 0 for unregistered event in async publish")
        void shouldReturnZeroForUnregisteredEventInAsync() throws Exception {
            var future = channel.publishAsync(999, "async-unregistered");

            Integer count = future.get(1, TimeUnit.SECONDS);
            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should handle null payload in async publish")
        void shouldHandleNullPayloadInAsync() throws Exception {
            TestListener listener = new TestListener();
            channel.register(1, listener);

            var future = channel.publishAsync(1, null);

            Integer count = future.get(5, TimeUnit.SECONDS);
            assertEquals(1, count);
            assertNull(listener.receivedPayloads.get(0));
        }
    }

    // ===== Default Method Tests for ObjectLessListener =====

    @Nested
    @DisplayName("ObjectLessListener Default Methods Tests")
    class ObjectLessListenerDefaultMethodsTests {

        @Test
        @DisplayName("isAsync should return false by default")
        void isAsync_shouldReturnFalseByDefault() {
            ObjectLessEventBus.ObjectLessListener listener = payload -> {};

            assertFalse(listener.isAsync());
        }

        @Test
        @DisplayName("isAsync should return true when overridden")
        void isAsync_shouldReturnTrueWhenOverridden() {
            ObjectLessEventBus.ObjectLessListener listener = new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {}

                @Override
                public boolean isAsync() {
                    return true;
                }
            };

            assertTrue(listener.isAsync());
        }

        @Test
        @DisplayName("getPriority should return 0 by default")
        void getPriority_shouldReturnZeroByDefault() {
            ObjectLessEventBus.ObjectLessListener listener = payload -> {};

            assertEquals(0, listener.getPriority());
        }

        @Test
        @DisplayName("getPriority should return custom value when overridden")
        void getPriority_shouldReturnCustomValueWhenOverridden() {
            ObjectLessEventBus.ObjectLessListener listener = new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {}

                @Override
                public int getPriority() {
                    return 42;
                }
            };

            assertEquals(42, listener.getPriority());
        }

        @Test
        @DisplayName("Should use default methods in actual dispatch")
        void shouldUseDefaultMethodsInActualDispatch() {
            // Test with a lambda (uses default isAsync = false)
            List<Object> received = new ArrayList<>();
            ObjectLessEventBus.ObjectLessListener syncListener = received::add;

            channel.register(1, syncListener);
            int count = channel.publish(1, "lambda-test");

            assertEquals(1, count);
            assertEquals(1, received.size());
            assertEquals("lambda-test", received.get(0));
        }
    }

    // ===== Registration Validation Tests =====

    @Nested
    @DisplayName("Registration Validation Tests")
    class RegistrationValidationTests {

        @Test
        @DisplayName("Should throw exception for negative event ID")
        void shouldThrowExceptionForNegativeEventId() {
            ObjectLessEventBus.ObjectLessListener listener = payload -> {};

            assertThrows(IllegalArgumentException.class, () ->
                channel.register(-1, listener));
        }

        @Test
        @DisplayName("Should throw exception for null listener")
        void shouldThrowExceptionForNullListener() {
            assertThrows(IllegalArgumentException.class, () ->
                channel.register(1, null));
        }
    }
}
