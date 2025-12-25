package io.github.yasmramos.veld.boot.example.service.veld;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.boot.example.service.spring.TodoBusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Veld component that demonstrates Spring-Veld integration.
 * This component injects a Spring-managed service (TodoBusinessService)
 * and provides Veld-specific functionality.
 * 
 * Demonstrates: Spring -> Veld bean bridging
 */
@Component("todoAnalysisService")
@Singleton
public class TodoAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(TodoAnalysisService.class);
    
    // Spring-managed service injected into Veld component
    @Inject
    private TodoBusinessService springTodoService;
    
    @PostConstruct
    public void init() {
        logger.info("TodoAnalysisService initialized with Spring TodoBusinessService: {}", 
                   springTodoService.getClass().getSimpleName());
    }
    
    /**
     * Analyze todo completion patterns
     */
    public CompletionAnalysis analyzeUserCompletion(Long userId) {
        TodoBusinessService.TodoStatistics stats = springTodoService.getTodoStatistics(userId);
        
        return new CompletionAnalysis(
            stats.getTotal(),
            stats.getCompleted(),
            stats.getPending(),
            stats.getCompletionRate()
        );
    }
    
    /**
     * Get productivity insights for a user
     */
    public ProductivityInsights getProductivityInsights(Long userId) {
        TodoBusinessService.TodoStatistics stats = springTodoService.getTodoStatistics(userId);
        
        String productivityLevel;
        if (stats.getCompletionRate() >= 0.8) {
            productivityLevel = "HIGH";
        } else if (stats.getCompletionRate() >= 0.6) {
            productivityLevel = "MEDIUM";
        } else {
            productivityLevel = "LOW";
        }
        
        return new ProductivityInsights(
            stats.getCompletionRate(),
            productivityLevel,
            stats.getPending()
        );
    }
    
    /**
     * Get recent activity summary
     */
    public ActivitySummary getRecentActivity() {
        List<io.github.yasmramos.veld.boot.example.domain.Todo> recentTodos = 
            springTodoService.getRecentTodos();
        
        return new ActivitySummary(
            recentTodos.size(),
            recentTodos.stream().mapToInt(t -> t.isCompleted() ? 1 : 0).sum(),
            recentTodos.stream().mapToInt(t -> t.isCompleted() ? 0 : 1).sum()
        );
    }
    
    // DTOs for analysis results
    public static class CompletionAnalysis {
        private final long totalTodos;
        private final long completedTodos;
        private final long pendingTodos;
        private final double completionRate;
        
        public CompletionAnalysis(long totalTodos, long completedTodos, long pendingTodos, double completionRate) {
            this.totalTodos = totalTodos;
            this.completedTodos = completedTodos;
            this.pendingTodos = pendingTodos;
            this.completionRate = completionRate;
        }
        
        // Getters
        public long getTotalTodos() { return totalTodos; }
        public long getCompletedTodos() { return completedTodos; }
        public long getPendingTodos() { return pendingTodos; }
        public double getCompletionRate() { return completionRate; }
    }
    
    public static class ProductivityInsights {
        private final double completionRate;
        private final String productivityLevel;
        private final long pendingCount;
        
        public ProductivityInsights(double completionRate, String productivityLevel, long pendingCount) {
            this.completionRate = completionRate;
            this.productivityLevel = productivityLevel;
            this.pendingCount = pendingCount;
        }
        
        // Getters
        public double getCompletionRate() { return completionRate; }
        public String getProductivityLevel() { return productivityLevel; }
        public long getPendingCount() { return pendingCount; }
    }
    
    public static class ActivitySummary {
        private final int totalRecent;
        private final int completedRecent;
        private final int pendingRecent;
        
        public ActivitySummary(int totalRecent, int completedRecent, int pendingRecent) {
            this.totalRecent = totalRecent;
            this.completedRecent = completedRecent;
            this.pendingRecent = pendingRecent;
        }
        
        // Getters
        public int getTotalRecent() { return totalRecent; }
        public int getCompletedRecent() { return completedRecent; }
        public int getPendingRecent() { return pendingRecent; }
    }
}
