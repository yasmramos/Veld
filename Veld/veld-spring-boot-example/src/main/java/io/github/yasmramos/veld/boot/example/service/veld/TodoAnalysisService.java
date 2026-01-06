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
            stats.total(),
            stats.completed(),
            stats.pending(),
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
            stats.pending()
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
    
    // DTOs for analysis results (Java 17 records)
    public record CompletionAnalysis(long totalTodos, long completedTodos, long pendingTodos, double completionRate) {}
    
    public record ProductivityInsights(double completionRate, String productivityLevel, long pendingCount) {}
    
    public record ActivitySummary(int totalRecent, int completedRecent, int pendingRecent) {}
}
