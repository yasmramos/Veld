package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.example.LogService;

/**
 * Configuración de aplicación - componente base sin dependencias.
 * Este servicio proporciona configuración para otros componentes.
 */
@Component("configService")
public class ConfigService {
    
    private String appName = "Veld Framework Example";
    private String environment = "development";
    private int port = 8080;
    private boolean debugMode = false;
    
    @Inject
    private LogService logService;
    
    @PostConstruct
    public void init() {
        System.out.println("    [OK] ConfigService inicializado - Configuración de aplicación lista");
        System.out.println("       App: " + appName + ", Environment: " + environment + ", Port: " + port);
    }
    
    public String getAppName() {
        return appName;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public int getPort() {
        return port;
    }
    
    public boolean isDevelopment() {
        return "development".equals(environment);
    }
    
    public boolean isProduction() {
        return "production".equals(environment);
    }
    
    public boolean isDebugMode() {
        return debugMode || isDevelopment();
    }
    
    public LogService getLogService() {
        return logService;
    }
    
    public String getMessagePrefix() {
        return "[" + appName + "]";
    }
}