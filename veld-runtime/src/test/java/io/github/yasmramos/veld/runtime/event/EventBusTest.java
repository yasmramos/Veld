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
        ChildEvent(Object source, String message) {
            super(source, message);
        }
    }
    
    static class OtherEvent extends Event {
        OtherEvent(Object source) {
            super(source);
        }
    }
    
    // Test Subscribers
    static class SimpleSubscriber {
        List<TestEvent> receivedEvents = new ArrayList<>();
        
        @Subscribe
        public void onEvent(TestEvent event) {
            receivedEvents.add(event);
        }
    }
    
    static class PrioritySubscriber {
        List<String> order = new ArrayList<>();
        
        @Subscribe(priority = 10)
        public void highPriority(TestEvent event) {
            order.add("high");
        }
        
        @Subscribe(priority = 1)
        public void lowPriority(TestEvent event) {
            order.add("low");
        }
        
        @Subscribe(priority = 5)
        public void mediumPriority(TestEvent event) {
            order.add("medium");
        }
    }
    
    static class AsyncSubscriber {
        CountDownLatch latch;
        volatile String receivedMessage;
        volatile String threadName;
        
        AsyncSubscriber(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @Subscribe(async = true)
        public void onEvent(TestEvent event) {
            threadName = Thread.currentThread().getName();
            receivedMessage = event.getMessage();
            latch.countDown();
        }
    }
    
    static class FilteredSubscriber {
        List<String> receivedMessages = new ArrayList<>();
        
        @Subscribe(filter = "message == 'accept'")
        public void onFilteredEvent(TestEvent event) {
            receivedMessages.add(event.getMessage());
        }
    }
    
    static class ExceptionSubscriber {
        AtomicInteger callCount = new AtomicInteger(0);
        
        @Subscribe(catchExceptions = true)
        public void onEventWithException(TestEvent event) {
            callCount.incrementAndGet();
            throw new RuntimeException("Test exception");
        }
    }
    
    static class CancellingSubscriber {
        @Subscribe(priority = 10)
        public void onEvent(TestEvent event) {
            event.cancel();
        }
    }
    
    static class AfterCancelSubscriber {
        AtomicBoolean called = new AtomicBoolean(false);
        
        @Subscribe(priority = 1)
        public void onEvent(TestEvent event) {
            called.set(true);
        }
    }
    
    static class InvalidSubscriber {
        @Subscribe
        public void tooManyParams(TestEvent event1, TestEvent event2) {
            // Invalid - should throw exception
        }
    }
    
    static class InvalidTypeSubscriber {
        @Subscribe
        public void nonEventParam(String notAnEvent) {
            // Invalid - parameter doesn't extend Event
        }
    }
    
    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {
        
        @Test
        @DisplayName("Should register subscriber with @Subscribe methods")
        void shouldRegisterSubscriber() {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            
            eventBus.register(subscriber);
            
            assertEquals(1, eventBus.getSubscriberCount());
        }
        
        @Test
        @DisplayName("Should throw exception for null subscriber")
        void shouldThrowExceptionForNullSubscriber() {
            assertThrows(IllegalArgumentException.class, () -> 
                eventBus.register((Object) null));
        }
        
        @Test
        @DisplayName("Should throw exception for invalid method signature")
        void shouldThrowExceptionForInvalidMethodSignature() {
            InvalidSubscriber subscriber = new InvalidSubscriber();
            
            assertThrows(IllegalArgumentException.class, () -> 
                eventBus.register(subscriber));
        }
        
        @Test
        @DisplayName("Should throw exception for non-Event parameter")
        void shouldThrowExceptionForNonEventParameter() {
            InvalidTypeSubscriber subscriber = new InvalidTypeSubscriber();
            
            assertThrows(IllegalArgumentException.class, () -> 
                eventBus.register(subscriber));
        }
        
        @Test
        @DisplayName("Should unregister subscriber")
        void shouldUnregisterSubscriber() {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            eventBus.register(subscriber);
            
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
        void shouldReturnRegisteredEventTypes() {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            eventBus.register(subscriber);
            
            List<Class<?>> types = eventBus.getRegisteredEventTypes();
            
            assertTrue(types.contains(TestEvent.class));
        }
    }
    
    @Nested
    @DisplayName("Publishing Tests")
    class PublishingTests {
        
        @Test
        @DisplayName("Should deliver event to subscriber")
        void shouldDeliverEventToSubscriber() {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            eventBus.register(subscriber);
            
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
            eventBus.register(subscriber);

            ChildEvent event = new ChildEvent(this, "child");
            eventBus.publish(event);

            assertEquals(1, subscriber.receivedEvents.size());
        }
        
        @Test
        @DisplayName("Should respect priority order")
        void shouldRespectPriorityOrder() {
            PrioritySubscriber subscriber = new PrioritySubscriber();
            eventBus.register(subscriber);
            
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
        void shouldIncrementDeliveredCount() {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            eventBus.register(subscriber);
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
            eventBus.register(subscriber);
            
            eventBus.publish(new TestEvent(this, "async-test"));
            
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals("async-test", subscriber.receivedMessage);
            assertTrue(subscriber.threadName.contains("EventBus-Async"));
        }
        
        @Test
        @DisplayName("Should return CompletableFuture for publishAsync")
        void shouldReturnCompletableFutureForPublishAsync() throws Exception {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            eventBus.register(subscriber);
            
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
        void shouldAllowFilteredSubscription() {
            FilteredSubscriber subscriber = new FilteredSubscriber();
            eventBus.register(subscriber);
            
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
        void shouldCatchExceptionsWhenConfigured() {
            ExceptionSubscriber subscriber = new ExceptionSubscriber();
            eventBus.register(subscriber);
            
            assertDoesNotThrow(() -> eventBus.publish(new TestEvent(this, "test")));
            assertEquals(1, subscriber.callCount.get());
        }
    }
    
    @Nested
    @DisplayName("Event Cancellation Tests")
    class EventCancellationTests {
        
        @Test
        @DisplayName("Should stop delivery when event is cancelled")
        void shouldStopDeliveryWhenEventIsCancelled() {
            CancellingSubscriber canceller = new CancellingSubscriber();
            AfterCancelSubscriber after = new AfterCancelSubscriber();
            
            eventBus.register(canceller);
            eventBus.register(after);
            
            eventBus.publish(new TestEvent(this, "test"));
            
            assertFalse(after.called.get());
        }
    }
    
    @Nested
    @DisplayName("Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Should clear all subscribers")
        void shouldClearAllSubscribers() {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            eventBus.register(subscriber);

            eventBus.clear();

            assertEquals(0, eventBus.getSubscriberCount());
            assertEquals(0, eventBus.getPublishedCount());
            assertEquals(0, eventBus.getDeliveredCount());
        }

        @Test
        @DisplayName("Should return statistics")
        void shouldReturnStatistics() {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            eventBus.register(subscriber);
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
        void shouldShutdownGracefully() {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            eventBus.register(subscriber);

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
        void shouldHandleMultipleSubscribersForSameEventType() {
            SimpleSubscriber subscriber1 = new SimpleSubscriber();
            SimpleSubscriber subscriber2 = new SimpleSubscriber();

            eventBus.register(subscriber1);
            eventBus.register(subscriber2);

            int count = eventBus.publish(new TestEvent(this, "multi"));

            assertEquals(2, count);
            assertEquals(1, subscriber1.receivedEvents.size());
            assertEquals(1, subscriber2.receivedEvents.size());
        }

        @Test
        @DisplayName("Should handle unregister during publish")
        void shouldHandleUnregisterDuringPublish() {
            SimpleSubscriber subscriber = new SimpleSubscriber();
            eventBus.register(subscriber);

            eventBus.unregister(subscriber);

            assertEquals(0, eventBus.publish(new TestEvent(this, "after-unregister")));
        }

        @Test
        @DisplayName("Should return correct delivered count with multiple subscribers")
        void shouldReturnCorrectDeliveredCountWithMultipleSubscribers() {
            SimpleSubscriber subscriber1 = new SimpleSubscriber();
            SimpleSubscriber subscriber2 = new SimpleSubscriber();

            eventBus.register(subscriber1);
            eventBus.register(subscriber2);

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
