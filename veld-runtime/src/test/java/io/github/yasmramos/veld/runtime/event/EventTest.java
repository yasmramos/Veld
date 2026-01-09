package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Event base class.
 */
@DisplayName("Event Tests")
class EventTest {
    
    // Test implementation of abstract Event
    static class TestEvent extends Event {
        private final String message;
        
        TestEvent(Object source, String message) {
            super(source);
            this.message = message;
        }
        
        TestEvent(String message) {
            super();
            this.message = message;
        }
        
        String getMessage() {
            return message;
        }
    }
    
    @Test
    @DisplayName("Should generate unique event ID")
    void shouldGenerateUniqueEventId() {
        TestEvent event1 = new TestEvent("test1");
        TestEvent event2 = new TestEvent("test2");
        
        assertNotNull(event1.getEventId());
        assertNotNull(event2.getEventId());
        assertNotEquals(event1.getEventId(), event2.getEventId());
    }
    
    @Test
    @DisplayName("Should record timestamp")
    void shouldRecordTimestamp() {
        TestEvent event = new TestEvent("test");
        
        assertNotNull(event.getTimestamp());
    }
    
    @Test
    @DisplayName("Should store source")
    void shouldStoreSource() {
        Object source = new Object();
        TestEvent event = new TestEvent(source, "test");
        
        assertSame(source, event.getSource());
    }
    
    @Test
    @DisplayName("Should allow null source")
    void shouldAllowNullSource() {
        TestEvent event = new TestEvent("test");
        
        assertNull(event.getSource());
    }
    
    @Test
    @DisplayName("Should not be cancelled initially")
    void shouldNotBeCancelledInitially() {
        TestEvent event = new TestEvent("test");
        
        assertFalse(event.isCancelled());
    }
    
    @Test
    @DisplayName("Should be cancellable")
    void shouldBeCancellable() {
        TestEvent event = new TestEvent("test");
        
        event.cancel();
        
        assertTrue(event.isCancelled());
    }
    
    @Test
    @DisplayName("Should not be consumed initially")
    void shouldNotBeConsumedInitially() {
        TestEvent event = new TestEvent("test");
        
        assertFalse(event.isConsumed());
    }
    
    @Test
    @DisplayName("Should be consumable")
    void shouldBeConsumable() {
        TestEvent event = new TestEvent("test");
        
        event.consume();
        
        assertTrue(event.isConsumed());
    }
    
    @Test
    @DisplayName("Should return event type name")
    void shouldReturnEventTypeName() {
        TestEvent event = new TestEvent("test");
        
        assertEquals("TestEvent", event.getEventType());
    }
    
    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
        Object source = new Object();
        TestEvent event = new TestEvent(source, "test");
        
        String toString = event.toString();
        
        assertTrue(toString.contains("TestEvent"));
        assertTrue(toString.contains("id="));
        assertTrue(toString.contains("timestamp="));
    }
}
