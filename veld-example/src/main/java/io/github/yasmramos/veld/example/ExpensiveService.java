package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Lazy;
import io.github.yasmramos.veld.annotation.PostConstruct;

/**
 * Demonstrates @Lazy initialization.
 * This component will NOT be instantiated when the container starts.
 * It will only be created when first accessed.
 * 
 * Use @Lazy for components that:
 * - Are expensive to create
 * - May not be needed in all execution paths
 * - Have heavy initialization logic
 * 
 * Note: @Lazy alone implies @Component with singleton scope.
 * You can also combine with @Singleton for clarity: @Lazy @Singleton
 */
@Lazy
public class ExpensiveService {
    
    private static int instanceCount = 0;
    
    public ExpensiveService() {
        instanceCount++;
        System.out.println("[ExpensiveService] Constructor called (instance #" + instanceCount + ")");
        // Simulate expensive initialization
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @PostConstruct
    public void init() {
        System.out.println("[ExpensiveService] @PostConstruct - Heavy initialization complete");
    }
    
    public String process(String data) {
        return "Processed: " + data;
    }
    
    public static int getInstanceCount() {
        return instanceCount;
    }
    
    public static void resetInstanceCount() {
        instanceCount = 0;
    }
}
