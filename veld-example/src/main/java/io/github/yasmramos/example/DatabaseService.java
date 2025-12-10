package io.github.yasmramos.example;

/**
 * Interface for database services.
 * Used to demonstrate @ConditionalOnMissingBean.
 */
public interface DatabaseService {
    
    String getConnectionInfo();
    
    void connect();
    
    void disconnect();
}
