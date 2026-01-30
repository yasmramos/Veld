package io.github.yasmramos.veld.example.dependsOn;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Servicio de base de datos de ejemplo para demostrar @DependsOn.
 * Este bean está disponible sin restricciones de perfil.
 */
@Singleton
@Component("databaseService")
public class ExampleDatabaseService {
    
    private boolean connected = false;
    
    @PostConstruct
    public void init() {
        System.out.println("    [ExampleDatabaseService] Conectando a base de datos de ejemplo");
        connected = true;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void executeQuery(String sql) {
        System.out.println("       [ExampleDatabaseService] Ejecutando: " + sql);
    }
    
    public void saveData(String data) {
        System.out.println("       [ExampleDatabaseService] Guardando datos: " + data);
    }
}
