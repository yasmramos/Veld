package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.PreDestroy;
import java.util.Optional;

/**
 * User service - Singleton scope.
 * Demonstrates method injection with @Inject on setter.
 * 
 * NOTE: This class injects IUserRepository (interface), NOT the concrete class.
 * Veld will automatically resolve IUserRepository -> UserRepositoryImpl.
 * 
 * Note: @Singleton implies @Component, so we don't need both.
 */
@Singleton
public class UserService {
    
    private LogService logService;
    private ConfigService configService;
    private IUserRepository userRepository; // <-- Interface, not concrete class!
    
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
    public void setUserRepository(IUserRepository userRepository) {  // <-- Interface injection!
        this.userRepository = userRepository;
        System.out.println("[UserService] IUserRepository injected via method -> " + userRepository.getClass().getSimpleName());
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
