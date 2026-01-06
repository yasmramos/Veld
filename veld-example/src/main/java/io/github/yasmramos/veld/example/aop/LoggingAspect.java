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
package io.github.yasmramos.veld.example.aop;

import io.github.yasmramos.veld.annotation.After;
import io.github.yasmramos.veld.annotation.AfterType;
import io.github.yasmramos.veld.annotation.Around;
import io.github.yasmramos.veld.annotation.Aspect;
import io.github.yasmramos.veld.annotation.Before;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.JoinPoint;

import java.util.Arrays;

/**
 * Example aspect demonstrating logging cross-cutting concern.
 *
 * <p>This aspect logs method entry, exit, and exceptions for
 * all methods in the example.service package.
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
@Aspect(order = 1)
public class LoggingAspect {

    /**
     * Around advice that logs method execution with timing.
     *
     * <p>Matches all public methods in CalculatorService.
     */
    @Around("execution(* io.github.yasmramos.veld.example.aop.CalculatorService.*(..))")
    public Object logCalculation(InvocationContext ctx) throws Throwable {
        String methodName = ctx.getMethodName();
        Object[] args = ctx.getParameters();
        
        System.out.printf("[CALC-LOG] Calculating %s with args: %s%n", 
                methodName, Arrays.toString(args));
        
        long start = System.nanoTime();
        try {
            Object result = ctx.proceed();
            long elapsed = System.nanoTime() - start;
            
            System.out.printf("[CALC-LOG] %s returned %s (%.3f ms)%n", 
                    methodName, result, elapsed / 1_000_000.0);
            
            return result;
        } catch (Throwable t) {
            System.out.printf("[CALC-LOG] %s threw %s%n", methodName, t.getClass().getSimpleName());
            throw t;
        }
    }

    /**
     * Before advice for ProductService methods.
     */
    @Before("execution(* io.github.yasmramos.veld.example.aop.ProductService.*(..))")
    public void logProductAccess(JoinPoint jp) {
        System.out.printf("[PRODUCT-LOG] >>> Entering %s%n", jp.toShortString());
    }

    /**
     * After advice for ProductService methods (on success).
     */
    @After(value = "execution(* io.github.yasmramos.veld.example.aop.ProductService.*(..))", 
           type = AfterType.RETURNING)
    public void logProductSuccess(JoinPoint jp, Object result) {
        System.out.printf("[PRODUCT-LOG] <<< %s completed successfully%n", jp.getMethodName());
    }

    /**
     * After advice for ProductService methods (on exception).
     */
    @After(value = "execution(* io.github.yasmramos.veld.example.aop.ProductService.*(..))", 
           type = AfterType.THROWING)
    public void logProductError(JoinPoint jp, Throwable error) {
        System.out.printf("[PRODUCT-LOG] !!! %s failed: %s%n", 
                jp.getMethodName(), error.getMessage());
    }
}
