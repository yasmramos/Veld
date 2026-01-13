/*
 * Copyright (c) 2025. Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
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
package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ObjectEventBus to increase code coverage.
 * Uses EventSubscriber for zero-reflection mode.
 */
class ObjectEventBusTest {

    private EventBus eventBus;
    private TestEventHandler handler;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.getInstance();
        eventBus.clear();
        handler = new TestEventHandler();
    }

    @Test
    void testRegisterAndUnregister() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);

        // Act
        eventBus.register(subscriber);
        assertEquals(1, eventBus.getSubscriberCount());

        eventBus.unregister(handler);
        assertEquals(0, eventBus.getSubscriberCount());
    }

    @Test
    void testPublishEvent() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act
        int count = eventBus.publish(new TestEvent("test"));

        // Assert
        assertEquals(1, count);
        assertEquals(1, handler.eventCount);
    }

    @Test
    void testPublishEventWithNoSubscribers() {
        // Act
        int count = eventBus.publish(new TestEvent("test"));

        // Assert
        assertEquals(0, count);
    }

    @Test
    void testMultipleEvents() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act
        eventBus.publish(new TestEvent("event1"));
        eventBus.publish(new TestEvent("event2"));
        eventBus.publish(new TestEvent("event3"));

        // Assert
        assertEquals(3, handler.eventCount);
    }

    @Test
    void testMultipleHandlers() throws Exception {
        // Arrange
        TestEventHandler handler2 = new TestEventHandler();
        EventSubscriber subscriber1 = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        EventSubscriber subscriber2 = createSubscriber(handler2, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber1);
        eventBus.register(subscriber2);

        // Act
        int count = eventBus.publish(new TestEvent("test"));

        // Assert
        assertEquals(2, count);
        assertEquals(1, handler.eventCount);
        assertEquals(1, handler2.eventCount);
    }

    @Test
    void testUnregisterRemovesHandler() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);
        eventBus.publish(new TestEvent("before"));

        // Act
        eventBus.unregister(handler);
        eventBus.publish(new TestEvent("after"));

        // Assert
        assertEquals(1, handler.eventCount);
    }

    @Test
    void testGetRegisteredEventTypes() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act
        var types = eventBus.getRegisteredEventTypes();

        // Assert
        assertTrue(types.contains(TestEvent.class));
    }

    @Test
    void testGetStatistics() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act
        String stats = eventBus.getStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.contains("EventBus"));
    }

    @Test
    void testClear() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act
        eventBus.clear();

        // Assert
        assertEquals(0, eventBus.getSubscriberCount());
    }

    @Test
    void testNullSourceEvent() throws Exception {
        // Arrange - register handler for NullSourceEvent
        NullHandler nullHandler = new NullHandler();
        EventSubscriber subscriber = createSubscriber(nullHandler, "handleNullEvent", NullSourceEvent.class);
        eventBus.register(subscriber);

        // Act
        NullSourceEvent event = new NullSourceEvent(null);
        int count = eventBus.publish(event);

        // Assert
        assertEquals(1, count);
    }

    @Test
    void testEventMetadata() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act
        TestEvent event = new TestEvent("test-data");
        eventBus.publish(event);

        // Assert
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertEquals("test-data", event.getData());
    }

    @Test
    void testCancelEvent() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act - events are delivered even if cancelled
        TestEvent event = new TestEvent("test");
        event.cancel();
        int count = eventBus.publish(event);

        // Assert - the handler receives the event
        assertEquals(1, count);
        assertEquals(1, handler.eventCount);
    }

    @Test
    void testConsumeEvent() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act
        TestEvent event = new TestEvent("test");
        event.consume();
        int count = eventBus.publish(event);

        // Assert
        assertEquals(1, count);
        assertTrue(event.isConsumed());
    }

    @Test
    void testRegisterNullSubscriber() {
        // Act & Assert - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> eventBus.register((EventSubscriber) null));
    }

    @Test
    void testUnregisterNullSubscriber() {
        // Act & Assert - should not throw (just returns)
        assertDoesNotThrow(() -> eventBus.unregister((Object) null));
    }

    @Test
    void testPublishAsync() throws Exception {
        // Arrange
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act
        CompletableFuture<Integer> future = eventBus.publishAsync(new TestEvent("async-test"));

        // Assert
        assertNotNull(future);
        Integer count = future.get();
        assertEquals(1, count);
        assertEquals(1, handler.eventCount);
    }

    @Test
    void testPublishAsyncWithNoSubscribers() throws Exception {
        // Act
        CompletableFuture<Integer> future = eventBus.publishAsync(new TestEvent("test"));

        // Assert
        assertNotNull(future);
        Integer count = future.get();
        assertEquals(0, count);
    }

    @Test
    void testInheritanceHandling() throws Exception {
        // Arrange - register for TestEvent.class (parent)
        EventSubscriber subscriber = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber);

        // Act - publish SubTestEvent (child of TestEvent)
        int count = eventBus.publish(new SubTestEvent("inheritance-test"));

        // Assert - should be handled because SubTestEvent extends TestEvent
        assertEquals(1, count);
    }

    @Test
    void testMultipleRegisterSameHandler() throws Exception {
        // Arrange
        EventSubscriber subscriber1 = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        EventSubscriber subscriber2 = createSubscriber(handler, "handleTestEvent", TestEvent.class);
        eventBus.register(subscriber1);
        eventBus.register(subscriber2);

        // Act
        eventBus.publish(new TestEvent("test"));

        // Assert - should be called twice (two different subscribers)
        assertEquals(2, handler.eventCount);
    }

    @Test
    void testSubscriberWithPriority() throws Exception {
        // Arrange
        PriorityHandler lowPriority = new PriorityHandler("low");
        PriorityHandler highPriority = new PriorityHandler("high");

        EventSubscriber lowSub = new EventSubscriber(
            lowPriority,
            lowPriority.getClass().getDeclaredMethod("handle", TestEvent.class),
            TestEvent.class,
            false,
            1,  // lower priority
            "",
            false
        );

        EventSubscriber highSub = new EventSubscriber(
            highPriority,
            highPriority.getClass().getDeclaredMethod("handle", TestEvent.class),
            TestEvent.class,
            false,
            10,  // higher priority
            "",
            false
        );

        eventBus.register(lowSub);
        eventBus.register(highSub);

        // Act
        eventBus.publish(new TestEvent("test"));

        // Assert - both handlers should be called
        assertEquals(1, lowPriority.order);
        assertEquals(1, highPriority.order);
    }

    /**
     * Helper method to create an EventSubscriber
     */
    private EventSubscriber createSubscriber(Object target, String methodName, Class<?> eventType) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, eventType);
        return new EventSubscriber(target, method, eventType, false, 0, "", false);
    }

    // Custom event class
    static class TestEvent extends Event {
        private final String data;

        public TestEvent(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    // Event with null source
    static class NullSourceEvent extends Event {
        public NullSourceEvent(Object source) {
            super(source);
        }
    }

    // Subclass of TestEvent for inheritance testing
    static class SubTestEvent extends TestEvent {
        public SubTestEvent(String data) {
            super(data);
        }
    }

    // Handler class
    static class TestEventHandler {
        int eventCount = 0;

        public void handleTestEvent(TestEvent event) {
            eventCount++;
        }
    }

    // Handler that checks for cancelled events
    static class CancelAwareHandler {
        int cancelCount = 0;

        public void handleCancelCheck(TestEvent event) {
            if (event.isCancelled()) {
                cancelCount++;
            }
        }
    }

    // Handler for base event type
    static class BaseEventHandler {
        int count = 0;

        public void handleEvent(Event event) {
            count++;
        }
    }

    // Handler for null source events
    static class NullHandler {
        int count = 0;

        public void handleNullEvent(NullSourceEvent event) {
            count++;
        }
    }

    // Handler with priority
    static class PriorityHandler {
        final String name;
        int order = 0;

        PriorityHandler(String name) {
            this.name = name;
        }

        public void handle(TestEvent event) {
            order++;
        }
    }

    // Handler with filter
    static class FilteredHandler {
        int count = 0;

        public void handleFiltered(TestEvent event) {
            count++;
        }
    }
}
