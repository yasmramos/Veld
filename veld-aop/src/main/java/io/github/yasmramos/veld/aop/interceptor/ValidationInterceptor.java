/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.aop.interceptor;

import io.github.yasmramos.veld.annotation.AroundInvoke;
import io.github.yasmramos.veld.annotation.Interceptor;
import io.github.yasmramos.veld.aop.InvocationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Interceptor that validates method arguments.
 *
 * <p>Performs null checks and basic validation on method parameters.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
@Interceptor(priority = 200)
@Validated
public class ValidationInterceptor {

    @AroundInvoke
    public Object validateArguments(InvocationContext ctx) throws Throwable {
        Method method = ctx.getMethod();
        Object[] params = ctx.getParameters();
        Parameter[] paramDefs = method.getParameters();

        // Get annotation configuration
        Validated config = method.getAnnotation(Validated.class);
        if (config == null) {
            config = method.getDeclaringClass().getAnnotation(Validated.class);
        }

        boolean strict = config != null ? config.strict() : true;
        String customMessage = config != null ? config.message() : "";

        List<String> violations = new ArrayList<>();

        for (int i = 0; i < params.length; i++) {
            Parameter paramDef = paramDefs[i];
            Object paramValue = params[i];
            String paramName = paramDef.getName();

            // Check for null
            if (paramValue == null) {
                // Check if @Optional or nullable
                if (!isOptional(paramDef)) {
                    violations.add(String.format("Parameter '%s' (index %d) cannot be null", 
                            paramName, i));
                }
                continue;
            }

            // Check empty strings (Java 17 pattern matching)
            if (paramValue instanceof String strValue && strValue.isEmpty() && hasNotEmptyAnnotation(paramDef)) {
                violations.add(String.format("Parameter '%s' cannot be empty", paramName));
            }

            // Check empty collections (Java 17 pattern matching)
            if (paramValue instanceof Collection<?> collValue && collValue.isEmpty() && hasNotEmptyAnnotation(paramDef)) {
                violations.add(String.format("Parameter '%s' cannot be empty", paramName));
            }
        }

        if (!violations.isEmpty()) {
            String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
            String message = customMessage.isEmpty() 
                    ? String.format("Validation failed for %s: %s", methodName, violations)
                    : customMessage + ": " + violations;

            if (strict) {
                throw new IllegalArgumentException(message);
            } else {
                System.out.println("[VALIDATION] WARNING: " + message);
            }
        }

        return ctx.proceed();
    }

    private boolean isOptional(Parameter param) {
        for (Annotation ann : param.getAnnotations()) {
            String annName = ann.annotationType().getSimpleName();
            if (annName.equals("Optional") || annName.equals("Nullable")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNotEmptyAnnotation(Parameter param) {
        for (Annotation ann : param.getAnnotations()) {
            String annName = ann.annotationType().getSimpleName();
            if (annName.equals("NotEmpty") || annName.equals("NotBlank")) {
                return true;
            }
        }
        return false;
    }
}
