package io.github.yasmramos.veld.boot.example.controller;

import io.github.yasmramos.veld.boot.example.service.MessageService;
import io.github.yasmramos.veld.boot.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller demonstrating integration between Spring Boot and Veld framework.
 * 
 * This controller:
 * - Uses Spring's @Autowired for Spring beans
 * - Accesses Veld components directly
 * - Provides REST endpoints for testing
 */
@RestController
@RequestMapping("/api/veld")
public class VeldIntegrationController {

    // Spring-managed bean (for comparison)
    @Autowired
    private MessageService veldMessageService;

    // Veld component
    private final UserService userService;

    // Spring-managed service using Veld bean
    public VeldIntegrationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/message")
    public String getMessage() {
        return veldMessageService.getMessage();
    }

    @GetMapping("/welcome")
    public String getWelcome(@RequestParam(defaultValue = "Developer") String name) {
        return userService.getUserWelcomeMessage(name);
    }

    @GetMapping("/status")
    public VeldStatus getStatus() {
        return new VeldStatus(
            "Veld Spring Boot Integration",
            userService.getServiceInfo(),
            veldMessageService.getFrameworkInfo(),
            "Running"
        );
    }

    @GetMapping("/health")
    public String getHealthCheck() {
        return "Veld framework is running successfully! Check /actuator/health/veld for detailed status.";
    }

    // Simple DTO for status response
    public static class VeldStatus {
        private final String framework;
        private final String userService;
        private final String messageService;
        private final String status;

        public VeldStatus(String framework, String userService, String messageService, String status) {
            this.framework = framework;
            this.userService = userService;
            this.messageService = messageService;
            this.status = status;
        }

        public String getFramework() { return framework; }
        public String getUserService() { return userService; }
        public String getMessageService() { return messageService; }
        public String getStatus() { return status; }
    }
}