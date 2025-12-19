package io.github.yasmramos.veld.example.dependsOn;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.DependsOn;
import io.github.yasmramos.veld.example.ConfigService;

/**
 * Servicio de email - depende explícitamente de ConfigService.
 * Envía notificaciones por email usando la configuración.
 */
@Component("emailService")
@DependsOn("configService")
public class EmailService {
    
    private ConfigService configService;
    private String smtpHost;
    private int smtpPort;
    
    @PostConstruct
    public void init() {
        System.out.println("    ✅ EmailService inicializado - Servicio de email configurado");
        if (configService != null) {
            smtpHost = "smtp." + configService.getEnvironment() + ".example.com";
            smtpPort = configService.getPort() == 8080 ? 587 : 465;
            System.out.println("       SMTP Host: " + smtpHost);
            System.out.println("       SMTP Port: " + smtpPort);
            System.out.println("       Entorno: " + configService.getEnvironment());
        } else {
            System.out.println("       ❌ ConfigService no disponible");
        }
    }
    
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
    
    public void sendEmail(String to, String subject, String body) {
        if (configService != null) {
            System.out.println("       Enviando email a: " + to);
            System.out.println("       Asunto: " + subject);
            System.out.println("       Configuración: " + configService.getAppName());
            System.out.println("       ✅ Email enviado exitosamente");
        } else {
            System.out.println("       ❌ No se puede enviar email - ConfigService no disponible");
        }
    }
    
    public void sendWelcomeEmail(String userEmail, String userName) {
        if (configService != null) {
            String subject = "Bienvenido a " + configService.getAppName();
            String body = "Hola " + userName + ",\n\n¡Bienvenido a nuestro framework!\n\nSaludos,\nEl equipo de " + configService.getAppName();
            sendEmail(userEmail, subject, body);
        } else {
            System.out.println("       ❌ No se puede enviar email de bienvenida - ConfigService no disponible");
        }
    }
}
