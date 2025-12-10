package io.github.yasmramos;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Main entry point for Veld dependency injection.
 * 
 * Delegates to the generated implementation at compile-time.
 */
public final class Veld {
    
    private static final String GENERATED_CLASS = "io.github.yasmramos.generated.Veld";
    private static Class<?> generatedVeld;
    private static boolean initialized = false;
    
    private Veld() {}
    
    private static void ensureInitialized() {
        if (!initialized) {
            try {
                generatedVeld = Class.forName(GENERATED_CLASS);
                initialized = true;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                    "Veld not initialized. Ensure veld-processor is in annotation processor path.");
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        ensureInitialized();
        try {
            Method method = generatedVeld.getMethod("get", Class.class);
            return (T) method.invoke(null, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get component: " + type.getName(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> List<T> getAll(Class<T> type) {
        ensureInitialized();
        try {
            Method method = generatedVeld.getMethod("getAll", Class.class);
            return (List<T>) method.invoke(null, type);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    public static boolean contains(Class<?> type) {
        ensureInitialized();
        try {
            Method method = generatedVeld.getMethod("contains", Class.class);
            return (Boolean) method.invoke(null, type);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static int componentCount() {
        ensureInitialized();
        try {
            Method method = generatedVeld.getMethod("componentCount");
            return (Integer) method.invoke(null);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public static void shutdown() {
        if (initialized && generatedVeld != null) {
            try {
                Method method = generatedVeld.getMethod("shutdown");
                method.invoke(null);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
