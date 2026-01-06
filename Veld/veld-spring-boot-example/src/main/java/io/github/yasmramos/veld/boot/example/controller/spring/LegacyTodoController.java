package io.github.yasmramos.veld.boot.example.controller.spring;

import io.github.yasmramos.veld.boot.example.domain.Todo;
import io.github.yasmramos.veld.boot.example.service.spring.TodoBusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Spring MVC controller demonstrating legacy Spring Boot implementation.
 * 
 * This controller represents the "before" state in a migration scenario.
 * In a real migration, this would be gradually replaced by Veld controllers.
 * 
 * Demonstrates the "Strangler Fig" pattern for incremental migration.
 */
@RestController
@RequestMapping("/api/v1/todos")
public class LegacyTodoController {
    
    private static final Logger logger = LoggerFactory.getLogger(LegacyTodoController.class);
    
    private final TodoBusinessService todoBusinessService;
    
    @Autowired
    public LegacyTodoController(TodoBusinessService todoBusinessService) {
        this.todoBusinessService = todoBusinessService;
        logger.info("LegacyTodoController initialized - Spring MVC only");
    }
    
    /**
     * Get todos using legacy Spring MVC approach
     */
    @GetMapping
    public List<Todo> getUserTodosLegacy(@RequestParam Long userId) {
        logger.info("Legacy Spring MVC: Getting todos for user: {}", userId);
        return todoBusinessService.getUserTodos(userId);
    }
    
    /**
     * Create todo using legacy approach
     */
    @PostMapping
    public Todo createTodoLegacy(@RequestParam Long userId,
                                @RequestParam String title,
                                @RequestParam(required = false) String description) {
        logger.info("Legacy Spring MVC: Creating todo for user: {} - {}", userId, title);
        return todoBusinessService.createTodo(userId, title, description, "MEDIUM");
    }
    
    /**
     * Complete todo using legacy approach
     */
    @PutMapping("/{todoId}/complete")
    public Todo completeTodoLegacy(@PathVariable Long todoId) {
        logger.info("Legacy Spring MVC: Completing todo: {}", todoId);
        return todoBusinessService.completeTodo(todoId);
    }
    
    /**
     * Legacy statistics endpoint
     */
    @GetMapping("/stats/{userId}")
    public TodoBusinessService.TodoStatistics getStatsLegacy(@PathVariable Long userId) {
        logger.info("Legacy Spring MVC: Getting statistics for user: {}", userId);
        return todoBusinessService.getTodoStatistics(userId);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Map<String, String> healthLegacy() {
        return Map.of(
            "framework", "Spring MVC (Legacy)",
            "status", "Running",
            "approach", "Traditional Spring Boot only"
        );
    }
}
