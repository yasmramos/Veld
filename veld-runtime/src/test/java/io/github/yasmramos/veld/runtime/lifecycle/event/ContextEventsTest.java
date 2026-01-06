package io.github.yasmramos.veld.runtime.lifecycle.event;

import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Context lifecycle events.
 */
@DisplayName("Context Events Tests")
class ContextEventsTest {

    // Concrete implementation for testing ContextEvent base class
    static class TestContextEvent extends ContextEvent {
        TestContextEvent(Object source) {
            super(source);
        }
    }

    @Nested
    @DisplayName("ContextEvent Base Tests")
    class ContextEventBaseTests {

        @Test
        @DisplayName("Should create event with source")
        void shouldCreateEventWithSource() {
            Object source = new Object();
            TestContextEvent event = new TestContextEvent(source);

            assertSame(source, event.getSource());
        }

        @Test
        @DisplayName("Should have event ID")
        void shouldHaveEventId() {
            TestContextEvent event = new TestContextEvent(this);

            assertNotNull(event.getEventId());
            assertFalse(event.getEventId().isEmpty());
        }

        @Test
        @DisplayName("Should have timestamp")
        void shouldHaveTimestamp() {
            Instant before = Instant.now();
            TestContextEvent event = new TestContextEvent(this);
            Instant after = Instant.now();

            assertNotNull(event.getTimestamp());
            assertFalse(event.getTimestamp().isBefore(before));
            assertFalse(event.getTimestamp().isAfter(after));
        }

        @Test
        @DisplayName("Should format toString correctly")
        void shouldFormatToStringCorrectly() {
            TestContextEvent event = new TestContextEvent(this);
            String str = event.toString();

            assertTrue(str.contains("TestContextEvent"));
            assertTrue(str.contains("source="));
            assertTrue(str.contains("timestamp="));
        }

        @Test
        @DisplayName("Should show null source in toString")
        void shouldShowNullSourceInToString() {
            TestContextEvent event = new TestContextEvent(null);
            String str = event.toString();

            assertTrue(str.contains("source=null"));
        }
    }

    @Nested
    @DisplayName("ContextRefreshedEvent Tests")
    class ContextRefreshedEventTests {

        @Test
        @DisplayName("Should create with source, bean count and init time")
        void shouldCreateWithSourceBeanCountAndInitTime() {
            Object source = new Object();

            ContextRefreshedEvent event = new ContextRefreshedEvent(source, 25, 150);

            assertSame(source, event.getSource());
            assertEquals(25, event.getBeanCount());
            assertEquals(150, event.getInitializationTimeMs());
        }

        @Test
        @DisplayName("Should have timestamp")
        void shouldHaveTimestamp() {
            ContextRefreshedEvent event = new ContextRefreshedEvent(this, 10, 100);

            assertNotNull(event.getTimestamp());
        }

        @Test
        @DisplayName("Should handle zero bean count")
        void shouldHandleZeroBeanCount() {
            ContextRefreshedEvent event = new ContextRefreshedEvent(this, 0, 50);

            assertEquals(0, event.getBeanCount());
        }

        @Test
        @DisplayName("Should handle zero initialization time")
        void shouldHandleZeroInitializationTime() {
            ContextRefreshedEvent event = new ContextRefreshedEvent(this, 10, 0);

            assertEquals(0, event.getInitializationTimeMs());
        }

        @Test
        @DisplayName("Should have meaningful toString")
        void shouldHaveMeaningfulToString() {
            ContextRefreshedEvent event = new ContextRefreshedEvent(this, 50, 200);

            String toString = event.toString();

            assertTrue(toString.contains("ContextRefreshedEvent"));
            assertTrue(toString.contains("beanCount=50"));
            assertTrue(toString.contains("initializationTimeMs=200"));
        }

