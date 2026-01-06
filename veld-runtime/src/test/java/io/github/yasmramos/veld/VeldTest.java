package io.github.yasmramos.veld;

import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.value.ValueResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Veld facade class.
 */
class VeldTest {

    @Test
    void testGetThrowsVeldException() {
        assertThrows(VeldException.class, () -> Veld.get(String.class));
    }

    @Test
    void testGetWithNameThrowsVeldException() {
        assertThrows(VeldException.class, () -> Veld.get(String.class, "name"));
    }

    @Test
    void testGetAllThrowsVeldException() {
        assertThrows(VeldException.class, () -> Veld.getAll(String.class));
    }

    @Test
    void testGetProviderThrowsVeldException() {
        assertThrows(VeldException.class, () -> Veld.getProvider(String.class));
    }

    @Test
    void testContainsThrowsVeldException() {
        assertThrows(VeldException.class, () -> Veld.contains(String.class));
    }

    @Test
    void testComponentCountThrowsVeldException() {
        assertThrows(VeldException.class, Veld::componentCount);
    }

    @Test
    void testGetLifecycleProcessorThrowsVeldException() {
        assertThrows(VeldException.class, Veld::getLifecycleProcessor);
    }

    @Test
    void testShutdownThrowsVeldException() {
        assertThrows(VeldException.class, Veld::shutdown);
    }

    @Test
    void testGetEventBusReturnsInstance() {
        EventBus eventBus = Veld.getEventBus();
        
        assertNotNull(eventBus);
        assertSame(EventBus.getInstance(), eventBus);
    }

    @Test
    void testResolveValueReturnsResolver() {
        // Test that resolveValue works (uses ValueResolver)
        String result = Veld.resolveValue("${nonexistent:default}");
        
        assertEquals("default", result);
    }

    @Test
    void testResolveValueWithType() {
        System.setProperty("test.int.property", "42");
        try {
            Integer result = Veld.resolveValue("${test.int.property}", Integer.class);
            assertEquals(42, result);
        } finally {
            System.clearProperty("test.int.property");
        }
    }

    @Test
    void testGetValueResolver() {
        ValueResolver resolver = Veld.getValueResolver();
        
        assertNotNull(resolver);
        assertSame(ValueResolver.getInstance(), resolver);
    }

    @Test
    void testSetActiveProfiles() {
        // Should not throw
        assertDoesNotThrow(() -> Veld.setActiveProfiles("dev", "test"));
    }

    @Test
    void testIsProfileActive() {
        // Currently returns false (TODO implementation)
        boolean active = Veld.isProfileActive("production");
        
        assertFalse(active);
    }

    @Test
    void testGetActiveProfiles() {
        // Currently returns empty array (TODO implementation)
        String[] profiles = Veld.getActiveProfiles();
        
        assertNotNull(profiles);
        assertEquals(0, profiles.length);
    }

    @Test
    void testAnnotationsClassExists() {
        // Verify the inner Annotations class exists
        assertNotNull(Veld.Annotations.class);
    }
}
