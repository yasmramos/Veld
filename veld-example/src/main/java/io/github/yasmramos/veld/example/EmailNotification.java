package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Prototype;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.PreDestroy;

/**
 * Email notification - Prototype scope.
 * Demonstrates @Prototype with constructor injection.
 * 
 * Note: @Prototype implies @Component, so we don't need both.
 */
@Prototype
public class EmailNotification {
    
    private static int notificationCount = 0;
    
    private final LogService logService;
    private final ConfigService configService;
    private final int notificationNumber;
    private String recipient;
    private String subject;
    private String body;
    
    @Inject
    public EmailNotification(LogService logService, ConfigService configService) {
        this.logService = logService;
        this.configService = configService;
        this.notificationNumber = ++notificationCount;
        System.out.println("[EmailNotification] Constructor called - Notification #" + notificationNumber);
    }
    
    @PostConstruct
    public void init() {
        logService.debug("EmailNotification #" + notificationNumber + " ready");
    }
    
    @PreDestroy
    public void cleanup() {
        logService.debug("EmailNotification #" + notificationNumber + " destroyed");
    }
    
    public EmailNotification to(String recipient) {
        this.recipient = recipient;
        return this;
    }
    
    public EmailNotification withSubject(String subject) {
        this.subject = subject;
        return this;
    }
    
    public EmailNotification withBody(String body) {
        this.body = body;
        return this;
    }
    
    public void send() {
        logService.log("=== Sending Email ===");
        logService.log("  From: " + configService.getAppName());
        logService.log("  To: " + recipient);
        logService.log("  Subject: " + subject);
        logService.log("  Body: " + body);
        logService.log("  [Email Sent Successfully]");
    }
    
    public int getNotificationNumber() {
        return notificationNumber;
    }
    
    public static void resetCount() {
        notificationCount = 0;
    }
}
