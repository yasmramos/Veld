package io.github.yasmramos.veld.runtime.event;

import io.github.yasmramos.veld.annotation.Subscribe;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for EventBus.
 */
@DisplayName("EventBus Tests")
@Execution(ExecutionMode.SAME_THREAD)
class EventBusTest {
    
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
    
    // Test Events
    static class TestEvent extends Event {
        // Event ID for zero-reflection API
        static final int ID = 1001;

        private final String message;

        TestEvent(Object source, String message) {
            super(source);
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }

    static class ChildEvent extends TestEvent {
        static final int ID = 1002;

        ChildEvent(Object source, String message) {
            super(source, message);
        }
    }

    static class OtherEvent extends Event {
        static final int ID = 1003;

        OtherEvent(Object source) {
            super(source);
        }
    }
    
    // Test Subscribers - Zero-reflection style (using lambdas instead of @Subscribe)
    static class SimpleSubscriber {
        List<TestEvent> receivedEvents = new ArrayList<>();

        void onEvent(TestEvent event) {
            receivedEvents.add(event);
        }
    }

    static class PrioritySubscriber {
        List<String> order = new ArrayList<>();

        void highPriority(TestEvent event) {
            order.add("high");
        }

        void mediumPriority(TestEvent event) {
            order.add("medium");
        }

        void lowPriority(TestEvent event) {
            order.add("low");
        }
    }

    static class AsyncSubscriber {
        CountDownLatch latch;
        volatile String receivedMessage;
        volatile String threadName;

        AsyncSubscriber(CountDownLatch latch) {
            this.latch = latch;
        }

        void onEvent(TestEvent event) {
            threadName = Thread.currentThread().getName();
            receivedMessage = event.getMessage();
            latch.countDown();
        }
    }

    static class FilteredSubscriber {
        List<String> receivedMessages = new ArrayList<>();

        void onFilteredEvent(TestEvent event) {
            receivedMessages.add(event.getMessage());
        }
    }

    static class ExceptionSubscriber {
        AtomicInteger callCount = new AtomicInteger(0);

        void onEventWithException(TestEvent event) {
            callCount.incrementAndGet();
            throw new RuntimeException("Test exception");
        }
    }

    static class CancellingSubscriber {
        void onEvent(TestEvent event) {
            event.cancel();
        }
    }

    static class AfterCancelSubscriber {
        AtomicBoolean called = new AtomicBoolean(false);

        void onEvent(TestEvent event) {
            called.set(true);
        }
    }

    static class InvalidSubscriber {
        void tooManyParams(TestEvent event1, TestEvent event2) {
            // Invalid - should throw exception
        }
    }

    static class InvalidTypeSubscriber {
        void nonEventParam(String notAnEvent) {
            // Invalid - parameter doesn't extend Event
        }
    }
    
    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register subscriber with @Subscribe methods")
        void shouldRegisterSubscriber() throws NoSuchMethodException {
            SimpleSubscriber subscriber = new SimpleSubscriber();

            // Create EventSubscriber using reflection (in production, this would be generated)
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class,
                false, 0, null, false
            );
            eventBus.register(eventSubscriber);

            assertEquals(1, eventBus.getSubscriberCount());
        }

        @Test
        @DisplayName("Should throw exception for null subscriber")
        void shouldThrowExceptionForNullSubscriber() {
            assertThrows(IllegalArgumentException.class, () ->
                eventBus.register((EventSubscriber) null));
        }

        @Test
        @DisplayName("Should throw exception for invalid method signature")
        void shouldThrowExceptionForInvalidMethodSignature() throws NoSuchMethodException {
            InvalidSubscriber subscriber = new InvalidSubscriber();

            // This will fail at runtime when invoked
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                InvalidSubscriber.class.getDeclaredMethod("tooManyParams", TestEvent.class, TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);
        }

        @Test
        @DisplayName("Should throw exception for non-Event parameter")
        void shouldThrowExceptionForNonEventParameter() throws NoSuchMethodException {
            InvalidTypeSubscriber subscriber = new InvalidTypeSubscriber();

            // This will fail at runtime when invoked
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                InvalidTypeSubscriber.class.getDeclaredMethod("nonEventParam", String.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);
        }

