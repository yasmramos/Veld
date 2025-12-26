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

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Interceptor that logs method invocations.
 *
 * <p>Logs method entry with arguments, exit with return value or exception,
 * and optionally execution time.
 *
 * <p>This implementation uses a hybrid approach: it prefers zero-reflection
 * methods but falls back to Method object when available for annotation access.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
@Interceptor(priority = 100)
@Logged
public class LoggingInterceptor {

    @AroundInvoke
    public Object logMethodCall(InvocationContext ctx) throws Throwable {
        // Zero-reflection: use context methods for method identification
        String methodName = ctx.getDeclaringClass().getSimpleName() + "." + ctx.getMethodName();
        
        // Get annotation configuration - try method first, then class
        Logged config = null;
        boolean isVoidMethod = false;
        
        // Fallback to Method object if available (for test compatibility)
        Method method = ctx.getMethod();
        if (method != null) {
            config = method.getAnnotation(Logged.class);
            isVoidMethod = method.getReturnType() == void.class;
        }
        
        // If no method-level annotation, check class
        if (config == null) {
            config = ctx.getDeclaringClass().getAnnotation(Logged.class);
        }

        boolean logArgs = config != null ? config.logArgs() : true;
        boolean logResult = config != null ? config.logResult() : true;
        boolean logTime = config != null ? config.logTime() : false;

        // Log entry
        if (logArgs) {
            System.out.printf("[LOG] >>> Entering %s with args: %s%n",
                    methodName, Arrays.toString(ctx.getParameters()));
        } else {
            System.out.printf("[LOG] >>> Entering %s%n", methodName);
        }

        long startTime = logTime ? System.nanoTime() : 0;

        try {
            Object result = ctx.proceed();

            // Log exit
            if (logResult && !isVoidMethod && result != null) {
                System.out.printf("[LOG] <<< Exiting %s with result: %s%n", methodName, result);
            } else {
                System.out.printf("[LOG] <<< Exiting %s%n", methodName);
            }

            if (logTime) {
                long elapsed = System.nanoTime() - startTime;
                System.out.printf("[LOG] --- %s took %.3f ms%n", methodName, elapsed / 1_000_000.0);
            }

            return result;
        } catch (Throwable t) {
            System.out.printf("[LOG] !!! %s threw exception: %s%n", methodName, t.getMessage());
            throw t;
        }
    }
}
