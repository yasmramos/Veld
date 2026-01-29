package io.github.yasmramos.veld.processor;

/**
 * Categorizes dependencies based on their availability requirements.
 * This enum is used to clearly separate the logic for required, optional, and conditional dependencies.
 * 
 * <p>Dependency categories:</p>
 * <ul>
 *   <li>{@link #REQUIRED} - Dependencies that MUST be present at runtime.
 *     Generated code will throw IllegalStateException if not available.</li>
 *   <li>{@link #OPTIONAL} - Dependencies marked with @Optional or wrapped in Optional&lt;T&gt;.
 *     May be null at runtime, caller must handle null case.</li>
 *   <li>{@link #CONDITIONAL} - Dependencies that depend on conditions (e.g., @ConditionalOnProperty).
 *     Existence is determined at compile-time, may not exist in all profiles.</li>
 *   <li>{@link #VALUE} - Dependencies injected via @Value annotation.
 *     Not a bean dependency, resolved from configuration.</li>
 *   <li>{@link #PROVIDER} - Dependencies wrapped in Provider&lt;T&gt;.
 *     Allows lazy lookup of the actual bean.</li>
 * </ul>
 */
public enum DependencyCategory {
    /**
     * Required dependency - must be present at runtime.
     * Generated code will throw IllegalStateException if not available.
     */
    REQUIRED,
    
    /**
     * Optional dependency - may be null at runtime.
     * Marked with @Optional or wrapped in Optional&lt;T&gt;.
     */
    OPTIONAL,
    
    /**
     * Conditional dependency - existence depends on compile-time conditions.
     * Determined by @ConditionalOnProperty, @ConditionalOnBean, etc.
     */
    CONDITIONAL,
    
    /**
     * Value injection - resolved from configuration, not a bean.
     * @Value annotation, no bean lookup required.
     */
    VALUE,
    
    /**
     * Provider dependency - wrapped in Provider&lt;T&gt;.
     * Allows lazy lookup of the actual bean.
     */
    PROVIDER;
    
    /**
     * Determines the category for a required dependency (not Optional, not Optional wrapper).
     */
    public static DependencyCategory forRequiredDependency() {
        return REQUIRED;
    }
    
    /**
     * Determines the category for an @Optional dependency.
     */
    public static DependencyCategory forOptionalDependency() {
        return OPTIONAL;
    }
    
    /**
     * Determines the category for an Optional&lt;T&gt; wrapper.
     */
    public static DependencyCategory forOptionalWrapper() {
        return OPTIONAL;
    }
    
    /**
     * Determines the category for a Provider&lt;T&gt; dependency.
     */
    public static DependencyCategory forProviderDependency() {
        return PROVIDER;
    }
    
    /**
     * Determines the category for a @Value injection.
     */
    public static DependencyCategory forValueInjection() {
        return VALUE;
    }
    
    /**
     * Returns true if this category represents a dependency that may be missing at runtime.
     */
    public boolean mayBeMissing() {
        return this == OPTIONAL || this == CONDITIONAL;
    }
    
    /**
     * Returns true if this category requires special wrapper handling (Provider or Optional).
     */
    public boolean requiresWrapper() {
        return this == PROVIDER || this == OPTIONAL;
    }
    
    /**
     * Returns a human-readable description of this category.
     */
    public String getDescription() {
        return switch (this) {
            case REQUIRED -> "Required dependency - must be present";
            case OPTIONAL -> "Optional dependency - may be null";
            case CONDITIONAL -> "Conditional dependency - existence determined at compile-time";
            case VALUE -> "Value injection - resolved from configuration";
            case PROVIDER -> "Provider dependency - lazy bean lookup";
        };
    }
}
