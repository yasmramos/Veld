package io.github.yasmramos.veld.runtime;

/**
 * Provides instances of type T.
 * 
 * This interface is compatible with javax.inject.Provider and jakarta.inject.Provider.
 * When you inject a Provider<T> instead of T directly, you get:
 * 
 * <ul>
 *   <li>Lazy initialization - the instance is created only when get() is called</li>
 *   <li>Multiple instances - each call to get() can return a new instance (for @Prototype)</li>
 *   <li>Breaking circular dependencies - by deferring the lookup</li>
 * </ul>
 * 
 * <pre>
 * {@literal @}Component
 * public class MyService {
 *     private final Provider&lt;ExpensiveService&gt; expensiveProvider;
 *     
 *     {@literal @}Inject
 *     public MyService(Provider&lt;ExpensiveService&gt; expensiveProvider) {
 *         this.expensiveProvider = expensiveProvider;
 *     }
 *     
 *     public void doWork() {
 *         // Instance created only when needed
 *         ExpensiveService service = expensiveProvider.get();
 *         service.process();
 *     }
 * }
 * </pre>
 *
 * @param <T> the type of object this provider returns
 */
@FunctionalInterface
public interface Provider<T> {
    
    /**
     * Provides an instance of type T.
     * 
     * For @Singleton components, always returns the same instance.
     * For @Prototype components, returns a new instance each time.
     *
     * @return an instance of T
     * @throws VeldException if the instance cannot be created
     */
    T get();
}
