package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Value;

/**
 * Servicio de email SMTP.
 */
@Singleton
@Component
public class SmtpEmailService implements EmailService {
    @Value("${app.smtp.host:localhost}")
    private String smtpHost;
    
    @Value("${app.smtp.port:587}")
    private int smtpPort;
    
    @io.github.yasmramos.veld.annotation.PostConstruct
    public void init() {
        System.out.println("  [SmtpEmailService] Conectando a SMTP: " + smtpHost + ":" + smtpPort);
    }
    
    @Override
    public String getProvider() {
        return "SMTP";
    }
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        System.out.println("  [SMTP] Enviando email a: " + to);
    }
}
