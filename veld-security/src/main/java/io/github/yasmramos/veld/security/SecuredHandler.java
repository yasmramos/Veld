package io.github.yasmramos.veld.security;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

import java.lang.reflect.Method;

public class SecuredHandler implements MethodInterceptor {
    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        Method method = ctx.getMethod();
        if (method.isAnnotationPresent(DenyAll.class)) throw new AccessDeniedException("Access denied");
        if (method.isAnnotationPresent(PermitAll.class)) return ctx.proceed();
        if (!SecurityContext.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed r = method.getAnnotation(RolesAllowed.class);
            if (!SecurityContext.hasAnyRole(r.value())) throw new AccessDeniedException("Required roles: " + String.join(", ", r.value()));
        }
        if (method.isAnnotationPresent(Secured.class)) {
            Secured s = method.getAnnotation(Secured.class);
            if (s.value().length > 0 && !SecurityContext.hasAnyRole(s.value())) throw new AccessDeniedException("Required roles: " + String.join(", ", s.value()));
        }
        return ctx.proceed();
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) { super(message); }
    }
}
