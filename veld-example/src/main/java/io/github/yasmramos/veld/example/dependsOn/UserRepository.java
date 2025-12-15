package io.github.yasmramos.veld.example.dependsOn;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.DependsOn;

/**
 * Repositorio de usuarios - depende explícitamente de DatabaseService.
 * El orden de inicialización debe asegurar que DatabaseService se inicialice antes.
 */
@Component("userRepository")
@DependsOn("databaseService")
public class UserRepository {
    
    private DatabaseService databaseService;
    
    @PostConstruct
    public void init() {
        System.out.println("    ✅ UserRepository inicializado - Repositorio de usuarios listo");
        if (databaseService != null && databaseService.isConnected()) {
            System.out.println("       DatabaseService está disponible y conectado");
            // Inicializar tabla de usuarios
            databaseService.executeQuery("CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(100))");
        } else {
            System.out.println("       ❌ DatabaseService no está disponible");
        }
    }
    
    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
    
    public void saveUser(Long id, String name) {
        if (databaseService != null && databaseService.isConnected()) {
            databaseService.saveData("User: " + id + " - " + name);
            System.out.println("       Usuario guardado: " + name + " (ID: " + id + ")");
        } else {
            System.out.println("       ❌ No se puede guardar usuario - DatabaseService no disponible");
        }
    }
    
    public void findUser(Long id) {
        if (databaseService != null && databaseService.isConnected()) {
            System.out.println("       Buscando usuario con ID: " + id);
            // Simular búsqueda
            System.out.println("       Usuario encontrado: John Doe (ID: " + id + ")");
        } else {
            System.out.println("       ❌ No se puede buscar usuario - DatabaseService no disponible");
        }
    }
}