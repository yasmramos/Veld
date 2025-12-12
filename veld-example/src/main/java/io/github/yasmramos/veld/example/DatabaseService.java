package io.github.yasmramos.veld.example;

/**
 * Interface for database services.
 * Used to demonstrate @ConditionalOnMissingBean.
 */
public interface DatabaseService {
    
    String getConnectionInfo();
    
    void connect();
    
    void disconnect();
}
