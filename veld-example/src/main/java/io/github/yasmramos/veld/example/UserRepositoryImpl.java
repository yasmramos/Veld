package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * User repository implementation - Singleton scope.
 * Demonstrates interface-based injection.
 * 
 * This class implements IUserRepository, so when you inject IUserRepository,
 * Veld will automatically resolve to this implementation.
 * 
 * Note: @Singleton("name") includes component naming - no need for @Component.
 */
@Singleton("userRepository")
public class UserRepositoryImpl implements IUserRepository {
    
    private final LogService logService;
    private final Map<Long, String> users = new HashMap<>();
    
    @Inject
    public UserRepositoryImpl(LogService logService) {
        this.logService = logService;
        System.out.println("[UserRepositoryImpl] Constructor called with LogService");
    }
    
    @PostConstruct
    public void init() {
        // Pre-populate with some users
        users.put(1L, "Alice");
        users.put(2L, "Bob");
        users.put(3L, "Charlie");
        logService.log("UserRepositoryImpl initialized with " + users.size() + " users");
    }
    
    @Override
    public Optional<String> findById(Long id) {
        logService.debug("Finding user by id: " + id);
        return Optional.ofNullable(users.get(id));
    }
    
    @Override
    public void save(Long id, String name) {
        users.put(id, name);
        logService.log("Saved user: " + name + " with id: " + id);
    }
    
    @Override
    public Map<Long, String> findAll() {
        return new HashMap<>(users);
    }
}
