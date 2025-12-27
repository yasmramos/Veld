package io.github.yasmramos.veld;

import io.github.yasmramos.veld.runtime.ComponentRegistry;
import io.github.yasmramos.veld.runtime.lifecycle.LifecycleProcessor;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.value.ValueResolver;

import java.util.List;
import java.util.function.Supplier;

/**
 * Stub class for compile-time API reference.
 * The actual implementation is generated at compile-time by veld-processor.
 */
public class Veld {
    
    private Veld() {}
    
    public static <T> T get(Class<T> type) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static <T> T get(Class<T> type, String name) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static <T> List<T> getAll(Class<T> type) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static <T> Supplier<T> getProvider(Class<T> type) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static boolean contains(Class<?> type) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static int componentCount() {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static LifecycleProcessor getLifecycleProcessor() {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static ComponentRegistry getRegistry() {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static EventBus getEventBus() {
        return EventBus.getInstance();
    }
    
    public static ValueResolver getValueResolver() {
        return ValueResolver.getInstance();
    }
    
    public static String resolveValue(String expression) {
        return ValueResolver.getInstance().resolve(expression);
    }
    
    public static <T> T resolveValue(String expression, Class<T> type) {
        return ValueResolver.getInstance().resolve(expression, type);
    }
    
    public static void setActiveProfiles(String... profiles) {
        // No-op in stub
    }
    
    public static boolean isProfileActive(String profile) {
        return false;
    }
    
    public static String[] getActiveProfiles() {
        return new String[0];
    }
    
    public static void shutdown() {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static class Annotations {
        private Annotations() {}
    }
}
