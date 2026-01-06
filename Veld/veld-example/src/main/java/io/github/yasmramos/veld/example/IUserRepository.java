package io.github.yasmramos.veld.example;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for user repository operations.
 * Demonstrates interface-based injection in Veld.
 * 
 * When injecting this interface, Veld will automatically resolve
 * to the concrete implementation (UserRepositoryImpl).
 */
public interface IUserRepository {
    
    /**
     * Finds a user by their ID.
     * 
     * @param id the user ID
     * @return Optional containing the user name if found
     */
    Optional<String> findById(Long id);
    
    /**
     * Saves a user with the given ID and name.
     * 
     * @param id the user ID
     * @param name the user name
     */
    void save(Long id, String name);
    
    /**
     * Returns all users.
     * 
     * @return map of user ID to user name
     */
    Map<Long, String> findAll();
}
