package com.veld.example;

import com.veld.annotation.Component;
import com.veld.annotation.Singleton;
import com.veld.annotation.Inject;
import com.veld.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * User repository - Singleton scope.
 * Demonstrates constructor injection.
 */
@Component
@Singleton
public class UserRepository {
    
    private final LogService logService;
    private final Map<Long, String> users = new HashMap<>();
    
    @Inject
    public UserRepository(LogService logService) {
        this.logService = logService;
        System.out.println("[UserRepository] Constructor called with LogService");
    }
    
    @PostConstruct
    public void init() {
        // Pre-populate with some users
        users.put(1L, "Alice");
        users.put(2L, "Bob");
        users.put(3L, "Charlie");
        logService.log("UserRepository initialized with " + users.size() + " users");
    }
    
    public Optional<String> findById(Long id) {
        logService.debug("Finding user by id: " + id);
        return Optional.ofNullable(users.get(id));
    }
    
    public void save(Long id, String name) {
        users.put(id, name);
        logService.log("Saved user: " + name + " with id: " + id);
    }
    
    public Map<Long, String> findAll() {
        return new HashMap<>(users);
    }
}
