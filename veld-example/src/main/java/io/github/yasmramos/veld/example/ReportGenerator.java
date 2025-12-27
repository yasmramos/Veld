package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.runtime.Provider;

/**
 * Demonstrates Provider<T> injection.
 * 
 * Provider<T> is useful when you:
 * - Need to create multiple instances of a @Prototype component
 * - Want lazy initialization without @Lazy
 * - Need to break circular dependencies
 * - Want to defer creation to runtime decision points
 * 
 * Note: @Singleton implies @Component, so we don't need both.
 */
@Singleton
public class ReportGenerator {
    
    // Inject a Provider instead of the service directly
    // This allows us to get fresh instances of RequestContext (which is @Prototype)
    private final Provider<RequestContext> requestContextProvider;
    
    @Inject
    public ReportGenerator(Provider<RequestContext> requestContextProvider) {
        this.requestContextProvider = requestContextProvider;
        System.out.println("[ReportGenerator] Created with Provider<RequestContext>");
    }
    
    @PostConstruct
    public void init() {
        System.out.println("[ReportGenerator] @PostConstruct - Ready to generate reports");
    }
    
    /**
     * Each call gets a fresh RequestContext (because RequestContext is @Prototype)
     */
    public String generateReport(String reportType) {
        // Get a new RequestContext for each report
        RequestContext context = requestContextProvider.get();
        
        return String.format("Report [%s] generated with context ID: %s", 
                reportType, context.getRequestId());
    }
    
    /**
     * Demonstrates getting multiple instances
     */
    public void demonstrateMultipleContexts() {
        System.out.println("\n→ Demonstrating Provider<RequestContext> - each call gets new instance:");
        
        RequestContext ctx1 = requestContextProvider.get();
        RequestContext ctx2 = requestContextProvider.get();
        RequestContext ctx3 = requestContextProvider.get();
        
        System.out.println("  Context 1 ID: " + ctx1.getRequestId());
        System.out.println("  Context 2 ID: " + ctx2.getRequestId());
        System.out.println("  Context 3 ID: " + ctx3.getRequestId());
        System.out.println("  All different instances: " + (ctx1 != ctx2 && ctx2 != ctx3));
    }
}
