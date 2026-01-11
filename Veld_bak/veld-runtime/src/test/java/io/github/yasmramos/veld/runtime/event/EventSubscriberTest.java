package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for EventSubscriber.
 */
@DisplayName("EventSubscriber Tests")
class EventSubscriberTest {

    // Test events
    static class TestEvent extends Event {
        private final String message;

        TestEvent(String message) {
            super();
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    static class ChildEvent extends TestEvent {
        ChildEvent(String message) {
            super(message);
        }
    }

    static class OtherEvent extends Event {
        OtherEvent() {
            super();
        }
    }

    // Test handler class
    static class TestHandler {
        String receivedMessage;
        boolean methodCalled = false;

        public void handleEvent(TestEvent event) {
            methodCalled = true;
            receivedMessage = event.getMessage();
        }

        public void throwingHandler(TestEvent event) {
            throw new RuntimeException("Test exception");
        }

        private void privateHandler(TestEvent event) {
            methodCalled = true;
        }

        // Methods for signature validation tests
        public void noParams() {
            // Invalid - no parameters
        }

        public void tooManyParams(TestEvent event1, TestEvent event2) {
            // Invalid - too many parameters
        }

        public void nonEventParam(String notAnEvent) {
            // Invalid - parameter doesn't extend Event
        }

        public void differentEventParam(OtherEvent event) {
            // Invalid - different event type incompatible with TestEvent
        }
    }

    private TestHandler handler;
    private Method handleEventMethod;
    private Method throwingMethod;
    private Method noParamsMethod;
    private Method tooManyParamsMethod;
    private Method nonEventParamMethod;
    private Method differentEventParamMethod;
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @BeforeEach
    void setUp() throws Exception {
        handler = new TestHandler();
        handleEventMethod = TestHandler.class.getMethod("handleEvent", TestEvent.class);
        throwingMethod = TestHandler.class.getMethod("throwingHandler", TestEvent.class);
        noParamsMethod = TestHandler.class.getMethod("noParams");
        tooManyParamsMethod = TestHandler.class.getMethod("tooManyParams", TestEvent.class, TestEvent.class);
        nonEventParamMethod = TestHandler.class.getMethod("nonEventParam", String.class);
        differentEventParamMethod = TestHandler.class.getMethod("differentEventParam", OtherEvent.class);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create subscriber with all parameters")
        void shouldCreateSubscriberWithAllParameters() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 5, "event.message == 'test'", true
            );

            assertNotNull(subscriber);
            assertSame(handler, subscriber.getTarget());
            assertEquals(handleEventMethod.getName(), subscriber.getMethodName());
            assertEquals(TestEvent.class, subscriber.getEventType());
            assertFalse(subscriber.isAsync());
            assertEquals(5, subscriber.getPriority());
            assertEquals("event.message == 'test'", subscriber.getFilter());
            assertTrue(subscriber.isCatchExceptions());
        }

        @Test
        @DisplayName("Should throw exception for null target")
        void shouldThrowExceptionForNullTarget() {
            assertThrows(NullPointerException.class, () ->
                    new EventSubscriber(null, handleEventMethod, TestEvent.class,
                            false, 0, "", false));
        }

        @Test
        @DisplayName("Should throw exception for null method")
        void shouldThrowExceptionForNullMethod() {
            assertThrows(NullPointerException.class, () ->
                    new EventSubscriber(handler, null, TestEvent.class,
                            false, 0, "", false));
        }

        @Test
        @DisplayName("Should throw exception for null event type")
        void shouldThrowExceptionForNullEventType() {
            assertThrows(NullPointerException.class, () ->
                    new EventSubscriber(handler, handleEventMethod, null,
                            false, 0, "", false));
        }

        @Test
        @DisplayName("Should handle null filter as empty string")
        void shouldHandleNullFilterAsEmptyString() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, null, false
            );

