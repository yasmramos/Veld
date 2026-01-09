package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Servicio para gestionar usuarios.
 */
@Singleton
@Component
public class UserService {
    private final DatabaseService databaseService;

    @Inject
    public UserService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Obtiene el nombre de usuario por ID.
     * @param userId ID del usuario
     * @return Nombre del usuario
     */
    public String getUserName(Long userId) {
        switch (userId.intValue()) {
            case 1: return "Alice";
            case 2: return "Bob";
            case 3: return "Charlie";
            default: return "User-" + userId;
        }
    }

    /**
     * Crea un nuevo usuario.
     * @param userId ID del usuario
     * @param name Nombre del usuario
     * @param email Email del usuario
     */
    public void createUser(Long userId, String name, String email) {
        System.out.println("  [UserService] Creating user: " + name + " (ID: " + userId + ", Email: " + email + ")");
    }

    public String getUserEmail(String userId) {
        return databaseService.getUserEmail(userId);
    }
}
