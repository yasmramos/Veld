package io.github.yasmramos.veld.runtime.condition;

import java.util.HashSet;
import java.util.Set;

/**
 * Context object passed to conditions during evaluation.
 * Provides access to the environment, classpath information, and already-registered beans.
 *
 * @since 1.0.0
 */
public final class ConditionContext {

    private final Set<String> registeredBeanNames;
    private final Set<String> registeredBeanTypes;
    private final ClassLoader classLoader;

    /**
     * Creates a new condition context.
     */
    public ConditionContext() {
        this(null);
    }

    /**
     * Creates a new condition context.
     *
     * @param classLoader the class loader to use for class checks
     */
    public ConditionContext(ClassLoader classLoader) {
        this.registeredBeanNames = new HashSet<>();
        this.registeredBeanTypes = new HashSet<>();
        this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
    }

    /**
     * Registers a bean name as present.
     * Called after a bean passes its condition check.
     *
     * @param name the bean name
     */
    public void registerBeanName(String name) {
        registeredBeanNames.add(name);
    }

    /**
     * Registers a bean type as present.
     * Called after a bean passes its condition check.
     *
     * @param type the fully qualified class name
     */
    public void registerBeanType(String type) {
        registeredBeanTypes.add(type);
    }

    /**
     * Registers interface types that a bean implements.
     *
     * @param interfaces the fully qualified interface names
     */
    public void registerBeanInterfaces(Iterable<String> interfaces) {
        for (String iface : interfaces) {
            registeredBeanTypes.add(iface);
        }
    }

    /**
     * Checks if a bean with the given name is registered.
     *
     * @param name the bean name
     * @return true if a bean with this name exists
     */
    public boolean containsBeanName(String name) {
        return registeredBeanNames.contains(name);
    }

    /**
     * Checks if a bean of the given type is registered.
     *
     * @param type the fully qualified class name
     * @return true if a bean of this type exists
     */
    public boolean containsBeanType(String type) {
        return registeredBeanTypes.contains(type);
    }

    /**
     * Gets a system property or environment variable.
     * Checks system properties first, then environment variables.
     *
     * @param name the property name
     * @return the property value, or null if not found
     */
    public String getProperty(String name) {
        // Check system properties first
        String value = System.getProperty(name);
        if (value != null) {
            return value;
        }

        // Check environment variables
        value = System.getenv(name);
        if (value != null) {
            return value;
        }

        // Try with underscores instead of dots (common for env vars)
        String envName = name.replace('.', '_').toUpperCase();
        return System.getenv(envName);
    }

    /**
     * Checks if a class is present on the classpath.
     *
     * @param className the fully qualified class name
     * @return true if the class is present
     */
    public boolean isClassPresent(String className) {
        try {
            classLoader.loadClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Gets the class loader used for class checks.
     *
     * @return the class loader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public String toString() {
        return "ConditionContext{registeredBeans=" + registeredBeanNames.size() + "}";
    }
}
