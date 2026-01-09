package io.github.yasmramos.veld.boot.example.controller.veld;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.boot.example.domain.Todo;
import io.github.yasmramos.veld.boot.example.domain.User;
import io.github.yasmramos.veld.boot.example.service.spring.TodoBusinessService;
import io.github.yasmramos.veld.boot.example.service.veld.TodoAnalysisService;
import io.github.yasmramos.veld.boot.example.service.veld.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Veld REST controller demonstrating Spring Boot + Veld integration.
 * 
 * This controller:
 * - Uses Veld annotations for routing and injection
 * - Injects both Spring-managed services and Veld components
 * - Demonstrates bidirectional bean bridging
 * - Provides REST endpoints for todo management
 */
@Component("todoController")
@RequestMapping("/api/v2/todos")
public class TodoController {
    
    private static final Logger logger = LoggerFactory.getLogger(TodoController.class);
    
    // Spring-managed service injected into Veld controller
    @Inject
    private TodoBusinessService springTodoService;
    
    // Veld component
    @Inject
    private TodoAnalysisService veldAnalysisService;
    
    // Veld user management service
    @Inject
    private UserManagementService userService;
    
    /**
     * Get all todos for a specific user
     */
    @GetMapping
    public List<Todo> getUserTodos(@RequestParam Long userId) {
        logger.info("Getting todos for user: {}", userId);
        return springTodoService.getUserTodos(userId);
    }
    
    /**
     * Create a new todo
     */
    @PostMapping
    public Todo createTodo(@RequestParam Long userId,
                          @RequestParam String title,
                          @RequestParam(required = false) String description,
                          @RequestParam(defaultValue = "MEDIUM") String priority) {
        logger.info("Creating todo for user: {} - {}", userId, title);
        return springTodoService.createTodo(userId, title, description, priority);
    }
    
    /**
     * Complete a todo
     */
    @PutMapping("/{todoId}/complete")
    public Todo completeTodo(@PathVariable Long todoId) {
        logger.info("Completing todo: {}", todoId);
        return springTodoService.completeTodo(todoId);
    }
    
    /**
     * Delete a todo
     */
    @DeleteMapping("/{todoId}")
    public Map<String, String> deleteTodo(@PathVariable Long todoId) {
        logger.info("Deleting todo: {}", todoId);
        springTodoService.deleteTodo(todoId);
        return Map.of("message", "Todo deleted successfully", "id", todoId.toString());
    }
    
    /**
     * Get todo statistics using Veld analysis service
     */
    @GetMapping("/statistics/{userId}")
    public TodoAnalysisService.CompletionAnalysis getStatistics(@PathVariable Long userId) {
        logger.info("Getting statistics for user: {}", userId);
        return veldAnalysisService.analyzeUserCompletion(userId);
    }
    
    /**
     * Get productivity insights using Veld service
     */
    @GetMapping("/productivity/{userId}")
    public TodoAnalysisService.ProductivityInsights getProductivity(@PathVariable Long userId) {
        logger.info("Getting productivity insights for user: {}", userId);
        return veldAnalysisService.getProductivityInsights(userId);
    }
    
    /**
     * Get recent activity summary
     */
    @GetMapping("/activity")
    public TodoAnalysisService.ActivitySummary getRecentActivity() {
        logger.info("Getting recent activity summary");
        return veldAnalysisService.getRecentActivity();
    }
    
    /**
     * Demo endpoint showing Veld controller accessing Spring services
     */
    @GetMapping("/demo/spring-integration")
    public Map<String, Object> demoSpringIntegration(@RequestParam Long userId) {
        logger.info("Demo Spring integration for user: {}", userId);
        
        // Get data from Spring service
        List<Todo> todos = springTodoService.getUserTodos(userId);
        TodoBusinessService.TodoStatistics springStats = springTodoService.getTodoStatistics(userId);
        
        // Get data from Veld service
        TodoAnalysisService.CompletionAnalysis veldStats = veldAnalysisService.analyzeUserCompletion(userId);
        
        return Map.of(
            "framework", "Veld Controller",
            "springService", "TodoBusinessService",
            "veldService", "TodoAnalysisService",
            "userId", userId,
            "totalTodos", springStats.total(),
            "springCompleted", springStats.completed(),
            "veldAnalysisCompleted", veldStats.completedTodos(),
            "todos", todos
        );
    }
}
