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

    public String getUserEmail(String userId) {
        return databaseService.getUserEmail(userId);
    }
}
