package io.github.yasmramos.veld.boot.example.repository;

import io.github.yasmramos.veld.boot.example.domain.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for Todo entity.
 * Demonstrates advanced Spring Data features with Veld integration.
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    
    /**
     * Find todos by user
     */
    List<Todo> findByUserId(Long userId);
    
    /**
     * Find completed todos by user
     */
    List<Todo> findByUserIdAndCompletedTrue(Long userId);
    
    /**
     * Find pending todos by user
     */
    List<Todo> findByUserIdAndCompletedFalse(Long userId);
    
    /**
     * Find todos by priority
     */
    List<Todo> findByPriorityOrderByCreatedAtDesc(String priority);
    
    /**
     * Find todos created after a specific date
     */
    List<Todo> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Get todo count by user
     */
    @Query("SELECT COUNT(t) FROM Todo t WHERE t.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * Get completed todo count by user
     */
    @Query("SELECT COUNT(t) FROM Todo t WHERE t.user.id = :userId AND t.completed = true")
    long countCompletedByUserId(@Param("userId") Long userId);
    
    /**
     * Mark all todos as completed for a user
     */
    @Modifying
    @Query("UPDATE Todo t SET t.completed = true, t.completedAt = :completedAt WHERE t.user.id = :userId AND t.completed = false")
    void markAllAsCompleted(@Param("userId") Long userId, @Param("completedAt") LocalDateTime completedAt);
    
    /**
     * Find todos with user information
     */
    @Query("SELECT t FROM Todo t JOIN FETCH t.user WHERE t.completed = :completed ORDER BY t.createdAt DESC")
    List<Todo> findByCompletedWithUser(@Param("completed") boolean completed);
}
