package io.github.yasmramos.veld.example;

/**
 * DataSource interface demonstrating profile-based configuration.
 * Different implementations are registered based on active profiles.
 */
public interface DataSource {
    
    /**
     * Gets the connection URL.
     * 
     * @return the connection URL
     */
    String getConnectionUrl();
    
    /**
     * Gets the data source name.
     * 
     * @return the data source name
     */
    String getName();
    
    /**
     * Simulates connecting to the database.
     */
    void connect();
    
    /**
     * Simulates disconnecting from the database.
     */
    void disconnect();
}
