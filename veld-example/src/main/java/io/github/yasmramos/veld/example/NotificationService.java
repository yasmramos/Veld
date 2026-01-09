package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Servicio para enviar notificaciones.
 */
@Singleton
@Component
public class NotificationService {
    private final EmailService emailService;
    private final UserService userService;
    
    @Inject
    public NotificationService(EmailService emailService, UserService userService) {
        this.emailService = emailService;
        this.userService = userService;
    }
    
    public void sendOrderConfirmation(String orderId) {
        System.out.println("  [NotificationService] Enviando confirmación para orden: " + orderId);
        // Lógica de notificación...
    }
}
