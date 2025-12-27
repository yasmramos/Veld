package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.ConditionalOnMissingBean;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.example.dependsOn.DatabaseService;

/**
 * Default database service implementation.
 * Only registered if no other DatabaseService is available.
 * 
 * This demonstrates @ConditionalOnMissingBean - this service will be
 * used as a fallback when no custom DatabaseService is provided.
 */
@Singleton
@ConditionalOnMissingBean(DatabaseService.class)
public class DefaultDatabaseService implements DatabaseService {
    
    @PostConstruct
    public void init() {
        System.out.println("[DefaultDatabaseService] Initializing in-memory database (fallback)");
    }
    
    @Override
    public String getConnectionInfo() {
        return "In-Memory H2 Database (default)";
    }
    
    @Override
    public void connect() {
        System.out.println("[DefaultDatabaseService] Connected to in-memory database");
    }
    
    @Override
    public void disconnect() {
        System.out.println("[DefaultDatabaseService] Disconnected from in-memory database");
    }
    
    @Override
    public boolean isConnected() {
        return true; // Simulate connected state
    }
    
    @Override
    public void saveData(String data) {
        System.out.println("[DefaultDatabaseService] Saving data: " + data);
    }
    
    @Override
    public void executeQuery(String query) {
        System.out.println("[DefaultDatabaseService] Executing query: " + query);
    }
}
