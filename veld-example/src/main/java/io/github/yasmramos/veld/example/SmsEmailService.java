package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Primary;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Value;

/**
 * Servicio de SMS.
 */
@Singleton
@Primary
@Component
public class SmsEmailService implements EmailService {
    @Value("${app.sms.provider:twilio}")
    private String smsProvider;
    
    @io.github.yasmramos.veld.annotation.PostConstruct
    public void init() {
        System.out.println("  [SmsEmailService] Inicializando SMS provider: " + smsProvider);
    }
    
    @Override
    public String getProvider() {
        return "SMS";
    }
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        System.out.println("  [SMS] Enviando SMS a: " + to);
    }
}
