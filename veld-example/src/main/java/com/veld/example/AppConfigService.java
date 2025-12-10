package com.veld.example;

import com.veld.annotation.PostConstruct;
import com.veld.annotation.Singleton;
import com.veld.annotation.Value;

/**
 * Example service demonstrating @Value injection for configuration.
 * 
 * <p>Values are resolved from:
 * <ol>
 *   <li>System properties (-Dproperty=value)</li>
 *   <li>Environment variables</li>
 *   <li>application.properties file</li>
 *   <li>Default values specified in the annotation</li>
 * </ol>
 * 
 * @author Veld Framework
 * @since 1.0.0-alpha.5
 */
@Singleton
public class AppConfigService {
    
    /** Application name from configuration */
    @Value("${app.name:Veld Application}")
    String appName;
    
    /** Application version */
    @Value("${app.version:1.0.0}")
    String appVersion;
    
    /** Server port - demonstrates integer conversion */
    @Value("${server.port:8080}")
    int serverPort;
    
    /** Debug mode - demonstrates boolean conversion */
    @Value("${app.debug:false}")
    boolean debugMode;
    
    /** Maximum connections - demonstrates long conversion */
    @Value("${db.max.connections:100}")
    long maxConnections;
    
    /** Timeout in seconds - demonstrates double conversion */
    @Value("${request.timeout:30.0}")
    double requestTimeout;
    
    /** Environment name */
    @Value("${app.environment:development}")
    String environment;
    
    /** API base URL */
    @Value("${api.base.url:http://localhost:8080/api}")
    String apiBaseUrl;
    
    @PostConstruct
    void init() {  // package-private - works via reflection
        System.out.println("[AppConfigService] Configuration loaded:");
        System.out.println("  App Name: " + appName);
        System.out.println("  Version: " + appVersion);
        System.out.println("  Environment: " + environment);
        System.out.println("  Server Port: " + serverPort);
        System.out.println("  Debug Mode: " + debugMode);
        System.out.println("  Max Connections: " + maxConnections);
        System.out.println("  Request Timeout: " + requestTimeout + "s");
        System.out.println("  API URL: " + apiBaseUrl);
    }
    
    // Getters for external access
    
    public String getAppName() {
        return appName;
    }
    
    public String getAppVersion() {
        return appVersion;
    }
    
    public int getServerPort() {
        return serverPort;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public long getMaxConnections() {
        return maxConnections;
    }
    
    public double getRequestTimeout() {
        return requestTimeout;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
    
    /**
     * Returns a summary of all configuration.
     */
    public String getConfigSummary() {
        return String.format(
            "%s v%s [%s] - Port: %d, Debug: %s, Timeout: %.1fs",
            appName, appVersion, environment, serverPort, debugMode, requestTimeout
        );
    }
}