        @Test
        @DisplayName("Should have unique event ID")
        void shouldHaveUniqueEventId() {
            ContextRefreshedEvent event1 = new ContextRefreshedEvent(this, 10, 100);
            ContextRefreshedEvent event2 = new ContextRefreshedEvent(this, 10, 100);

            assertNotEquals(event1.getEventId(), event2.getEventId());
        }
    }

    @Nested
    @DisplayName("ContextStartedEvent Tests")
    class ContextStartedEventTests {
        
        @Test
        @DisplayName("Should create with source and lifecycle count")
        void shouldCreateWithSourceAndLifecycleCount() {
            Object source = new Object();
            
            ContextStartedEvent event = new ContextStartedEvent(source, 5);
            
            assertSame(source, event.getSource());
            assertEquals(5, event.getLifecycleBeanCount());
        }
        
        @Test
        @DisplayName("Should have timestamp")
        void shouldHaveTimestamp() {
            ContextStartedEvent event = new ContextStartedEvent(this, 0);
            
            assertNotNull(event.getTimestamp());
        }
        
        @Test
        @DisplayName("Should have meaningful toString")
        void shouldHaveMeaningfulToString() {
            ContextStartedEvent event = new ContextStartedEvent(this, 3);
            
            String toString = event.toString();
            
            assertTrue(toString.contains("ContextStartedEvent"));
            assertTrue(toString.contains("lifecycleBeanCount=3"));
        }
    }
    
    @Nested
    @DisplayName("ContextStoppedEvent Tests")
    class ContextStoppedEventTests {
        
        @Test
        @DisplayName("Should create with source and lifecycle count")
        void shouldCreateWithSourceAndLifecycleCount() {
            Object source = new Object();
            
            ContextStoppedEvent event = new ContextStoppedEvent(source, 10);
            
            assertSame(source, event.getSource());
            assertEquals(10, event.getLifecycleBeanCount());
        }
        
        @Test
        @DisplayName("Should have timestamp")
        void shouldHaveTimestamp() {
            ContextStoppedEvent event = new ContextStoppedEvent(this, 0);
            
            assertNotNull(event.getTimestamp());
        }
        
        @Test
        @DisplayName("Should have meaningful toString")
        void shouldHaveMeaningfulToString() {
            ContextStoppedEvent event = new ContextStoppedEvent(this, 7);
            
            String toString = event.toString();
            
            assertTrue(toString.contains("ContextStoppedEvent"));
            assertTrue(toString.contains("lifecycleBeanCount=7"));
        }
    }
    
    @Nested
    @DisplayName("ContextClosedEvent Tests")
    class ContextClosedEventTests {
        
        @Test
        @DisplayName("Should create with source, uptime and destroyed count")
        void shouldCreateWithSourceUptimeAndDestroyedCount() {
            Object source = new Object();
            Duration uptime = Duration.ofMinutes(30);
            
            ContextClosedEvent event = new ContextClosedEvent(source, uptime, 15);
            
            assertSame(source, event.getSource());
            assertEquals(uptime, event.getUptime());
            assertEquals(15, event.getDestroyedBeanCount());
        }
        
        @Test
        @DisplayName("Should have timestamp")
        void shouldHaveTimestamp() {
            ContextClosedEvent event = new ContextClosedEvent(this, Duration.ZERO, 0);
            
            assertNotNull(event.getTimestamp());
        }
        
        @Test
        @DisplayName("Should have meaningful toString")
        void shouldHaveMeaningfulToString() {
            ContextClosedEvent event = new ContextClosedEvent(this, Duration.ofHours(1), 20);
            
            String toString = event.toString();
            
            assertTrue(toString.contains("ContextClosedEvent"));
            assertTrue(toString.contains("destroyedBeanCount=20"));
            assertTrue(toString.contains("uptime="));
        }

        @Test
        @DisplayName("Should handle zero uptime")
        void shouldHandleZeroUptime() {
            ContextClosedEvent event = new ContextClosedEvent(this, Duration.ZERO, 5);

            assertEquals(Duration.ZERO, event.getUptime());
        }