            assertEquals("", subscriber.getFilter());
            assertFalse(subscriber.hasFilter());
        }
    }

    @Nested
    @DisplayName("Property Tests")
    class PropertyTests {

        @Test
        @DisplayName("Should return correct async status")
        void shouldReturnCorrectAsyncStatus() {
            EventSubscriber syncSubscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );
            EventSubscriber asyncSubscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    true, 0, "", false
            );

            assertFalse(syncSubscriber.isAsync());
            assertTrue(asyncSubscriber.isAsync());
        }

        @Test
        @DisplayName("Should return correct priority")
        void shouldReturnCorrectPriority() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 10, "", false
            );

            assertEquals(10, subscriber.getPriority());
        }

        @Test
        @DisplayName("Should detect when filter is present")
        void shouldDetectWhenFilterIsPresent() {
            EventSubscriber withFilter = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "event.message == 'test'", false
            );
            EventSubscriber withoutFilter = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertTrue(withFilter.hasFilter());
            assertFalse(withoutFilter.hasFilter());
        }
    }

    @Nested
    @DisplayName("CanHandle Tests")
    class CanHandleTests {

        @Test
        @DisplayName("Should handle exact event type")
        void shouldHandleExactEventType() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertTrue(subscriber.canHandle(new TestEvent("test")));
        }

        @Test
        @DisplayName("Should handle child event type")
        void shouldHandleChildEventType() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertTrue(subscriber.canHandle(new ChildEvent("child")));
        }

        @Test
        @DisplayName("Should not handle unrelated event type")
        void shouldNotHandleUnrelatedEventType() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertFalse(subscriber.canHandle(new OtherEvent()));
        }
    }

    @Nested
    @DisplayName("Invoke Tests")
    class InvokeTests {

        @Test
        @DisplayName("Should invoke handler method")
        void shouldInvokeHandlerMethod() throws Throwable {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            subscriber.invoke(new TestEvent("hello"));

            assertTrue(handler.methodCalled);
            assertEquals("hello", handler.receivedMessage);
        }

        @Test
        @DisplayName("Should throw exception from handler")
        void shouldThrowExceptionFromHandler() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, throwingMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertThrows(Exception.class, () ->
                    subscriber.invoke(new TestEvent("test")));
        }
    }

    @Nested
    @DisplayName("CompareTo Tests")
    class CompareToTests {

        @Test
        @DisplayName("Should order by priority descending")
        void shouldOrderByPriorityDescending() {
            EventSubscriber high = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 10, "", false
            );
            EventSubscriber low = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 1, "", false
            );

            assertTrue(high.compareTo(low) < 0); // high comes first
            assertTrue(low.compareTo(high) > 0); // low comes after
        }

        @Test
        @DisplayName("Should consider equal priority as equal")
        void shouldConsiderEqualPriorityAsEqual() {
            EventSubscriber sub1 = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 5, "", false
            );
            EventSubscriber sub2 = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 5, "", false
            );

            assertEquals(0, sub1.compareTo(sub2));
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertEquals(subscriber, subscriber);
        }

        @Test
        @DisplayName("Should be equal to same target and method")
        void shouldBeEqualToSameTargetAndMethod() {
            EventSubscriber sub1 = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );
            EventSubscriber sub2 = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    true, 10, "filter", true
            );

            assertEquals(sub1, sub2);
            assertEquals(sub1.hashCode(), sub2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to different target")
        void shouldNotBeEqualToDifferentTarget() {
            TestHandler otherHandler = new TestHandler();
            EventSubscriber sub1 = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );
            EventSubscriber sub2 = new EventSubscriber(
                    otherHandler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertNotEquals(sub1, sub2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertNotEquals(null, subscriber);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertNotEquals("not a subscriber", subscriber);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should include class name in toString")
        void shouldIncludeClassNameInToString() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 5, "", false
            );

            String str = subscriber.toString();

            assertTrue(str.contains("TestHandler"));
            assertTrue(str.contains("handleEvent"));
            assertTrue(str.contains("TestEvent"));
            assertTrue(str.contains("priority=5"));
        }

        @Test
        @DisplayName("Should show async status in toString")
        void shouldShowAsyncStatusInToString() {
            EventSubscriber asyncSubscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    true, 0, "", false
            );

            String str = asyncSubscriber.toString();

            assertTrue(str.contains("async=true"));
        }
    }

    @Nested
    @DisplayName("Signature Validation Tests")
    class SignatureValidationTests {

        @Test
        @DisplayName("Should return null warning for valid signature")
        void shouldReturnNullWarningForValidSignature() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, handleEventMethod, TestEvent.class,
                    false, 0, "", false
            );

            assertNull(subscriber.getSignatureWarning());
        }

        @Test
        @DisplayName("Should return warning for method with no parameters")
        void shouldReturnWarningForNoParameters() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, noParamsMethod, TestEvent.class,
                    false, 0, "", false
            );

            String warning = subscriber.getSignatureWarning();
            assertNotNull(warning);
            assertTrue(warning.contains("expected exactly one parameter but found 0"));
        }

        @Test
        @DisplayName("Should return warning for method with too many parameters")
        void shouldReturnWarningForTooManyParameters() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, tooManyParamsMethod, TestEvent.class,
                    false, 0, "", false
            );

            String warning = subscriber.getSignatureWarning();
            assertNotNull(warning);
            assertTrue(warning.contains("expected exactly one parameter but found 2"));
        }

        @Test
        @DisplayName("Should return warning for non-Event parameter type")
        void shouldReturnWarningForNonEventParameter() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, nonEventParamMethod, TestEvent.class,
                    false, 0, "", false
            );

            String warning = subscriber.getSignatureWarning();
            assertNotNull(warning);
            assertTrue(warning.contains("does not extend io.github.yasmramos.veld.runtime.event.Event"));
        }

        @Test
        @DisplayName("Should return warning for incompatible event type")
        void shouldReturnWarningForIncompatibleEventType() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, differentEventParamMethod, TestEvent.class,
                    false, 0, "", false
            );

            String warning = subscriber.getSignatureWarning();
            assertNotNull(warning);
            assertTrue(warning.contains("is not compatible with parameter type"));
        }

        @Test
        @DisplayName("Should include method signature in warning message")
        void shouldIncludeMethodSignatureInWarning() {
            EventSubscriber subscriber = new EventSubscriber(
                    handler, nonEventParamMethod, TestEvent.class,
                    false, 0, "", false
            );

            String warning = subscriber.getSignatureWarning();
            assertNotNull(warning);
            assertTrue(warning.contains("TestHandler#nonEventParam"));
            assertTrue(warning.contains("String"));
        }

        @Test
        @DisplayName("Functional interface constructor should have null warning")
        void functionalInterfaceShouldHaveNullWarning() throws Exception {
            MethodHandle handle = LOOKUP.unreflect(handleEventMethod).bindTo(handler);
            EventSubscriber subscriber = new EventSubscriber(
                    handler, "handleEvent", handle, TestEvent.class,
                    false, 0, "", false
            );

            // Functional interface constructor sets signatureWarning to null
            assertNull(subscriber.getSignatureWarning());
        }
    }
}
