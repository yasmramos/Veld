package io.github.yasmramos.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

class ComponentFactoryTest {

    @Test
    void testDefaultMethods() {
        ComponentFactory<String> factory = new ComponentFactory<>() {
            @Override public int getIndex() { return 0; }
            @Override public String getName() { return "test"; }
            @Override public Class<String> getType() { return String.class; }
            @Override public Scope getScope() { return Scope.SINGLETON; }
            @Override public boolean isLazy() { return false; }
            @Override public String create() { return "value"; }
            @Override public void invokePostConstruct(String instance) {}
            @Override public void invokePreDestroy(String instance) {}
        };

        assertFalse(factory.isPrimary());
        assertNull(factory.getQualifier());
        assertArrayEquals(new Class<?>[0], factory.getInterfaces());
        assertArrayEquals(new String[0], factory.getProfiles());
        assertTrue(factory.getConditions().isEmpty());
    }

    @Test
    void testLifecycleCallbacks() {
        AtomicBoolean postConstructCalled = new AtomicBoolean(false);
        AtomicBoolean preDestroyCalled = new AtomicBoolean(false);

        ComponentFactory<String> factory = new ComponentFactory<>() {
            @Override public int getIndex() { return 0; }
            @Override public String getName() { return "test"; }
            @Override public Class<String> getType() { return String.class; }
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
            @Override public String getName() { return "primary"; }
            @Override public Class<String> getType() { return String.class; }
            @Override public Scope getScope() { return Scope.SINGLETON; }
            @Override public boolean isLazy() { return false; }
            @Override public String create() { return "primary"; }
            @Override public void invokePostConstruct(String instance) {}
            @Override public void invokePreDestroy(String instance) {}
            @Override public boolean isPrimary() { return true; }
        };

        assertTrue(factory.isPrimary());
    }

    @Test
    void testQualifiedFactory() {
        ComponentFactory<String> factory = new ComponentFactory<>() {
            @Override public int getIndex() { return 0; }
            @Override public String getName() { return "qualified"; }
            @Override public Class<String> getType() { return String.class; }
            @Override public Scope getScope() { return Scope.SINGLETON; }
            @Override public boolean isLazy() { return false; }
            @Override public String create() { return "qualified"; }
            @Override public void invokePostConstruct(String instance) {}
            @Override public void invokePreDestroy(String instance) {}
            @Override public String getQualifier() { return "myQualifier"; }
        };

        assertEquals("myQualifier", factory.getQualifier());
    }
}
