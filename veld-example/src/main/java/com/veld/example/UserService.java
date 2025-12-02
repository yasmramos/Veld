package com.veld.example;

import com.veld.annotation.Component;
import com.veld.annotation.Singleton;
import com.veld.annotation.Inject;
import com.veld.annotation.PostConstruct;
import com.veld.annotation.PreDestroy;
import java.util.Optional;

/**
 * User service - Singleton scope.
 * Demonstrates method injection with @Inject on setter.
 */
@Component
@Singleton
public class UserService {
    
    private LogService logService;
    private ConfigService configService;
    private UserRepository userRepository;
    
    public UserService() {
        System.out.println("[UserService] Constructor called");
    }
    
    @Inject
    public void setLogService(LogService logService) {
        this.logService = logService;
        System.out.println("[UserService] LogService injected via method");
    }
    
    @Inject
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
        System.out.println("[UserService] ConfigService injected via method");
    }
    
    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("[UserService] UserRepository injected via method");
    }
    
    @PostConstruct
    public void init() {
        logService.log("UserService initialized for app: " + configService.getAppName());
    }
    
    @PreDestroy
    public void shutdown() {
        logService.log("UserService shutting down...");
    }
    
    public String getUserName(Long id) {
        Optional<String> user = userRepository.findById(id);
        if (user.isPresent()) {
            if (configService.isDebugMode()) {
                logService.debug("Found user: " + user.get());
            }
            return user.get();
        }
        logService.log("User not found: " + id);
        return null;
    }
    
    public void createUser(Long id, String name) {
        logService.log("Creating user: " + name);
        userRepository.save(id, name);
    }
    
    public void listAllUsers() {
        logService.log("=== All Users ===");
        userRepository.findAll().forEach((id, name) -> 
            logService.log("  ID: " + id + " -> " + name)
        );
    }
}
