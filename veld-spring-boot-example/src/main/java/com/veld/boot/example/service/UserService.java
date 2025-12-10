package com.veld.boot.example.service;

import com.veld.annotation.Component;
import com.veld.annotation.Inject;
import com.veld.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example Veld component demonstrating dependency injection.
 * This component shows:
 * - Veld @Component annotation
 * - Field injection using @Inject
 * - Lifecycle management with @PostConstruct
 */
@Component("userService")
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final MessageService messageService;

    @Inject
    public UserService(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostConstruct
    public void init() {
        logger.info("UserService initialized with message service: {}", 
                   messageService.getMessage());
    }

    public String getUserWelcomeMessage(String username) {
        return messageService.formatMessage("Welcome " + username + "!");
    }

    public String getServiceInfo() {
        return "UserService running with Veld DI framework";
    }
}