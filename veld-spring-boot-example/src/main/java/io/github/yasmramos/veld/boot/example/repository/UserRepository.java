package io.github.yasmramos.veld.boot.example.repository;

import io.github.yasmramos.veld.boot.example.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 * Demonstrates Spring Data integration with Veld application.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find all active users
     */
    List<User> findByActiveTrue();
    
    /**
     * Find users with todos
     */
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.todos WHERE u.active = true")
    List<User> findUsersWithTodos();
    
    /**
     * Find user by username (case insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username)")
    Optional<User> findByUsernameIgnoreCase(@Param("username") String username);
}
