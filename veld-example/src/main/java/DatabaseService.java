package io.github.yasmramos.veld.example.dependsOn;

/**
 * Interfaz de servicio de base de datos.
 */
public interface DatabaseService {
    String getConnectionInfo();
    void connect();
    void disconnect();
}