package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Profile;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Test DataSource implementation.
 * Only registered when the "test" profile is active.
 * 
 * <p>Uses an in-memory database that resets between tests.
 */
@Singleton
@Profile("test")
public class TestDataSource implements DataSource {
    
    private static final String CONNECTION_URL = "jdbc:h2:mem:testdb;MODE=PostgreSQL";
    
    @PostConstruct
    public void init() {
        System.out.println("[TestDataSource] Initializing H2 test database with PostgreSQL mode");
    }
    
    @Override
    public String getConnectionUrl() {
        return CONNECTION_URL;
    }
    
    @Override
    public String getName() {
        return "H2 Test (PostgreSQL Mode)";
    }
    
    @Override
    public void connect() {
        System.out.println("[TestDataSource] Connected to test database");
    }
    
    @Override
    public void disconnect() {
        System.out.println("[TestDataSource] Disconnected from test database");
    }
}
