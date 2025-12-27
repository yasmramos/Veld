package io.github.yasmramos.veld.security;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.aop.AspectHandler;
import io.github.yasmramos.veld.aop.MethodInvocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Handles security annotations for method access control.
 */
public class SecuredHandler implements AspectHandler {

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Secured.class;
    }

    @Override
    public Object handle(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        
        // Check @DenyAll first
        if (method.isAnnotationPresent(DenyAll.class)) {
            throw new AccessDeniedException("Access denied");
        }
        
        // Check @PermitAll
        if (method.isAnnotationPresent(PermitAll.class)) {
            return invocation.proceed();
        }
        
        // Require authentication
        if (!SecurityContext.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        
        // Check @RolesAllowed
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
            if (!SecurityContext.hasAnyRole(rolesAllowed.value())) {
                throw new AccessDeniedException("Required roles: " + String.join(", ", rolesAllowed.value()));
            }
        }
        
        // Check @Secured roles
        if (method.isAnnotationPresent(Secured.class)) {
            Secured secured = method.getAnnotation(Secured.class);
            if (secured.value().length > 0 && !SecurityContext.hasAnyRole(secured.value())) {
                throw new AccessDeniedException("Required roles: " + String.join(", ", secured.value()));
            }
        }
        
        return invocation.proceed();
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) { super(message); }
    }
}
