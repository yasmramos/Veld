package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Profile;

/**
 * Servicio para enviar notificaciones.
 * Only available in production profile since it depends on production services.
 */
@Profile("production")
@Singleton
@Component
public class NotificationService {
    private final SmtpEmailService emailService;
    private final UserService userService;

    @Inject
    public NotificationService(SmtpEmailService emailService, UserService userService) {
        this.emailService = emailService;
        this.userService = userService;
    }

    public void sendOrderConfirmation(String orderId) {
        System.out.println("  [NotificationService] Enviando confirmación para orden: " + orderId);
        // Lógica de notificación...
    }
}
