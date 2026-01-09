package io.github.yasmramos.veld.example;

/**
 * Interfaz de servicio de email.
 */
public interface EmailService {
    String getProvider();
    void sendEmail(String to, String subject, String body);
}