        @Test
        @DisplayName("Should handle long uptime")
        void shouldHandleLongUptime() {
            Duration longUptime = Duration.ofDays(30).plusHours(12);
            ContextClosedEvent event = new ContextClosedEvent(this, longUptime, 100);

            assertEquals(longUptime, event.getUptime());
        }

        @Test
        @DisplayName("Should have unique event ID")
        void shouldHaveUniqueEventId() {
            ContextClosedEvent event1 = new ContextClosedEvent(this, Duration.ZERO, 0);
            ContextClosedEvent event2 = new ContextClosedEvent(this, Duration.ZERO, 0);

            assertNotEquals(event1.getEventId(), event2.getEventId());
        }
    }

    @Nested
    @DisplayName("Event Inheritance Tests")
    class EventInheritanceTests {

        @Test
        @DisplayName("ContextRefreshedEvent should extend ContextEvent")
        void contextRefreshedEventShouldExtendContextEvent() {
            ContextRefreshedEvent event = new ContextRefreshedEvent(this, 10, 100);

            assertTrue(event instanceof ContextEvent);
        }

        @Test
        @DisplayName("ContextStartedEvent should extend ContextEvent")
        void contextStartedEventShouldExtendContextEvent() {
            ContextStartedEvent event = new ContextStartedEvent(this, 5);

            assertTrue(event instanceof ContextEvent);
        }

        @Test
        @DisplayName("ContextStoppedEvent should extend ContextEvent")
        void contextStoppedEventShouldExtendContextEvent() {
            ContextStoppedEvent event = new ContextStoppedEvent(this, 5);

            assertTrue(event instanceof ContextEvent);
        }

        @Test
        @DisplayName("ContextClosedEvent should extend ContextEvent")
        void contextClosedEventShouldExtendContextEvent() {
            ContextClosedEvent event = new ContextClosedEvent(this, Duration.ZERO, 5);

            assertTrue(event instanceof ContextEvent);
        }

        @Test
        @DisplayName("All context events should extend Event")
        void allContextEventsShouldExtendEvent() {
            assertTrue(new ContextRefreshedEvent(this, 0, 0) instanceof io.github.yasmramos.veld.runtime.event.Event);
            assertTrue(new ContextStartedEvent(this, 0) instanceof io.github.yasmramos.veld.runtime.event.Event);
            assertTrue(new ContextStoppedEvent(this, 0) instanceof io.github.yasmramos.veld.runtime.event.Event);
            assertTrue(new ContextClosedEvent(this, Duration.ZERO, 0) instanceof io.github.yasmramos.veld.runtime.event.Event);
        }
    }

    @Nested
    @DisplayName("Event State Tests")
    class EventStateTests {

        @Test
        @DisplayName("Context events should support cancellation")
        void contextEventsShouldSupportCancellation() {
            ContextRefreshedEvent event = new ContextRefreshedEvent(this, 10, 100);

            assertFalse(event.isCancelled());
            event.cancel();
            assertTrue(event.isCancelled());
        }

        @Test
        @DisplayName("Context events should support consumption")
        void contextEventsShouldSupportConsumption() {
            ContextStartedEvent event = new ContextStartedEvent(this, 5);

            assertFalse(event.isConsumed());
            event.consume();
            assertTrue(event.isConsumed());
        }

        @Test
        @DisplayName("Should return correct event type")
        void shouldReturnCorrectEventType() {
            assertEquals("ContextRefreshedEvent", new ContextRefreshedEvent(this, 0, 0).getEventType());
            assertEquals("ContextStartedEvent", new ContextStartedEvent(this, 0).getEventType());
            assertEquals("ContextStoppedEvent", new ContextStoppedEvent(this, 0).getEventType());
            assertEquals("ContextClosedEvent", new ContextClosedEvent(this, Duration.ZERO, 0).getEventType());
        }
    }
}
