package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.PreDestroy;
import io.github.yasmramos.veld.annotation.Primary;
import io.github.yasmramos.veld.annotation.Profile;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Production DataSource implementation.
 * Only registered when the "prod" profile is active.
 * 
 * <p>Uses PostgreSQL for production deployments.
 */
@Singleton
@Primary
@Profile("prod")
public class ProdDataSource implements DataSource {
    
    private static final String CONNECTION_URL = "jdbc:postgresql://prod-db:5432/appdb";
    
    @PostConstruct
    public void init() {
        System.out.println("[ProdDataSource] Initializing PostgreSQL connection pool");
    }
    
    @Override
    public String getConnectionUrl() {
        return CONNECTION_URL;
    }
    
    @Override
    public String getName() {
        return "PostgreSQL (Production)";
    }
    
    @Override
    public void connect() {
        System.out.println("[ProdDataSource] Connected to PostgreSQL production database");
    }
    
    @Override
    public void disconnect() {
        System.out.println("[ProdDataSource] Disconnected from PostgreSQL production database");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("[ProdDataSource] Shutting down PostgreSQL connection pool");
    }
}
