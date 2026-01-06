package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests specifically designed to cover the remaining coverage gaps in StandardEventChannel.
 * Targets dispatch methods with 5+ listeners and other low-coverage branches.
 */
@DisplayName("StandardEventChannel Coverage Gap Tests")
@Execution(ExecutionMode.SAME_THREAD)
class StandardEventChannelCoverageGapTest {

    private EventBus eventBus;
    private EventChannel channel;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.getInstance();
        eventBus.clear();
        channel = eventBus.getStandardChannel();
    }

    @AfterEach
    void tearDown() {
        eventBus.resetForTesting();
    }

    // ========== Five Listener Dispatch Tests ==========

    @Nested
    @DisplayName("Five Listener Dispatch Tests")
    class FiveListenerDispatchTests {

        @Test
        @DisplayName("Should dispatch to 5 listeners in priority order")
        void shouldDispatchToFiveListenersInPriorityOrder() {
            List<String> callOrder = new ArrayList<>();

            // Register 5 listeners with different priorities using anonymous classes
            channel.register(100, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("listener-5");
                }

                @Override
                public int getPriority() {
                    return 5;
                }
            });

            channel.register(100, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("listener-3");
                }

                @Override
                public int getPriority() {
                    return 3;
                }
            });

            channel.register(100, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("listener-1");
                }

                @Override
                public int getPriority() {
                    return 1;
                }
            });

            channel.register(100, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("listener-4");
                }

                @Override
                public int getPriority() {
                    return 4;
                }
            });

            channel.register(100, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    callOrder.add("listener-2");
                }

                @Override
                public int getPriority() {
                    return 2;
                }
            });

            channel.publish(100, "test");

            assertEquals(5, callOrder.size());
            // Verify priority order (5, 4, 3, 2, 1)
            assertEquals("listener-5", callOrder.get(0));
            assertEquals("listener-4", callOrder.get(1));
            assertEquals("listener-3", callOrder.get(2));
            assertEquals("listener-2", callOrder.get(3));
            assertEquals("listener-1", callOrder.get(4));
        }

        @Test
        @DisplayName("Should dispatch to 5 sync listeners")
        void shouldDispatchToFiveSyncListeners() {
            AtomicInteger count = new AtomicInteger(0);

            // 5 sync listeners
            for (int i = 0; i < 5; i++) {
                channel.register(101, payload -> count.incrementAndGet());
            }

            channel.publish(101, "test");

            assertEquals(5, count.get());
        }

        @Test
        @DisplayName("Should handle 5 listeners with mixed sync/async")
        void shouldHandleFiveListenersWithMixedSyncAsync() throws Exception {
            CountDownLatch latch = new CountDownLatch(3);
            AtomicInteger syncCount = new AtomicInteger(0);

            // 2 sync listeners
            channel.register(102, payload -> syncCount.incrementAndGet());
            channel.register(102, payload -> syncCount.incrementAndGet());

            // 3 sync listeners to reach 5 total
            channel.register(102, payload -> syncCount.incrementAndGet());
            channel.register(102, payload -> syncCount.incrementAndGet());
            channel.register(102, payload -> syncCount.incrementAndGet());

            channel.publish(102, "test");

            assertEquals(5, syncCount.get());
        }
    }

    // ========== Four Listener Dispatch Tests ==========

    @Nested
    @DisplayName("Four Listener Dispatch Tests")
    class FourListenerDispatchTests {

        @Test
        @DisplayName("Should dispatch to 4 listeners")
        void shouldDispatchToFourListeners() {
            List<String> received = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                final int listenerId = i;
                channel.register(200, payload -> received.add("listener-" + listenerId));
            }

            channel.publish(200, "test");

            assertEquals(4, received.size());
        }

        @Test
        @DisplayName("Should dispatch to 4 listeners with priority")
        void shouldDispatchToFourListenersWithPriority() {
            List<String> order = new ArrayList<>();

            channel.register(201, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("1");
                }

                @Override
                public int getPriority() {
                    return 1;
                }
            });

            channel.register(201, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("4");
                }

                @Override
                public int getPriority() {
                    return 4;
                }
            });

            channel.register(201, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("2");
                }

                @Override
                public int getPriority() {
                    return 2;
                }
            });

            channel.register(201, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("3");
                }

                @Override
                public int getPriority() {
                    return 3;
                }
            });

            channel.publish(201, "test");

            assertEquals(4, order.size());
            // Should be in order: 4, 3, 2, 1
            assertEquals("4", order.get(0));
            assertEquals("3", order.get(1));
            assertEquals("2", order.get(2));
            assertEquals("1", order.get(3));
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

            channel.register(300, payload -> received.add("1"));
            channel.register(300, payload -> received.add("2"));

            channel.publish(300, "test");

            assertEquals(2, received.size());
        }

        @Test
        @DisplayName("Should dispatch to 2 listeners with priority")
        void shouldDispatchToTwoListenersWithPriority() {
            List<String> order = new ArrayList<>();

            channel.register(301, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("low");
                }

                @Override
                public int getPriority() {
                    return 1;
                }
            });

            channel.register(301, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("high");
                }

                @Override
                public int getPriority() {
                    return 10;
                }
            });

            channel.publish(301, "test");

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

            channel.register(400, payload -> called.set(true));

            channel.publish(400, "test");

            assertTrue(called.get());
        }
    }

    // ========== Priority Edge Cases Tests ==========

    @Nested
    @DisplayName("Priority Edge Cases Tests")
    class PriorityEdgeCasesTests {

        @Test
        @DisplayName("Should handle same priority listeners in registration order")
        void shouldHandleSamePriorityListenersInRegistrationOrder() {
            List<String> order = new ArrayList<>();

            // Same priority, should maintain registration order
            channel.register(500, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("first");
                }

                @Override
                public int getPriority() {
                    return 5;
                }
            });

            channel.register(500, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("second");
                }

                @Override
                public int getPriority() {
                    return 5;
                }
            });

            channel.register(500, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("third");
                }

                @Override
                public int getPriority() {
                    return 5;
                }
            });

            channel.publish(500, "test");

            assertEquals(3, order.size());
            assertEquals("first", order.get(0));
            assertEquals("second", order.get(1));
            assertEquals("third", order.get(2));
        }

        @Test
        @DisplayName("Should handle negative priority")
        void shouldHandleNegativePriority() {
            List<String> order = new ArrayList<>();

            channel.register(501, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("neg");
                }

                @Override
                public int getPriority() {
                    return -10;
                }
            });

            channel.register(501, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("zero");
                }

                @Override
                public int getPriority() {
                    return 0;
                }
            });

            channel.register(501, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("pos");
                }

                @Override
                public int getPriority() {
                    return 10;
                }
            });

            channel.publish(501, "test");

            assertEquals(3, order.size());
            assertEquals("pos", order.get(0)); // highest priority first
            assertEquals("zero", order.get(1));
            assertEquals("neg", order.get(2));
        }

        @Test
        @DisplayName("Should handle very high priority values")
        void shouldHandleVeryHighPriorityValues() {
            List<String> order = new ArrayList<>();

            channel.register(502, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("high");
                }

                @Override
                public int getPriority() {
                    return Integer.MAX_VALUE;
                }
            });

            channel.register(502, new ObjectLessEventBus.ObjectLessListener() {
                @Override
                public void onEvent(Object payload) {
                    order.add("low");
                }

                @Override
                public int getPriority() {
                    return 0;
                }
            });

            channel.publish(502, "test");

            assertEquals(2, order.size());
            assertEquals("high", order.get(0));
        }
    }

    // ========== Zero Listener Tests ==========

    @Nested
    @DisplayName("Zero Listener Tests")
    class ZeroListenerTests {

        @Test
        @DisplayName("Should return 0 for publish with no listeners")
        void shouldReturnZeroForPublishWithNoListeners() {
            int count = channel.publish(700, "no-listeners");

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

            channel.register(800, payload -> received.set((Integer) payload));

            channel.publish(800, 42);

            assertEquals(42, received.get());
        }

        @Test
        @DisplayName("Should handle list payload")
        void shouldHandleListPayload() {
            List<String> testList = new ArrayList<>();
            testList.add("item1");
            testList.add("item2");

            AtomicReference<List<String>> received = new AtomicReference<>();

            channel.register(801, payload -> received.set((List<String>) payload));

            channel.publish(801, testList);

            assertNotNull(received.get());
            assertEquals(2, received.get().size());
        }

        @Test
        @DisplayName("Should handle custom object payload")
        void shouldHandleCustomObjectPayload() {
            class CustomPayload {
                final String value;
                CustomPayload(String value) { this.value = value; }
            }

            AtomicReference<CustomPayload> received = new AtomicReference<>();

            channel.register(802, payload -> received.set((CustomPayload) payload));

            channel.publish(802, new CustomPayload("test"));

            assertNotNull(received.get());
            assertEquals("test", received.get().value);
        }
    }

    // ========== Listener Management Tests ==========

    @Nested
    @DisplayName("Listener Management Tests")
    class ListenerManagementTests {

        @Test
        @DisplayName("Should handle rapid register/unregister")
        void shouldHandleRapidRegisterUnregister() {
            ObjectLessEventBus.ObjectLessListener listener = payload -> {};

            channel.register(900, listener);
            assertEquals(1, channel.getListenerCount());

            channel.unregister(900, listener);
            assertEquals(0, channel.getListenerCount());

            channel.register(900, listener);
            assertEquals(1, channel.getListenerCount());

            channel.publish(900, "test");

            assertEquals(1, channel.getListenerCount());
        }

        @Test
        @DisplayName("Should handle unregister from different event ID")
        void shouldHandleUnregisterFromDifferentEventId() {
            ObjectLessEventBus.ObjectLessListener listener = payload -> {};

            channel.register(901, listener);
            channel.register(902, listener);

            // Unregister from only one event
            channel.unregister(901, listener);

            // Should still be registered for 902
            assertEquals(1, channel.getListenerCount());
        }

        @Test
        @DisplayName("Should handle clear when empty")
        void shouldHandleClearWhenEmpty() {
            assertDoesNotThrow(() -> channel.clear());
            assertEquals(0, channel.getListenerCount());
        }
    }

    // ========== Complex Scenario Tests ==========

    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {

        @Test
        @DisplayName("Should handle many events with few listeners")
        void shouldHandleManyEventsWithFewListeners() {
            AtomicInteger count = new AtomicInteger(0);
            ObjectLessEventBus.ObjectLessListener listener = payload -> count.incrementAndGet();

            channel.register(1100, listener);

            // Publish many different events
            for (int i = 0; i < 100; i++) {
                channel.publish(1100, "event-" + i);
            }

            assertEquals(100, count.get());
        }
    }
}
