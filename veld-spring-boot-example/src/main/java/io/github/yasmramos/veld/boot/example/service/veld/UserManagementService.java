package io.github.yasmramos.veld.boot.example.service.veld;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.boot.example.domain.User;
import io.github.yasmramos.veld.boot.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Veld component for user management demonstrating Veld -> Spring repository bridging.
 * 
 * This service:
 * - Is a Veld component (@Component, @Singleton)
 * - Injects Spring Data JPA repository
 * - Provides user management functionality
 * - Demonstrates Veld -> Spring integration
 */
@Component("userManagementService")
@Singleton
public class UserManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);
    
    // Spring repository injected into Veld component
    @Inject
    private UserRepository springUserRepository;
    
    @PostConstruct
    public void init() {
        logger.info("UserManagementService initialized with Spring UserRepository: {}", 
                   springUserRepository.getClass().getSimpleName());
    }
    
    /**
     * Create a new user
     */
    public User createUser(String username, String email, String firstName, String lastName) {
        // Check if user already exists
        if (springUserRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("User already exists: " + username);
        }
        
        if (springUserRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists: " + email);
        }
        
        User user = new User(username, email, firstName, lastName);
        User saved = springUserRepository.save(user);
        
        logger.info("Created user: {} ({})", saved.getUsername(), saved.getEmail());
        return saved;
    }
    
    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long id) {
        return springUserRepository.findById(id);
    }
    
    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return springUserRepository.findByUsername(username);
    }
    
    /**
     * Get user by email
     */
    public Optional<User> getUserByEmail(String email) {
        return springUserRepository.findByEmail(email);
    }
    
    /**
     * Get all active users
     */
    public List<User> getActiveUsers() {
        return springUserRepository.findByActiveTrue();
    }
    
    /**
     * Get users with todos
     */
    public List<User> getUsersWithTodos() {
        return springUserRepository.findUsersWithTodos();
    }
    
    /**
     * Update user information
     */
    public User updateUser(Long userId, String email, String firstName, String lastName) {
        User user = springUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Check if email is taken by another user
        if (!email.equals(user.getEmail()) && 
            springUserRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists: " + email);
        }
        
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        
        User saved = springUserRepository.save(user);
        logger.info("Updated user: {}", saved.getUsername());
        return saved;
    }
    
    /**
     * Deactivate user
     */
    public User deactivateUser(Long userId) {
        User user = springUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setActive(false);
        User saved = springUserRepository.save(user);
        
        logger.info("Deactivated user: {}", saved.getUsername());
        return saved;
    }
    
    /**
     * Get user statistics
     */
    public UserStatistics getUserStatistics() {
        List<User> allUsers = springUserRepository.findAll();
        List<User> activeUsers = springUserRepository.findByActiveTrue();
        
        return new UserStatistics(
            allUsers.size(),
            activeUsers.size(),
            allUsers.size() - activeUsers.size()
        );
    }
    
    /**
     * Search users by name or email
     */
    public List<User> searchUsers(String query) {
        // Simple search implementation
        List<User> allUsers = springUserRepository.findAll();
        String lowerQuery = query.toLowerCase();
        
        return allUsers.stream()
                .filter(user -> 
                    user.getUsername().toLowerCase().contains(lowerQuery) ||
                    user.getEmail().toLowerCase().contains(lowerQuery) ||
                    user.getFullName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }
    
    /**
     * Statistics DTO
     */
    public static class UserStatistics {
        private final int totalUsers;
        private final int activeUsers;
        private final int inactiveUsers;
        
        public UserStatistics(int totalUsers, int activeUsers, int inactiveUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.inactiveUsers = inactiveUsers;
        }
        
        // Getters
        public int getTotalUsers() { return totalUsers; }
        public int getActiveUsers() { return activeUsers; }
        public int getInactiveUsers() { return inactiveUsers; }
    }
}
