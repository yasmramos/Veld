package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Simple message service - Singleton scope.
 * Demonstrates basic dependency injection with Veld.
 */
@Singleton
@Component
public class MessageService {
    private final ConfigService config;
    
    @Inject
    public MessageService(ConfigService config) {
        this.config = config;
    }
    
    public void sendMessage(String message) {
        String prefix = config.getMessagePrefix();
        System.out.println(prefix + ": " + message);
    }
}
