package io.github.yasmramos.veld.test.context;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.test.mock.MockFactory;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lightweight test container that manages the lifecycle
 * of Veld beans for testing purposes.
 * 
 * <p>This class provides a wrapper around the Veld container
 * with additional functionalities specific to testing:</p>
 * <ul>
 *   <li>Registering mocks before container startup</li>
 *   <li>Lifecycle management (start/stop)</li>
 *   <li>Bean injection in test fields</li>
 *   <li>Access to registered beans and mocks</li>
 * </ul>
 * 
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * TestContext context = TestContextBuilder.create()
 *     .withProfile("test")
 *     .withMock("myMock", mockObject)
 *     .build();
 * 
 * try {
 *     MyService service = context.getBean(MyService.class);
 *     // Run tests...
 * } finally {
 *     context.close();
 * }
 * }</pre>
 * 
 * @author Veld Framework
 * @since 1.0.0
 */
public final class TestContext implements AutoCloseable {
    
    private final Map<Class<?>, Object> mocks;
    private final Map<String, Object> namedMocks;
    private final String activeProfile;
    private boolean closed = false;
    
    private TestContext(Map<Class<?>, Object> mocks, Map<String, Object> namedMocks, String profile) {
        this.mocks = mocks;
        this.namedMocks = namedMocks;
        this.activeProfile = profile;
    }
    
