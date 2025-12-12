package io.github.yasmramos.veld.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

class ComponentFactoryTest {

    @Test
    void testDefaultMethods() {
        ComponentFactory<String> factory = new ComponentFactory<>() {
            @Override public int getIndex() { return 0; }
            @Override public String getComponentName() { return "test"; }
            @Override public Class<String> getComponentType() { return String.class; }
            @Override public Scope getScope() { return Scope.SINGLETON; }
            @Override public boolean isLazy() { return false; }
            @Override public String create() { return "value"; }
            @Override public void invokePostConstruct(String instance) {}
            @Override public void invokePreDestroy(String instance) {}
        };

        assertFalse(factory.hasConditions());
        assertTrue(factory.evaluateConditions(null));
        assertEquals(0, factory.getImplementedInterfaces().size());
    }

    @Test
    void testLifecycleCallbacks() {
        AtomicBoolean postConstructCalled = new AtomicBoolean(false);
        AtomicBoolean preDestroyCalled = new AtomicBoolean(false);

        ComponentFactory<String> factory = new ComponentFactory<>() {
            @Override public int getIndex() { return 0; }
            @Override public String getComponentName() { return "test"; }
            @Override public Class<String> getComponentType() { return String.class; }
            @Override public Scope getScope() { return Scope.SINGLETON; }
            @Override public boolean isLazy() { return false; }
            @Override public String create() { return "value"; }
            @Override public void invokePostConstruct(String instance) {
                postConstructCalled.set(true);
            }
            @Override public void invokePreDestroy(String instance) {
                preDestroyCalled.set(true);
            }
        };

        String instance = factory.create();
        factory.invokePostConstruct(instance);
        factory.invokePreDestroy(instance);

        assertTrue(postConstructCalled.get());
        assertTrue(preDestroyCalled.get());
    }

    @Test
    void testPrimaryFactory() {
        ComponentFactory<String> factory = new ComponentFactory<>() {
            @Override public int getIndex() { return 0; }
            @Override public String getComponentName() { return "primary"; }
            @Override public Class<String> getComponentType() { return String.class; }
            @Override public Scope getScope() { return Scope.SINGLETON; }
            @Override public boolean isLazy() { return false; }
            @Override public String create() { return "primary"; }
            @Override public void invokePostConstruct(String instance) {}
            @Override public void invokePreDestroy(String instance) {}
        };

        // Primary functionality is not part of ComponentFactory interface
        // This test validates that a basic factory can be created
        assertEquals("primary", factory.getComponentName());
        assertEquals(String.class, factory.getComponentType());
    }

    @Test
    void testQualifiedFactory() {
        ComponentFactory<String> factory = new ComponentFactory<>() {
            @Override public int getIndex() { return 0; }
            @Override public String getComponentName() { return "qualified"; }
            @Override public Class<String> getComponentType() { return String.class; }
            @Override public Scope getScope() { return Scope.SINGLETON; }
            @Override public boolean isLazy() { return false; }
            @Override public String create() { return "qualified"; }
            @Override public void invokePostConstruct(String instance) {}
            @Override public void invokePreDestroy(String instance) {}
        };

        // Qualifier functionality is not part of ComponentFactory interface
        // This test validates that a basic factory can be created with a specific name
        assertEquals("qualified", factory.getComponentName());
        assertEquals(String.class, factory.getComponentType());
    }
}
