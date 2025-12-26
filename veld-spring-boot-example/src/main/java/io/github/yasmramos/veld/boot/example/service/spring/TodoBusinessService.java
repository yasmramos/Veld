package io.github.yasmramos.veld.boot.example.service.spring;

import io.github.yasmramos.veld.boot.example.domain.Todo;
import io.github.yasmramos.veld.boot.example.domain.User;
import io.github.yasmramos.veld.boot.example.repository.TodoRepository;
import io.github.yasmramos.veld.boot.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring-managed service for Todo business logic.
 * Demonstrates Spring @Service with @Transactional.
 * This service will be bridged to Veld for cross-framework injection.
 */
@Service
@Transactional
public class TodoBusinessService {
    
    private static final Logger logger = LoggerFactory.getLogger(TodoBusinessService.class);
    
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public TodoBusinessService(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
        logger.info("TodoBusinessService initialized - Spring-managed service");
    }
    
    /**
     * Create a new todo for a user
     */
    public Todo createTodo(Long userId, String title, String description, String priority) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Todo todo = new Todo(title, description, priority);
        todo.setUser(user);
        
        Todo saved = todoRepository.save(todo);
        logger.info("Created todo {} for user {}", saved.getId(), user.getUsername());
        
        return saved;
    }
    
    /**
     * Get all todos for a user
     */
    @Transactional(readOnly = true)
    public List<Todo> getUserTodos(Long userId) {
        return todoRepository.findByUserId(userId);
    }
    
    /**
     * Complete a todo
     */
    public Todo completeTodo(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found: " + todoId));
        
        todo.setCompleted(true);
        Todo saved = todoRepository.save(todo);
        
        logger.info("Completed todo {} - {}", todoId, todo.getTitle());
        return saved;
    }
    
    /**
     * Get todo statistics for a user
     */
    @Transactional(readOnly = true)
    public TodoStatistics getTodoStatistics(Long userId) {
        long total = todoRepository.countByUserId(userId);
        long completed = todoRepository.countCompletedByUserId(userId);
        long pending = total - completed;
        
        return new TodoStatistics(total, completed, pending);
    }
    
    /**
     * Delete a todo
     */
    public void deleteTodo(Long todoId) {
        todoRepository.deleteById(todoId);
        logger.info("Deleted todo {}", todoId);
    }
    
    /**
     * Get recent todos across all users
     */
    @Transactional(readOnly = true)
    public List<Todo> getRecentTodos() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return todoRepository.findByCreatedAtAfter(weekAgo);
    }
    
    /**
     * Statistics DTO (Java 17 record)
     */
    public record TodoStatistics(long total, long completed, long pending) {
        public double getCompletionRate() {
            return total > 0 ? (double) completed / total : 0.0;
        }
    }
}