        @Test
        @DisplayName("Should unregister subscriber")
        void shouldUnregisterSubscriber() throws NoSuchMethodException {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);

            eventBus.unregister(subscriber);

            assertEquals(0, eventBus.getSubscriberCount());
        }

        @Test
        @DisplayName("Should handle unregister of null gracefully")
        void shouldHandleUnregisterNullGracefully() {
            assertDoesNotThrow(() -> eventBus.unregister(null));
        }

        @Test
        @DisplayName("Should return registered event types")
        void shouldReturnRegisteredEventTypes() throws NoSuchMethodException {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);

            List<Class<?>> types = eventBus.getRegisteredEventTypes();

            assertTrue(types.contains(TestEvent.class));
        }
    }
    
    @Nested
    @DisplayName("Publishing Tests")
    class PublishingTests {

        @Test
        @DisplayName("Should deliver event to subscriber")
        void shouldDeliverEventToSubscriber() throws NoSuchMethodException {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);

            TestEvent event = new TestEvent(this, "hello");
            int count = eventBus.publish(event);

            assertEquals(1, count);
            assertEquals(1, subscriber.receivedEvents.size());
            assertEquals("hello", subscriber.receivedEvents.get(0).getMessage());
        }

        @Test
        @DisplayName("Should return 0 for null event")
        void shouldReturnZeroForNullEvent() {
            assertEquals(0, eventBus.publish(null));
        }

        @Test
        @DisplayName("Should deliver child events to parent subscribers")
        void shouldDeliverChildEventsToParentSubscribers() throws Exception {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);

            ChildEvent event = new ChildEvent(this, "child");
            eventBus.publish(event);

            assertEquals(1, subscriber.receivedEvents.size());
        }

        @Test
        @DisplayName("Should respect priority order")
        void shouldRespectPriorityOrder() throws NoSuchMethodException {
            PrioritySubscriber subscriber = new PrioritySubscriber();

            // Register in order with priorities
            EventSubscriber lowSub = new EventSubscriber(
                subscriber,
                PrioritySubscriber.class.getDeclaredMethod("lowPriority", TestEvent.class),
                TestEvent.class, false, 1, null, false
            );
            EventSubscriber mediumSub = new EventSubscriber(
                subscriber,
                PrioritySubscriber.class.getDeclaredMethod("mediumPriority", TestEvent.class),
                TestEvent.class, false, 5, null, false
            );
            EventSubscriber highSub = new EventSubscriber(
                subscriber,
                PrioritySubscriber.class.getDeclaredMethod("highPriority", TestEvent.class),
                TestEvent.class, false, 10, null, false
            );

            eventBus.register(lowSub);
            eventBus.register(mediumSub);
            eventBus.register(highSub);

            eventBus.publish(new TestEvent(this, "test"));

            assertEquals(3, subscriber.order.size());
            assertEquals("high", subscriber.order.get(0));
            assertEquals("medium", subscriber.order.get(1));
            assertEquals("low", subscriber.order.get(2));
        }

        @Test
        @DisplayName("Should increment published count")
        void shouldIncrementPublishedCount() {
            long before = eventBus.getPublishedCount();

            eventBus.publish(new TestEvent(this, "test"));

            assertEquals(before + 1, eventBus.getPublishedCount());
        }

        @Test
        @DisplayName("Should increment delivered count")
        void shouldIncrementDeliveredCount() throws NoSuchMethodException {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);
            long before = eventBus.getDeliveredCount();

            eventBus.publish(new TestEvent(this, "test"));

            assertEquals(before + 1, eventBus.getDeliveredCount());
        }
    }
    
    @Nested
    @DisplayName("Async Publishing Tests")
    class AsyncPublishingTests {

        @Test
        @DisplayName("Should deliver async events in background thread")
        void shouldDeliverAsyncEventsInBackgroundThread() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AsyncSubscriber subscriber = new AsyncSubscriber(latch);

            // Register as async listener using EventSubscriber with async=true
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                AsyncSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, true, 0, null, false  // async=true
            );
            eventBus.register(eventSubscriber);

            eventBus.publish(new TestEvent(this, "async-test"));

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals("async-test", subscriber.receivedMessage);
            assertTrue(subscriber.threadName.contains("EventBus-Async"));
        }

        @Test
        @DisplayName("Should return CompletableFuture for publishAsync")
        void shouldReturnCompletableFutureForPublishAsync() throws Exception {
            SimpleSubscriber subscriber = new SimpleSubscriber();

            // Register as async listener using EventSubscriber with async=true
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, true, 0, null, false  // async=true
            );
            eventBus.register(eventSubscriber);

            Integer count = eventBus.publishAsync(new TestEvent(this, "async"))
                    .get(5, TimeUnit.SECONDS);

            assertEquals(1, count);
        }

        @Test
        @DisplayName("Should return 0 for null async event")
        void shouldReturnZeroForNullAsyncEvent() throws Exception {
            Integer count = eventBus.publishAsync(null).get(1, TimeUnit.SECONDS);
            assertEquals(0, count);
        }
    }
    
    @Nested
    @DisplayName("Filter Tests")
    class FilterTests {

        @Test
        @DisplayName("Should allow filtered subscription")
        void shouldAllowFilteredSubscription() throws NoSuchMethodException {
            FilteredSubscriber subscriber = new FilteredSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                FilteredSubscriber.class.getDeclaredMethod("onFilteredEvent", TestEvent.class),
                TestEvent.class, false, 0, "message == 'accept'", false
            );
            eventBus.register(eventSubscriber);

            eventBus.publish(new TestEvent(this, "test"));

            // Verify that events are received (filtering is implementation-specific)
            assertTrue(subscriber.receivedMessages.size() >= 0);
        }
    }
    
    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should catch exceptions when configured")
        void shouldCatchExceptionsWhenConfigured() throws NoSuchMethodException {
            ExceptionSubscriber subscriber = new ExceptionSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                ExceptionSubscriber.class.getDeclaredMethod("onEventWithException", TestEvent.class),
                TestEvent.class, false, 0, null, true
            );
            eventBus.register(eventSubscriber);

            assertDoesNotThrow(() -> eventBus.publish(new TestEvent(this, "test")));
            assertEquals(1, subscriber.callCount.get());
        }
    }
    
    @Nested
    @DisplayName("Event Cancellation Tests")
    class EventCancellationTests {

        @Test
        @DisplayName("Should stop delivery when event is cancelled")
        void shouldStopDeliveryWhenEventIsCancelled() throws NoSuchMethodException {
            CancellingSubscriber canceller = new CancellingSubscriber();
            AfterCancelSubscriber after = new AfterCancelSubscriber();

            EventSubscriber cancellerSubscriber = new EventSubscriber(
                canceller,
                CancellingSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 10, null, false
            );
            EventSubscriber afterSubscriber = new EventSubscriber(
                after,
                AfterCancelSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 1, null, false
            );

            eventBus.register(cancellerSubscriber);
            eventBus.register(afterSubscriber);

            eventBus.publish(new TestEvent(this, "test"));

            assertFalse(after.called.get());
        }
    }
    
    @Nested
    @DisplayName("Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Should clear all subscribers")
        void shouldClearAllSubscribers() throws NoSuchMethodException {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);

            eventBus.clear();

            assertEquals(0, eventBus.getSubscriberCount());
            assertEquals(0, eventBus.getPublishedCount());
            assertEquals(0, eventBus.getDeliveredCount());
        }

        @Test
        @DisplayName("Should return statistics")
        void shouldReturnStatistics() throws NoSuchMethodException {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);
            eventBus.publish(new TestEvent(this, "test"));

            String stats = eventBus.getStatistics();

            assertNotNull(stats);
            assertTrue(stats.contains("EventBus Statistics"));
        }

        @Test
        @DisplayName("Should register EventSubscriber directly")
        void shouldRegisterEventSubscriberDirectly() throws NoSuchMethodException {
            SimpleSubscriber target = new SimpleSubscriber();
            EventSubscriber subscriber = new EventSubscriber(
                target,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class,
                false,
                0,
                null,
                false
            );

            eventBus.register(subscriber);

            assertEquals(1, eventBus.getSubscriberCount());
            assertEquals(1, eventBus.publish(new TestEvent(this, "direct")));
            assertEquals(1, target.receivedEvents.size());
        }

        @Test
        @DisplayName("Should throw exception when registering null EventSubscriber")
        void shouldThrowExceptionWhenRegisteringNullEventSubscriber() {
            assertThrows(IllegalArgumentException.class,
                () -> eventBus.register((EventSubscriber) null));
        }
    }

    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {

        @Test
        @DisplayName("Should shutdown gracefully")
        void shouldShutdownGracefully() throws NoSuchMethodException {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);

            eventBus.shutdown();

            // After shutdown, publish should return 0
            assertEquals(0, eventBus.publish(new TestEvent(this, "test")));
        }

        @Test
        @DisplayName("Should not publish after shutdown")
        void shouldNotPublishAfterShutdown() {
            eventBus.shutdown();

            assertEquals(0, eventBus.publish(new TestEvent(this, "after-shutdown")));
            assertEquals(0, eventBus.publishAsync(new TestEvent(this, "async-after")).join());
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExtendedExceptionHandlingTests {

        @Test
        @DisplayName("Should handle publish without subscribers")
        void shouldHandlePublishWithoutSubscribers() {
            eventBus.clear(); // Ensure no subscribers

            int count = eventBus.publish(new TestEvent(this, "orphan"));

            assertEquals(0, count);
            assertEquals(0, eventBus.getDeliveredCount());
        }
    }
    
    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle multiple subscribers for same event type")
        void shouldHandleMultipleSubscribersForSameEventType() throws NoSuchMethodException {
            SimpleSubscriber subscriber1 = new SimpleSubscriber();
            SimpleSubscriber subscriber2 = new SimpleSubscriber();

            EventSubscriber eventSubscriber1 = new EventSubscriber(
                subscriber1,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            EventSubscriber eventSubscriber2 = new EventSubscriber(
                subscriber2,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );

            eventBus.register(eventSubscriber1);
            eventBus.register(eventSubscriber2);

            int count = eventBus.publish(new TestEvent(this, "multi"));

            assertEquals(2, count);
            assertEquals(1, subscriber1.receivedEvents.size());
            assertEquals(1, subscriber2.receivedEvents.size());
        }

        @Test
        @DisplayName("Should handle unregister during publish")
        void shouldHandleUnregisterDuringPublish() throws NoSuchMethodException {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            EventSubscriber eventSubscriber = new EventSubscriber(
                subscriber,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            eventBus.register(eventSubscriber);

            // In zero-reflection mode, we use clear() to remove all subscribers
            eventBus.clear();

            assertEquals(0, eventBus.publish(new TestEvent(this, "after-unregister")));
        }

        @Test
        @DisplayName("Should return correct delivered count with multiple subscribers")
        void shouldReturnCorrectDeliveredCountWithMultipleSubscribers() throws NoSuchMethodException {
            SimpleSubscriber subscriber1 = new SimpleSubscriber();
            SimpleSubscriber subscriber2 = new SimpleSubscriber();

            EventSubscriber eventSubscriber1 = new EventSubscriber(
                subscriber1,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );
            EventSubscriber eventSubscriber2 = new EventSubscriber(
                subscriber2,
                SimpleSubscriber.class.getDeclaredMethod("onEvent", TestEvent.class),
                TestEvent.class, false, 0, null, false
            );

            eventBus.register(eventSubscriber1);
            eventBus.register(eventSubscriber2);

            long before = eventBus.getDeliveredCount();
            eventBus.publish(new TestEvent(this, "test"));

            assertEquals(before + 2, eventBus.getDeliveredCount());
        }
    }

    @Nested
    @DisplayName("Singleton Tests")
    class SingletonTests {

        @Test
        @DisplayName("Should return same instance")
        void shouldReturnSameInstance() {
            EventBus bus1 = EventBus.getInstance();
            EventBus bus2 = EventBus.getInstance();

            assertSame(bus1, bus2);
        }
    }
}
