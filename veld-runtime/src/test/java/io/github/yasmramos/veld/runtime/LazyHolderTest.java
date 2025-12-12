package io.github.yasmramos.veld.runtime;

import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LazyHolder.
 */
@DisplayName("LazyHolder Tests")
class LazyHolderTest {
    
    @Nested
    @DisplayName("Lazy Initialization Tests")
    class LazyInitializationTests {
        
        @Test
        @DisplayName("Should not initialize until accessed")
        void shouldNotInitializeUntilAccessed() {
            AtomicInteger initCount = new AtomicInteger(0);
            Supplier<String> supplier = () -> {
                initCount.incrementAndGet();
                return "value";
            };
            
            LazyHolder<String> holder = new LazyHolder<>(supplier);
            
            assertEquals(0, initCount.get());
        }
        
        @Test
        @DisplayName("Should initialize on first access")
        void shouldInitializeOnFirstAccess() {
            AtomicInteger initCount = new AtomicInteger(0);
            Supplier<String> supplier = () -> {
                initCount.incrementAndGet();
                return "value";
            };
            
            LazyHolder<String> holder = new LazyHolder<>(supplier);
            String result = holder.get();
            
            assertEquals(1, initCount.get());
            assertEquals("value", result);
        }
        
        @Test
        @DisplayName("Should only initialize once")
        void shouldOnlyInitializeOnce() {
            AtomicInteger initCount = new AtomicInteger(0);
            Supplier<String> supplier = () -> {
                initCount.incrementAndGet();
                return "value";
            };
            
            LazyHolder<String> holder = new LazyHolder<>(supplier);
            holder.get();
            holder.get();
            holder.get();
            
            assertEquals(1, initCount.get());
        }
        
        @Test
        @DisplayName("Should return same instance")
        void shouldReturnSameInstance() {
            LazyHolder<Object> holder = new LazyHolder<>(Object::new);
            
            Object first = holder.get();
            Object second = holder.get();
            
            assertSame(first, second);
        }
    }
    
    @Nested
    @DisplayName("Initialized Check Tests")
    class InitializedCheckTests {
        
        @Test
        @DisplayName("Should report not initialized before access")
        void shouldReportNotInitializedBeforeAccess() {
            LazyHolder<String> holder = new LazyHolder<>(() -> "value");
            
            assertFalse(holder.isInitialized());
        }
        
        @Test
        @DisplayName("Should report initialized after access")
        void shouldReportInitializedAfterAccess() {
            LazyHolder<String> holder = new LazyHolder<>(() -> "value");
            holder.get();
            
            assertTrue(holder.isInitialized());
        }
    }
    
    @Nested
    @DisplayName("Null Value Tests")
    class NullValueTests {
        
        @Test
        @DisplayName("Should handle null value")
        void shouldHandleNullValue() {
            LazyHolder<String> holder = new LazyHolder<>(() -> null);
            
            assertNull(holder.get());
            assertTrue(holder.isInitialized());
        }
    }
    
    @Nested
    @DisplayName("Exception Tests")
    class ExceptionTests {
        
        @Test
        @DisplayName("Should propagate supplier exception")
        void shouldPropagateSupplierException() {
            LazyHolder<String> holder = new LazyHolder<>(() -> {
                throw new RuntimeException("Initialization failed");
            });
            
            assertThrows(RuntimeException.class, () -> holder.get());
        }
    }
    
    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("Should be thread safe")
        void shouldBeThreadSafe() throws InterruptedException {
            AtomicInteger initCount = new AtomicInteger(0);
            LazyHolder<String> holder = new LazyHolder<>(() -> {
                initCount.incrementAndGet();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "value";
            });
            
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> holder.get());
            }
            
            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();
            
            assertEquals(1, initCount.get());
        }
    }
}
