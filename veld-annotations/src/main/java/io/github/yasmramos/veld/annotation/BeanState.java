package io.github.yasmramos.veld.annotation;

/**
 * Represents the possible states of a bean in the Veld dependency injection container.
 * 
 * <p>The bean lifecycle progresses through states as follows:</p>
 * <pre>
 * DECLARED → CREATED → USABLE → DESTROYING → DESTROYED
 *                ↓
 *           (error state)
 * </pre>
 * 
 * <p>State descriptions:</p>
 * <ul>
 *   <li>{@link #DECLARED} - Bean is defined in the dependency graph, but no instance exists yet.
 *     This is the initial state for all beans at compile-time.</li>
 *   <li>{@link #CREATED} - Bean instance has been created (constructor called), but dependencies
 *     may not be injected and @PostConstruct may not have been called yet.</li>
 *   <li>{@link #USABLE} - Bean is fully initialized: instance created, dependencies injected,
 *     and @PostConstruct called. Bean is ready for use.</li>
 *   <li>{@link #DESTROYING} - Bean is in the process of being destroyed. @PreDestroy called
 *     and resources being released. Transition state between USABLE and DESTROYED.</li>
 *   <li>{@link #DESTROYED} - Bean has been destroyed: @PreDestroy called and resources released.
 *     This is a terminal state.</li>
 *   <li>{@link #CREATION_FAILED} - Bean creation failed due to an error. This is an error state.</li>
 * </ul>
 * 
 * <p>State transition rules:</p>
 * <ul>
 *   <li>DECLARED → CREATED: When the bean instance is first created</li>
 *   <li>CREATED → USABLE: After all dependencies are injected and @PostConstruct completes</li>
 *   <li>USABLE → DESTROYING: When shutdown() is initiated</li>
 *   <li>DESTROYING → DESTROYED: When @PreDestroy completes successfully</li>
 *   <li>Any state → CREATION_FAILED: If an exception occurs during creation</li>
 * </ul>
 */
public enum BeanState {
    /**
     * Bean is defined in the dependency graph but no instance exists.
     * Initial state for all beans at compile-time.
     */
    DECLARED(false, "Bean declared in dependency graph"),
    
    /**
     * Bean instance created but not yet fully initialized.
     * Constructor called, but dependencies may not be injected.
     */
    CREATED(false, "Bean instance created"),
    
    /**
     * Bean is fully initialized and ready for use.
     * All dependencies injected, @PostConstruct completed successfully.
     */
    USABLE(true, "Bean fully initialized and usable"),
    
    /**
     * Bean is in the process of being destroyed.
     * @PreDestroy called, close() invoked, but destruction not yet complete.
     * Transition state between USABLE and DESTROYED.
     */
    DESTROYING(false, "Bean destruction in progress"),
    
    /**
     * Bean has been destroyed.
     * @PreDestroy called, resources released.
     * Terminal state.
     */
    DESTROYED(false, "Bean destroyed"),
    
    /**
     * Bean creation failed due to an error.
     * Error state - bean cannot be used.
     */
    CREATION_FAILED(false, "Bean creation failed");
    
    private final boolean isUsable;
    private final String description;
    
    BeanState(boolean isUsable, String description) {
        this.isUsable = isUsable;
        this.description = description;
    }
    
    /**
     * Returns true if the bean is in a usable state and can be accessed.
     */
    public boolean isUsable() {
        return isUsable;
    }
    
    /**
     * Returns true if this state represents a terminal state (no further transitions possible).
     */
    public boolean isTerminal() {
        return this == DESTROYED || this == CREATION_FAILED;
    }
    
    /**
     * Returns true if this state represents an error state.
     */
    public boolean isErrorState() {
        return this == CREATION_FAILED;
    }
    
    /**
     * Returns a human-readable description of this state.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the next state in the normal lifecycle progression.
     * 
     * @return the next state, or null if this is a terminal state
     */
    public BeanState getNextState() {
        return switch (this) {
            case DECLARED -> CREATED;
            case CREATED -> USABLE;
            case USABLE -> DESTROYING;
            case DESTROYING -> DESTROYED;
            case DESTROYED, CREATION_FAILED -> null;
        };
    }
    
    /**
     * Checks if a transition to the given state is valid from this state.
     * 
     * @param target the target state
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(BeanState target) {
        // From DECLARED, can only go to CREATED
        if (this == DECLARED) {
            return target == CREATED;
        }
        // From CREATED, can go to USABLE or CREATION_FAILED
        if (this == CREATED) {
            return target == USABLE || target == CREATION_FAILED;
        }
        // From USABLE, can go to DESTROYING
        if (this == USABLE) {
            return target == DESTROYING;
        }
        // From DESTROYING, can go to DESTROYED
        if (this == DESTROYING) {
            return target == DESTROYED;
        }
        // Terminal states cannot transition
        return false;
    }
    
    /**
     * Gets the state that indicates a failed initialization from a usable state.
     */
    public static BeanState forFailedInitialization() {
        return CREATION_FAILED;
    }
}
