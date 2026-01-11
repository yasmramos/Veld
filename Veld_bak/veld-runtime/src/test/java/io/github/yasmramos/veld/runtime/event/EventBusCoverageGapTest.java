package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests specifically designed to cover the remaining coverage gaps in EventBus.
 * Targets dispatch methods with 2, 3, 4, 5 listeners and other low-coverage branches.
 */
@DisplayName("EventBus Coverage Gap Tests")
@Execution(ExecutionMode.SAME_THREAD)
class EventBusCoverageGapTest {

    private EventBus eventBus;
    private EventChannel channel;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.getInstance();
        eventBus.clear();
        channel = eventBus.getStandardChannel();
    }

    // ========== Five Listener Dispatch Tests ==========

    @Nested
    @DisplayName("Five Listener Dispatch Tests")
    class FiveListenerDispatchTests {

        @Test
        @DisplayName("Should dispatch to 5 listeners")
        void shouldDispatchToFiveListeners() {
            AtomicInteger count = new AtomicInteger(0);

            for (int i = 0; i < 5; i++) {
                channel.register(100, payload -> count.incrementAndGet());
            }

            channel.publish(100, "test");

            assertEquals(5, count.get());
        }

        @Test
        @DisplayName("Should dispatch to 5 listeners with priority order")
        void shouldDispatchToFiveListenersWithPriorityOrder() {
            List<String> callOrder = new ArrayList<>();

            // Register with different priorities (implemented via anonymous classes)
            channel.register(101, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("p1");
                }

                @Override
                public int getPriority() {
                    return 1;
                }
            });

            channel.register(101, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("p3");
                }

                @Override
                public int getPriority() {
                    return 3;
                }
            });

            channel.register(101, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("p5");
                }

                @Override
                public int getPriority() {
                    return 5;
                }
            });

            channel.register(101, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("p2");
                }

                @Override
                public int getPriority() {
                    return 2;
                }
            });

            channel.register(101, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("p4");
                }

                @Override
                public int getPriority() {
                    return 4;
                }
            });

            channel.publish(101, "test");

            assertEquals(5, callOrder.size());
            // Verify priority order (5, 4, 3, 2, 1)
            assertEquals("p5", callOrder.get(0));
            assertEquals("p4", callOrder.get(1));
            assertEquals("p3", callOrder.get(2));
            assertEquals("p2", callOrder.get(3));
            assertEquals("p1", callOrder.get(4));
        }
    }

    // ========== Four Listener Dispatch Tests ==========

    @Nested
    @DisplayName("Four Listener Dispatch Tests")
    class FourListenerDispatchTests {

        @Test
        @DisplayName("Should dispatch to 4 listeners")
        void shouldDispatchToFourListeners() {
            AtomicInteger count = new AtomicInteger(0);

            for (int i = 0; i < 4; i++) {
                channel.register(200, payload -> count.incrementAndGet());
            }

            channel.publish(200, "test");

            assertEquals(4, count.get());
        }

        @Test
        @DisplayName("Should dispatch to 4 listeners with priority")
        void shouldDispatchToFourListenersWithPriority() {
            List<String> callOrder = new ArrayList<>();

            channel.register(201, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("p1");
                }

                @Override
                public int getPriority() {
                    return 1;
                }
            });

            channel.register(201, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("p4");
                }

                @Override
                public int getPriority() {
                    return 4;
                }
            });

            channel.register(201, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("p2");
                }

                @Override
                public int getPriority() {
                    return 2;
                }
            });

            channel.register(201, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("p3");
                }

                @Override
                public int getPriority() {
                    return 3;
                }
            });

            channel.publish(201, "test");

            assertEquals(4, callOrder.size());
            assertEquals("p4", callOrder.get(0));
            assertEquals("p3", callOrder.get(1));
            assertEquals("p2", callOrder.get(2));
            assertEquals("p1", callOrder.get(3));
        }
    }

    // ========== Three Listener Dispatch Tests ==========

    @Nested
    @DisplayName("Three Listener Dispatch Tests")
    class ThreeListenerDispatchTests {

        @Test
        @DisplayName("Should dispatch to 3 listeners")
        void shouldDispatchToThreeListeners() {
            List<String> received = new ArrayList<>();

            channel.register(300, payload -> received.add("1"));
            channel.register(300, payload -> received.add("2"));
            channel.register(300, payload -> received.add("3"));

            channel.publish(300, "test");

            assertEquals(3, received.size());
        }

        @Test
        @DisplayName("Should dispatch to 3 listeners with different priorities")
        void shouldDispatchToThreeListenersWithDifferentPriorities() {
            List<String> callOrder = new ArrayList<>();

            channel.register(301, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("low");
                }

                @Override
                public int getPriority() {
                    return 1;
                }
            });

            channel.register(301, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("high");
                }

                @Override
                public int getPriority() {
                    return 10;
                }
            });

            channel.register(301, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("medium");
                }

                @Override
                public int getPriority() {
                    return 5;
                }
            });

            channel.publish(301, "test");

            assertEquals(3, callOrder.size());
            assertEquals("high", callOrder.get(0));
            assertEquals("medium", callOrder.get(1));
            assertEquals("low", callOrder.get(2));
        }
    }

    // ========== Two Listener Dispatch Tests ==========

    @Nested
    @DisplayName("Two Listener Dispatch Tests")
    class TwoListenerDispatchTests {

        @Test
        @DisplayName("Should dispatch to 2 listeners")
        void shouldDispatchToTwoListeners() {
            List<String> received = new ArrayList<>();

            channel.register(400, payload -> received.add("1"));
            channel.register(400, payload -> received.add("2"));

            channel.publish(400, "test");

            assertEquals(2, received.size());
        }

        @Test
        @DisplayName("Should dispatch to 2 listeners with priority")
        void shouldDispatchToTwoListenersWithPriority() {
            List<String> order = new ArrayList<>();

            channel.register(401, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("low");
                }

                @Override
                public int getPriority() {
                    return 1;
                }
            });

            channel.register(401, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("high");
                }

                @Override
                public int getPriority() {
                    return 10;
                }
            });

            channel.publish(401, "test");

            assertEquals(2, order.size());
            assertEquals("high", order.get(0));
            assertEquals("low", order.get(1));
        }
    }

    // ========== Single Listener Dispatch Tests ==========

    @Nested
    @DisplayName("Single Listener Dispatch Tests")
    class SingleListenerDispatchTests {

        @Test
        @DisplayName("Should dispatch to single listener")
        void shouldDispatchToSingleListener() {
            AtomicBoolean called = new AtomicBoolean(false);

            channel.register(500, payload -> called.set(true));

            channel.publish(500, "test");

            assertTrue(called.get());
        }
    }

    // ========== ObjectLessEventBus Dispatch Tests ==========

    @Nested
    @DisplayName("ObjectLessEventBus Dispatch Tests")
    class ObjectLessEventBusDispatchTests {

        @Test
        @DisplayName("Should dispatch to 5 listeners via EventBus")
        void shouldDispatchToFiveListenersViaEventBus() {
            AtomicInteger count = new AtomicInteger(0);
            int eventId = 600;

            for (int i = 0; i < 5; i++) {
                eventBus.register(eventId, payload -> count.incrementAndGet());
            }

            eventBus.publish(eventId, "test");

            assertEquals(5, count.get());
        }

        @Test
        @DisplayName("Should handle async publish with 5 listeners")
        void shouldHandleAsyncPublishWithFiveListeners() throws Exception {
            AtomicInteger count = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(5);
            int eventId = 601;

            for (int i = 0; i < 5; i++) {
                eventBus.register(eventId, new ObjectLessEventBus.ObjectLessListener() {
                    @Override
                    public void onEvent(Object payload) {
                        count.incrementAndGet();
                        latch.countDown();
                    }

                    @Override
                    public boolean isAsync() {
                        return true;
                    }
                });
            }

            var future = eventBus.publishAsync(eventId, "test");

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(5, count.get());
        }

        @Test
        @DisplayName("Should dispatch to 5 listeners with priority order")
        void shouldDispatchToFiveListenersWithPriorityOrder() {
            List<String> callOrder = new ArrayList<>();
            int eventId = 602;

            // Register with different priorities
            for (int i = 1; i <= 5; i++) {
                final int priority = i;
                eventBus.register(eventId, new ObjectLessEventBus.ObjectLessListener() {
                    @Override
                    public void onEvent(Object payload) {
                        callOrder.add("p" + priority);
                    }

                    @Override
                    public int getPriority() {
                        return priority;
                    }
                });
            }

            eventBus.publish(eventId, "test");

            assertEquals(5, callOrder.size());
            // Should be in reverse order: 5, 4, 3, 2, 1
            for (int i = 0; i < 5; i++) {
                assertEquals("p" + (5 - i), callOrder.get(i));
            }
        }
    }

    // ========== ObjectLessEventBus Async Tests ==========

    @Nested
    @DisplayName("ObjectLessEventBus Async Tests")
    class ObjectLessEventBusAsyncTests {

        @Test
        @DisplayName("Should publish async with multiple listeners")
        void shouldPublishAsyncWithMultipleListeners() throws Exception {
            List<String> received = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(2);
            int eventId = 700;

            eventBus.register(eventId, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    received.add("listener1");
                    latch.countDown();
                }

                @Override
                public boolean isAsync() {
                    return true;
                }
            });

            eventBus.register(eventId, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    received.add("listener2");
                    latch.countDown();
                }

                @Override
                public boolean isAsync() {
                    return true;
                }
            });

            var future = eventBus.publishAsync(eventId, "test");

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(2, received.size());
        }

        @Test
        @DisplayName("Should publish async to zero listeners")
        void shouldPublishAsyncToZeroListeners() throws Exception {
            int eventId = 701;

            var future = eventBus.publishAsync(eventId, "test");

            Integer count = future.get(1, TimeUnit.SECONDS);
            assertEquals(0, count);
        }
    }

    // ========== EventChannel Tests ==========

    @Nested
    @DisplayName("EventChannel Tests")
    class EventChannelTests {

        @Test
        @DisplayName("Should register with priority on channel")
        void shouldRegisterWithPriorityOnChannel() {
            List<String> order = new ArrayList<>();

            channel.register(800, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("high");
                }

                @Override
                public int getPriority() {
                    return 10;
                }
            });

            channel.register(800, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("low");
                }

                @Override
                public int getPriority() {
                    return 1;
                }
            });

            channel.publish(800, "test");

            assertEquals(2, order.size());
            assertEquals("high", order.get(0));
            assertEquals("low", order.get(1));
        }

        @Test
        @DisplayName("Should publish async to standard channel")
        void shouldPublishAsyncToStandardChannel() throws Exception {
            AtomicBoolean called = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            channel.register(801, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    called.set(true);
                    latch.countDown();
                }

                @Override
                public boolean isAsync() {
                    return true;
                }
            });

            var future = channel.publishAsync(801, "test");

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertTrue(called.get());
        }
    }

    // ========== Zero Listener Tests ==========

    @Nested
    @DisplayName("Zero Listener Tests")
    class ZeroListenerTests {

        @Test
        @DisplayName("Should return 0 for publish with no listeners")
        void shouldReturnZeroForPublishWithNoListeners() {
            int count = channel.publish(900, "no-listeners");

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should return 0 for async publish with no listeners via EventBus")
        void shouldReturnZeroForAsyncPublishWithNoListenersViaEventBus() throws Exception {
            Integer count = eventBus.publishAsync(901, "no-listeners").get(1, TimeUnit.SECONDS);

            assertEquals(0, count);
        }
    }

    // ========== Payload Type Tests ==========

    @Nested
    @DisplayName("Payload Type Tests")
    class PayloadTypeTests {

        @Test
        @DisplayName("Should handle integer payload")
        void shouldHandleIntegerPayload() {
            AtomicInteger received = new AtomicInteger(0);

            channel.register(1000, payload -> received.set((Integer) payload));

            channel.publish(1000, 42);

            assertEquals(42, received.get());
        }

        @Test
        @DisplayName("Should handle list payload")
        void shouldHandleListPayload() {
            List<String> testList = new ArrayList<>();
            testList.add("item1");
            testList.add("item2");

            AtomicReference<List<String>> received = new AtomicReference<>();

            channel.register(1001, payload -> received.set((List<String>) payload));

            channel.publish(1001, testList);

            assertNotNull(received.get());
            assertEquals(2, received.get().size());
        }
    }

    // ========== Listener Management Tests ==========

    @Nested
    @DisplayName("Listener Management Tests")
    class ListenerManagementTests {

        @Test
        @DisplayName("Should handle unregister from different event ID")
        void shouldHandleUnregisterFromDifferentEventId() {
            ObjectLessEventBus.ObjectLessListener listener = payload -> {};

            channel.register(1100, listener);
            channel.register(1101, listener);

            // Unregister from only one event
            channel.unregister(1100, listener);

            // Should still be registered for 1101
            assertEquals(1, channel.getListenerCount());
        }

        @Test
        @DisplayName("Should handle clear when empty")
        void shouldHandleClearWhenEmpty() {
            assertDoesNotThrow(() -> channel.clear());
            assertEquals(0, channel.getListenerCount());
        }
    }

    // ========== Concurrent Publish Tests ==========

    @Nested
    @DisplayName("Concurrent Publish Tests")
    class ConcurrentPublishTests {

        @Test
        @DisplayName("Should handle concurrent publishes")
        void shouldHandleConcurrentPublishes() throws Exception {
            List<String> received = new ArrayList<>();
            int eventId = 1200;
            int publishCount = 5;

            channel.register(eventId, payload -> {
                synchronized (received) {
                    received.add((String) payload);
                }
            });

            List<CompletableFuture<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < publishCount; i++) {
                futures.add(channel.publishAsync(eventId, "test-" + i));
            }

            for (CompletableFuture<Integer> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }

            assertEquals(publishCount, received.size());
        }
    }
}
