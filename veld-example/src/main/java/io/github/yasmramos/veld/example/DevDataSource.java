package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.PreDestroy;
import io.github.yasmramos.veld.annotation.Profile;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Development DataSource implementation.
 * Only registered when the "dev" profile is active.
 * 
 * <p>Uses an in-memory H2 database for fast development cycles.
 */
@Singleton
@Profile("dev")
public class DevDataSource implements DataSource {
    
    private static final String CONNECTION_URL = "jdbc:h2:mem:devdb;DB_CLOSE_DELAY=-1";
    
    @PostConstruct
    public void init() {
        System.out.println("[DevDataSource] Initializing H2 in-memory database for development");
    }
    
    @Override
    public String getConnectionUrl() {
        return CONNECTION_URL;
    }
    
    @Override
    public String getName() {
        return "H2 In-Memory (Development)";
    }
    
    @Override
    public void connect() {
        System.out.println("[DevDataSource] Connected to H2 in-memory database");
    }
    
    @Override
    public void disconnect() {
        System.out.println("[DevDataSource] Disconnected from H2 in-memory database");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("[DevDataSource] Shutting down H2 database");
    }
}
