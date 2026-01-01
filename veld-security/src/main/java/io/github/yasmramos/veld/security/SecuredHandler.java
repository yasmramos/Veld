package io.github.yasmramos.veld.security;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

public class SecuredHandler implements MethodInterceptor {
    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        if (ctx.hasAnnotation(DenyAll.class)) throw new AccessDeniedException("Access denied");
        if (ctx.hasAnnotation(PermitAll.class)) return ctx.proceed();
        if (!SecurityContext.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        if (ctx.hasAnnotation(RolesAllowed.class)) {
            RolesAllowed r = ctx.getAnnotation(RolesAllowed.class);
            if (!SecurityContext.hasAnyRole(r.value())) throw new AccessDeniedException("Required roles: " + String.join(", ", r.value()));
        }
        if (ctx.hasAnnotation(Secured.class)) {
            Secured s = ctx.getAnnotation(Secured.class);
            if (s.value().length > 0 && !SecurityContext.hasAnyRole(s.value())) throw new AccessDeniedException("Required roles: " + String.join(", ", s.value()));
        }
        return ctx.proceed();
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) { super(message); }
    }
}
