# Veld Security

The `veld-security` module provides security features for the Veld Framework.

## Features

- Method-level security
- Role-based access control
- Custom security expressions
- Integration with AOP

## Available Annotations

### @Secured
Restricts access to a method based on roles.

```java
@Secured("ADMIN")
public void adminOnlyMethod() {
    // ...
}
```

### @PreAuthorize
Evaluates a security expression before method execution.

```java
@PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
public void getUserData(String userId) {
    // ...
}
```

### @PostAuthorize
Evaluates a security expression after method execution.

```java
@PostAuthorize("returnObject.owner == authentication.principal")
public Document getDocument(String id) {
    // ...
}
```

### @DenyAll
Denies all access to a method.

```java
@DenyAll
public void neverAccessible() {
    // ...
}
```

### @PermitAll
Allows all access to a method.

```java
@PermitAll
public String publicMethod() {
    // ...
}
```

## Configuration

### Security Configuration

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityManager securityManager() {
        return new DefaultSecurityManager();
    }
    
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
            ROLE_ADMIN > ROLE_USER
            ROLE_USER > ROLE_GUEST
            """);
    }
}
```

### Method Security

Enable method security in your configuration:

```java
@EnableMethodSecurity
public class AppConfig {
    // ...
}
```

## Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-security</artifactId>
    <version>1.0.4</version>
</dependency>
```

## Custom Security Expressions

Create custom security expressions:

```java
@Component("customSecurity")
public class CustomSecurityExpressions {
    
    public boolean isOwner(Document doc, Authentication auth) {
        return doc.getOwnerId().equals(auth.getPrincipal().getId());
    }
}
```

Use in annotations:

```java
@PreAuthorize("@customSecurity.isOwner(#doc, authentication)")
public void updateDocument(Document doc) {
    // ...
}
```

## Error Handling

Security violations throw `AccessDeniedException`:

```java
try {
    securedService.adminMethod();
} catch (AccessDeniedException e) {
    // Handle access denied
}
```
