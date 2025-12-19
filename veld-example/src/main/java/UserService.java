package io.github.yasmramos.veld.example.dependsOn;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.DependsOn;

/**
 * Servicio de usuario - depende explícitamente de múltiples servicios.
 * Demuestra el uso de @DependsOn con múltiples dependencias.
 * Orden requerido: DatabaseService, ConfigService, EmailService
 */
@Component("userService")
@DependsOn({"databaseService", "configService", "emailService"})
public class UserService {
    
    private DatabaseService databaseService;
    private ConfigService configService;
    private EmailService emailService;
    
    @PostConstruct
    public void init() {
        System.out.println("    ✅ UserService inicializado - Servicio completo de usuarios listo");
        
        boolean allDependenciesAvailable = true;
        
        if (databaseService != null && databaseService.isConnected()) {
            System.out.println("       ✓ DatabaseService disponible");
        } else {
            System.out.println("       ❌ DatabaseService no disponible");
            allDependenciesAvailable = false;
        }
        
        if (configService != null) {
            System.out.println("       ✓ ConfigService disponible (App: " + configService.getAppName() + ")");
        } else {
            System.out.println("       ❌ ConfigService no disponible");
            allDependenciesAvailable = false;
        }
        
        if (emailService != null) {
            System.out.println("       ✓ EmailService disponible");
        } else {
            System.out.println("       ❌ EmailService no disponible");
            allDependenciesAvailable = false;
        }
        
        if (allDependenciesAvailable) {
            System.out.println("       🎉 Todas las dependencias están disponibles - UserService completamente funcional");
        } else {
            System.out.println("       ⚠️  Algunas dependencias faltan - funcionalidad limitada");
        }
    }
    
    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
    
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
    
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    public void createUser(Long id, String name, String email) {
        System.out.println("       Creando usuario: " + name + " (" + email + ")");
        
        if (databaseService != null && databaseService.isConnected()) {
            databaseService.saveData("User: " + id + " - " + name + " - " + email);
            System.out.println("       ✓ Usuario guardado en BD");
        } else {
            System.out.println("       ❌ No se puede guardar en BD");
            return;
        }
        
        if (emailService != null) {
            emailService.sendWelcomeEmail(email, name);
        } else {
            System.out.println("       ❌ No se puede enviar email de bienvenida");
        }
        
        System.out.println("       ✅ Usuario creado exitosamente");
    }
    
    public void getUserInfo(Long id) {
        System.out.println("       Obteniendo información del usuario ID: " + id);
        
        if (databaseService != null && databaseService.isConnected()) {
            databaseService.executeQuery("SELECT * FROM users WHERE id = " + id);
            System.out.println("       ✓ Consulta ejecutada en BD");
        } else {
            System.out.println("       ❌ No se puede consultar BD");
        }
        
        if (configService != null) {
            System.out.println("       ✓ Configuración disponible para el usuario");
        } else {
            System.out.println("       ❌ Configuración no disponible");
        }
    }
    
    public void listAllUsers() {
        System.out.println("       Listando todos los usuarios:");
        
        if (databaseService != null && databaseService.isConnected()) {
            databaseService.executeQuery("SELECT * FROM users");
            System.out.println("       ✓ Usuarios obtenidos de BD");
        } else {
            System.out.println("       ❌ No se puede acceder a BD");
        }
        
        if (configService != null) {
            System.out.println("       ✓ Aplicación: " + configService.getAppName());
            System.out.println("       ✓ Entorno: " + configService.getEnvironment());
        } else {
            System.out.println("       ❌ Configuración no disponible");
        }
    }
    
    public String getUserName(Long id) {
        System.out.println("       Obteniendo nombre del usuario con ID: " + id);
        
        // Simular obtención de nombre de usuario desde la base de datos
        String userName = "Usuario_" + id;
        
        if (databaseService != null && databaseService.isConnected()) {
            databaseService.executeQuery("SELECT name FROM users WHERE id = " + id);
            System.out.println("       ✓ Consulta ejecutada en BD para obtener nombre");
        } else {
            System.out.println("       ❌ No se puede consultar BD, usando nombre simulado");
        }
        
        System.out.println("       ✓ Nombre obtenido: " + userName);
        return userName;
    }
}