    /**
     * Gets a bean from the container by type.
     * 
     * <p>If the bean is registered as a mock, returns the mock.
     * Otherwise, attempts to get it from the Veld container.
     * If Veld cannot provide the bean (e.g., dependencies are mocked),
     * creates the bean manually using reflection and injects the mocks.</p>
     * 
     * @param <T> bean type
     * @param type bean class
     * @return bean of the specified type
     * @throws IllegalStateException if the context is closed
     * @throws RuntimeException if the bean does not exist
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        checkOpen();
        
        // Check if we have a mock for this type
        if (mocks.containsKey(type)) {
            return (T) mocks.get(type);
        }
        
        // Check if we have a named mock that can be cast
        for (Object mock : namedMocks.values()) {
            if (type.isInstance(mock)) {
                return (T) mock;
            }
        }
        
        // Try to get from Veld container
        T bean = null;
        try {
            bean = Veld.get(type);
        } catch (ExceptionInInitializerError e) {
            // Veld failed to initialize, try manual creation
            return createBeanManually(type);
        } catch (Exception e) {
            // Veld.get() threw an exception, try manual creation
            return createBeanManually(type);
        }
        
        // Bean was created but may have null dependencies for interfaces
        // Check if any field that should have a mock is null
        if (bean != null && hasMocksForDependencies(type)) {
            // Need to create manually with mocks injected
            return createBeanManually(type);
        }
        
        return bean;
    }
    
    /**
     * Checks if there are mocks registered for the dependencies of the given type.
     * 
     * @param type the bean type to check
     * @return true if mocks are registered for some dependencies
     */
    private boolean hasMocksForDependencies(Class<?> type) {
        // Check constructor parameters against mocks
        for (java.lang.reflect.Constructor<?> ctor : type.getDeclaredConstructors()) {
            for (Class<?> paramType : ctor.getParameterTypes()) {
                if (isMockableType(paramType) && hasMockFor(paramType)) {
                    return true;
                }
            }
        }
        
        // Check fields for injection
        for (java.lang.reflect.Field field : type.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                Class<?> fieldType = field.getType();
                if (isMockableType(fieldType) && hasMockFor(fieldType)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a type can be mocked (interface or class with mock).
     * 
     * @param type the type to check
     * @return true if the type is mockable
     */
    private boolean isMockableType(Class<?> type) {
        return type.isInterface() || mocks.containsKey(type) || hasMockFor(type);
    }
    
    /**
     * Checks if we have a mock for the given type (exact or interface match).
     * 
     * @param type the type to check
     * @return true if a mock is available
     */
    private boolean hasMockFor(Class<?> type) {
        // Check exact match
        if (mocks.containsKey(type)) {
            return true;
        }
        
        // Check if any mock implements this interface
        for (Object mock : mocks.values()) {
            if (type.isInstance(mock)) {
                return true;
            }
        }
        
        // Check named mocks
        for (Object mock : namedMocks.values()) {
            if (type.isInstance(mock)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Creates a bean manually using reflection, injecting mocks for dependencies.
     * 
     * @param <T> bean type
     * @param type bean class
     * @return created bean
     */
    @SuppressWarnings("unchecked")
    private <T> T createBeanManually(Class<T> type) {
        try {
            // Find a suitable constructor with all dependencies available as mocks
            for (java.lang.reflect.Constructor<?> ctor : type.getDeclaredConstructors()) {
                Class<?>[] paramTypes = ctor.getParameterTypes();
                Object[] args = new Object[paramTypes.length];
                boolean allArgsAvailable = true;
                
                for (int i = 0; i < paramTypes.length; i++) {
                    // Check for exact type match in mocks
                    if (mocks.containsKey(paramTypes[i])) {
                        args[i] = mocks.get(paramTypes[i]);
                    }
                    // Check for interface match in mocks
                    else {
                        Object mock = findMockForType(paramTypes[i]);
                        if (mock != null) {
                            args[i] = mock;
                        } else {
                            allArgsAvailable = false;
                            break;
                        }
                    }
                }
                
                if (allArgsAvailable) {
                    ctor.setAccessible(true);
                    return (T) ctor.newInstance(args);
                }
            }
            
            throw new TestContextException(
                "Could not find suitable constructor for: " + type.getName() + 
                " (some dependencies are not mocked)");
        } catch (Exception e) {
            if (e instanceof TestContextException) {
                throw (TestContextException) e;
            }
            throw new TestContextException(
                "Could not create bean of type: " + type.getName(), e);
        }
    }
    
    /**
     * Finds a mock that is assignable to the given type.
     * 
     * @param type required type
     * @return mock instance or null if not found
     */
    private Object findMockForType(Class<?> type) {
        // Check exact match
        if (mocks.containsKey(type)) {
            return mocks.get(type);
        }
        
        // Check for assignable types (interfaces, superclasses)
        for (Object mock : mocks.values()) {
            if (type.isInstance(mock)) {
                return mock;
            }
        }
        
        // Check named mocks by type
        for (Object mock : namedMocks.values()) {
            if (type.isInstance(mock)) {
                return mock;
            }
        }
        
        return null;
    }
    
    /**
     * Gets a bean optionally.
     * 
     * @param <T> bean type
     * @param type bean class
     * @return Optional with the bean or empty if it does not exist
     */
    public <T> Optional<T> findBean(Class<T> type) {
        checkOpen();
        try {
            T bean = Veld.get(type);
            return Optional.ofNullable(bean);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Resets all registered mocks.
     * 
     * <p>This method is useful for clearing the mock state
     * between tests without restarting the entire container.</p>
     */
    public void resetMocks() {
        mocks.values().forEach(Mockito::reset);
        namedMocks.values().forEach(Mockito::reset);
    }
    
    /**
     * Injects beans into annotated fields of an object.
     * 
     * @param instance object to inject
     * @param <T> object type
     * @return the object with injected fields
     */
    public <T> T injectFields(T instance) {
        checkOpen();
        
        Class<?> clazz = instance.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                injectField(instance, field);
            }
            clazz = clazz.getSuperclass();
        }
        
        return instance;
    }
    
    private <T> void injectField(T instance, Field field) {
        field.setAccessible(true);
        
        Class<?> fieldType = field.getType();
        
        // Check if a mock exists for this type
        if (mocks.containsKey(fieldType)) {
            try {
                field.set(instance, mocks.get(fieldType));
                return;
            } catch (IllegalAccessException e) {
                throw new TestContextException(
                    "Could not inject mock into field: " + field.getName(), e);
            }
        }
        
        // Check if a named mock matches the field
        String fieldName = field.getName();
        if (namedMocks.containsKey(fieldName)) {
            try {
                Object mock = namedMocks.get(fieldName);
                if (fieldType.isInstance(mock)) {
                    field.set(instance, mock);
                    return;
                }
            } catch (IllegalAccessException e) {
                throw new TestContextException(
                    "Could not inject mock into field: " + field.getName(), e);
            }
        }
        
        // Try to get bean by type from Veld container
        try {
            Object bean = Veld.get(fieldType);
            field.set(instance, bean);
        } catch (Exception e) {
            // Optional or non-injectable field, ignore
        }
    }
    
    /**
     * Checks if the context is open.
     */
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException(
                "TestContext is closed and cannot be used");
        }
    }
    
    /**
     * Closes the context and frees resources.
     * 
     * <p>This method clears all registered mocks.</p>
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            mocks.clear();
            namedMocks.clear();
        }
    }
    
    /**
     * Gets the active profile of the context.
     * 
     * @return active profile name
     */
    public String getActiveProfile() {
        return activeProfile;
    }
    
    /**
     * Checks if a mock exists for the specified type.
     * 
     * @param type type to check
     * @return true if a mock exists for the type
     */
    public boolean hasMock(Class<?> type) {
        return mocks.containsKey(type);
    }
    
    /**
     * Gets the mock for the specified type.
     * 
     * @param <T> mock type
     * @param type mock class
     * @return Optional with the mock or empty if it does not exist
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMock(Class<T> type) {
        return Optional.ofNullable((T) mocks.get(type));
    }
    
    /**
     * Builder for creating TestContext instances.
     */
    public static final class Builder {
        private final Map<Class<?>, Object> mocks = new HashMap<>();
        private final Map<String, Object> namedMocks = new HashMap<>();
        private final Map<String, String> properties = new HashMap<>();
        private String profile = "test";
        
        private Builder() {}
        
        /**
         * Creates a new builder.
         * 
         * @return configured builder
         */
        public static Builder create() {
            return new Builder();
        }
        
        /**
         * Sets the test profile.
         * 
         * @param profile profile name
         * @return this builder
         */
        public Builder withProfile(String profile) {
            this.profile = profile;
            return this;
        }
        
        /**
         * Adds system properties.
         * 
         * @param key property key
         * @param value property value
         * @return this builder
         */
        public Builder withProperty(String key, String value) {
            this.properties.put(key, value);
            return this;
        }
        
        /**
         * Adds multiple properties.
         * 
         * @param props properties in "key=value" format
         * @return this builder
         */
        public Builder withProperties(String... props) {
            for (String prop : props) {
                String[] parts = prop.split("=", 2);
                if (parts.length == 2) {
                    withProperty(parts[0], parts[1]);
                }
            }
            return this;
        }
        
        /**
         * Registers a mock for a specific type.
         * 
         * @param type mock type
         * @param mock mock instance
         * @param <T> mock type
         * @return this builder
         */
        public <T> Builder withMock(Class<T> type, T mock) {
            this.mocks.put(type, mock);
            return this;
        }
        
        /**
         * Registers a mock for a specific type (accepts wildcards).
         * 
         * @param type mock type
         * @param mock mock instance
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public Builder withMockRaw(Class<?> type, Object mock) {
            this.mocks.put(type, mock);
            return this;
        }
        
        /**
         * Registers a mock with a specific name.
         * 
         * @param name mock name
         * @param mock mock instance
         * @return this builder
         */
        public Builder withMock(String name, Object mock) {
            this.namedMocks.put(name, mock);
            return this;
        }
        
        /**
         * Creates and registers a mock for a specific type.
         * 
         * @param type mock type
         * @param <T> mock type
         * @return created mock
         */
        public <T> T withAutoMock(Class<T> type) {
            T mock = MockFactory.createMock(type);
            this.mocks.put(type, mock);
            return mock;
        }
        
        /**
         * Builds the TestContext.
         * 
         * @return configured test context
         */
        public TestContext build() {
            // Configure system properties
            properties.forEach(System::setProperty);
            
            // Configure profile in Veld
            Veld.setActiveProfiles(profile);
            
            // The builder returns a context that handles mocks manually
            return new TestContext(new HashMap<>(mocks), 
                                  new HashMap<>(namedMocks), profile);
        }
    }
    
    /**
     * Exception specific for test context errors.
     */
    public static class TestContextException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public TestContextException(String message) {
            super(message);
        }
        
        public TestContextException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
