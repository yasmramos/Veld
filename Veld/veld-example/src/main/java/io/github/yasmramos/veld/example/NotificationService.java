package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;

/**
 * Example service using Veld annotations.
 * Demonstrates Veld's native annotation support for dependency injection.
 * Uses:
 * - @Component("name") from Veld (includes component naming)
 * - @Inject for constructor and method injection
 * 
 * This demonstrates that Veld handles dependency injection natively.
 * Note: @Component implies @Singleton functionality.
 */
@Component("notificationService")
public class NotificationService {
    
    private LogService logService;
    private ConfigService configService;
    private EmailNotification emailNotification;
    
    /**
     * Constructor using Veld @Inject
     */
    @Inject
    public NotificationService(LogService logService) {
        this.logService = logService;
        System.out.println("[NotificationService] Created with Veld @Inject constructor");
    }
    
    /**
     * Method injection using Veld @Inject
     */
    @Inject
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
        System.out.println("[NotificationService] ConfigService injected via Veld @Inject");
    }
    
    /**
     * Field injection using Veld's native @Inject
     * Note: Field must be non-private for Veld (no reflection used)
     */
    @Inject
    EmailNotification emailNotificationField;
    
    @PostConstruct
    public void init() {
        this.emailNotification = emailNotificationField;
        logService.log("NotificationService initialized with Veld annotations");
    }
    
    public void sendNotification(String recipient, String message) {
        // Using email as default channel
        String channel = "email";
        
        logService.log("Sending notification via " + channel + " to: " + recipient);
        
        switch (channel) {
            case "email":
                // Using EmailNotification's fluent API
                emailNotification
                    .to(recipient)
                    .withSubject("Notification")
                    .withBody(message)
                    .send();
                break;
            case "log":
                logService.log("NOTIFICATION to " + recipient + ": " + message);
                break;
            default:
                System.out.println("[NotificationService] Unknown channel: " + channel);
        }
    }
    
    public void sendWelcomeNotification(String userName) {
        // Using existing ConfigService API - get app name for personalized message
        String appName = configService.getAppName();
        String welcomeMessage = "Welcome to " + appName + "!";
        sendNotification(userName, welcomeMessage);
    }
}
