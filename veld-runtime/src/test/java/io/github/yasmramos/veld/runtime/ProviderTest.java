package io.github.yasmramos.veld.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

class ProviderTest {

    @Test
    void testProviderAsLambda() {
        Provider<String> provider = () -> "Hello";
        assertEquals("Hello", provider.get());
    }

    @Test
    void testProviderMultipleCalls() {
        AtomicInteger counter = new AtomicInteger(0);
        Provider<Integer> provider = counter::incrementAndGet;
        
        assertEquals(1, provider.get());
        assertEquals(2, provider.get());
        assertEquals(3, provider.get());
    }

    @Test
    void testProviderWithNullValue() {
        Provider<String> provider = () -> null;
        assertNull(provider.get());
    }

    @Test
    void testProviderWithException() {
        Provider<String> provider = () -> {
            throw new VeldException("Test error");
        };
        
        assertThrows(VeldException.class, provider::get);
    }

    @Test
    void testProviderLazyInitialization() {
        AtomicInteger initCount = new AtomicInteger(0);
        
        Provider<String> provider = () -> {
            initCount.incrementAndGet();
            return "Lazy";
        };
        
        assertEquals(0, initCount.get());
        assertEquals("Lazy", provider.get());
        assertEquals(1, initCount.get());
    }

    @Test
    void testSingletonBehavior() {
        String singleton = "Singleton";
        Provider<String> provider = () -> singleton;
        assertSame(provider.get(), provider.get());
    }

    @Test
    void testPrototypeBehavior() {
        Provider<Object> provider = Object::new;
        assertNotSame(provider.get(), provider.get());
    }
}
