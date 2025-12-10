package com.veld.boot.example.service;

import com.veld.annotation.Component;
import com.veld.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Another Veld component demonstrating the framework's capabilities.
 * This service shows:
 * - Singleton scope management
 * - Lifecycle callbacks
 * - No dependencies (simpler service)
 */
@Component("messageService")
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private String message = "Hello from Veld!";

    @PostConstruct
    public void init() {
        logger.info("MessageService initialized - Veld framework working!");
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String formatMessage(String content) {
        return "[Veld Service] " + content + " (Generated at " + 
               java.time.LocalTime.now() + ")";
    }

    public String getFrameworkInfo() {
        return "MessageService running on Veld Framework - Zero Reflection DI";
    }
}