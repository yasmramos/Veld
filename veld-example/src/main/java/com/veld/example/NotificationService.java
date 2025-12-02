package com.veld.example;

import com.veld.annotation.Component;
import com.veld.annotation.PostConstruct;
import com.veld.annotation.Singleton;

/**
 * Example service demonstrating mixed annotations from different specifications.
 * Uses:
 * - @Component from Veld (required for component discovery)
 * - @Singleton from Veld
 * - @javax.inject.Inject for some injections
 * - @jakarta.inject.Inject for other injections
 * - @javax.inject.Named for qualifier
 * 
 * This demonstrates that Veld can handle mixed annotation usage in a single class.
 */
@Component("notificationService")
@Singleton
public class NotificationService {
    
    private LogService logService;
    private ConfigService configService;
    private EmailNotification emailNotification;
    
    /**
     * Constructor using javax.inject.Inject
     */
    @javax.inject.Inject
    public NotificationService(LogService logService) {
        this.logService = logService;
        System.out.println("[NotificationService] Created with javax.inject.Inject constructor");
    }
    
    /**
     * Method injection using jakarta.inject.Inject
     */
    @jakarta.inject.Inject
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
        System.out.println("[NotificationService] ConfigService injected via jakarta.inject.Inject");
    }
    
    /**
     * Field injection using Veld's native @Inject
     * Note: Field must be non-private for Veld (no reflection used)
     */
    @com.veld.annotation.Inject
    EmailNotification emailNotificationField;
    
    @PostConstruct
    public void init() {
        this.emailNotification = emailNotificationField;
        logService.log("NotificationService initialized with mixed annotation support");
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
