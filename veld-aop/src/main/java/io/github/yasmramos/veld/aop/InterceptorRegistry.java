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
package io.github.yasmramos.veld.aop;

import io.github.yasmramos.veld.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for aspects and interceptors.
 *
 * <p>Manages registration and lookup of aspects and interceptors,
 * and matches them to target methods based on pointcut expressions.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class InterceptorRegistry {

    private static final InterceptorRegistry INSTANCE = new InterceptorRegistry();

    private final List<Advice> advices = new ArrayList<>();
    private final Map<Class<? extends Annotation>, List<BoundInterceptor>> bindings = new ConcurrentHashMap<>();
    private final Map<Method, List<MethodInterceptor>> interceptorCache = new ConcurrentHashMap<>();

    /**
     * Represents an interceptor bound to an annotation.
     */
    private static class BoundInterceptor {
        final Object interceptorInstance;
        final Method aroundInvokeMethod;
        final int priority;

        BoundInterceptor(Object instance, Method method, int priority) {
            this.interceptorInstance = instance;
            this.aroundInvokeMethod = method;
            this.priority = priority;
        }
    }

    private InterceptorRegistry() {}

    /**
     * Returns the singleton instance.
     *
     * @return the registry instance
     */
    public static InterceptorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers an aspect.
     *
     * @param aspect the aspect instance
     */
    public void registerAspect(Object aspect) {
        Class<?> aspectClass = aspect.getClass();
        Aspect aspectAnn = aspectClass.getAnnotation(Aspect.class);
        
        if (aspectAnn == null) {
            throw new IllegalArgumentException("Class " + aspectClass.getName() +
                    " is not annotated with @Aspect");
        }

        int order = aspectAnn.order();

        for (Method method : aspectClass.getDeclaredMethods()) {
            // Check for @Around
            Around around = method.getAnnotation(Around.class);
            if (around != null) {
                advices.add(Advice.around(around.value(), aspect, method, order));
                continue;
            }

            // Check for @Before
            Before before = method.getAnnotation(Before.class);
            if (before != null) {
                advices.add(Advice.before(before.value(), aspect, method, order));
                continue;
            }

            // Check for @After
            After after = method.getAnnotation(After.class);
            if (after != null) {
                advices.add(Advice.after(after.value(), after.type(), aspect, method, order));
            }
        }

        // Sort advices by order
        Collections.sort(advices);
        
        // Clear cache since new aspect was added
        interceptorCache.clear();

        System.out.println("[AOP] Registered aspect: " + aspectClass.getSimpleName() +
                " with " + countAdvicesFor(aspect) + " advices");
    }

    private int countAdvicesFor(Object aspect) {
        return (int) advices.stream()
                .filter(a -> a.getAspectInstance() == aspect)
                .count();
    }

    /**
     * Registers an interceptor.
     *
     * @param interceptor the interceptor instance
     */
    public void registerInterceptor(Object interceptor) {
        Class<?> interceptorClass = interceptor.getClass();
        Interceptor interceptorAnn = interceptorClass.getAnnotation(Interceptor.class);
        
        if (interceptorAnn == null) {
            throw new IllegalArgumentException("Class " + interceptorClass.getName() +
                    " is not annotated with @Interceptor");
        }

        // Find the @AroundInvoke method
        Method aroundInvokeMethod = null;
        for (Method method : interceptorClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AroundInvoke.class)) {
                aroundInvokeMethod = method;
                aroundInvokeMethod.setAccessible(true);
                break;
            }
        }

        if (aroundInvokeMethod == null) {
            throw new IllegalArgumentException("Interceptor " + interceptorClass.getName() +
                    " has no @AroundInvoke method");
        }

        // Find binding annotations
        for (Annotation ann : interceptorClass.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(InterceptorBinding.class)) {
                BoundInterceptor binding = new BoundInterceptor(
                        interceptor, aroundInvokeMethod, interceptorAnn.priority());
                
                bindings.computeIfAbsent(ann.annotationType(), k -> new ArrayList<>())
                        .add(binding);
                
                System.out.println("[AOP] Registered interceptor: " + interceptorClass.getSimpleName() +
                        " for binding @" + ann.annotationType().getSimpleName());
            }
        }

        // Clear cache since new interceptor was added
        interceptorCache.clear();
    }

    /**
     * Gets interceptors for a method.
     *
     * @param method the target method
     * @return list of matching interceptors
     */
    public List<MethodInterceptor> getInterceptors(Method method) {
        return interceptorCache.computeIfAbsent(method, this::buildInterceptorChain);
    }

    private List<MethodInterceptor> buildInterceptorChain(Method method) {
        List<MethodInterceptor> chain = new ArrayList<>();

        // Add interceptors from annotations on the method
        for (Annotation ann : method.getAnnotations()) {
            List<BoundInterceptor> interceptorBindings = bindings.get(ann.annotationType());
            if (interceptorBindings != null) {
                for (BoundInterceptor binding : interceptorBindings) {
                    chain.add(ctx -> binding.aroundInvokeMethod.invoke(
                            binding.interceptorInstance, ctx));
                }
            }
        }

        // Add interceptors from annotations on the class
        Class<?> declaringClass = method.getDeclaringClass();
        for (Annotation ann : declaringClass.getAnnotations()) {
            List<BoundInterceptor> interceptorBindings = bindings.get(ann.annotationType());
            if (interceptorBindings != null) {
                for (BoundInterceptor binding : interceptorBindings) {
                    // Check if not already added from method
                    if (!method.isAnnotationPresent(ann.annotationType())) {
                        chain.add(ctx -> binding.aroundInvokeMethod.invoke(
                                binding.interceptorInstance, ctx));
                    }
                }
            }
        }

        // Add advice from aspects
        List<Advice> matchingAdvices = advices.stream()
                .filter(advice -> advice.matches(method))
                .collect(Collectors.toList());

        // Build interceptors from advices
        for (Advice advice : matchingAdvices) {
            chain.add(buildAdviceInterceptor(advice));
        }

        return chain;
    }

    private MethodInterceptor buildAdviceInterceptor(Advice advice) {
        switch (advice.getType()) {
            case AROUND:
                return ctx -> advice.invoke(ctx);
                
            case BEFORE:
                return ctx -> {
                    advice.invoke(createJoinPoint(ctx));
                    return ctx.proceed();
                };
                
            case AFTER_RETURNING:
                return ctx -> {
                    Object result = ctx.proceed();
                    advice.invoke(createJoinPoint(ctx), result);
                    return result;
                };
                
            case AFTER_THROWING:
                return ctx -> {
                    try {
                        return ctx.proceed();
                    } catch (Throwable t) {
                        advice.invoke(createJoinPoint(ctx), t);
                        throw t;
                    }
                };
                
            case AFTER_FINALLY:
                return ctx -> {
                    try {
                        return ctx.proceed();
                    } finally {
                        advice.invoke(createJoinPoint(ctx));
                    }
                };
                
            default:
                return InvocationContext::proceed;
        }
    }

    private JoinPoint createJoinPoint(InvocationContext ctx) {
        return ctx; // InvocationContext extends JoinPoint
    }

    /**
     * Checks if a class has any matching advices.
     *
     * @param clazz the class to check
     * @return true if any advice could apply
     */
    public boolean hasAdvicesFor(Class<?> clazz) {
        // Check if class has any interceptor binding annotations
        for (Annotation ann : clazz.getAnnotations()) {
            if (bindings.containsKey(ann.annotationType())) {
                return true;
            }
        }

        // Check if any methods have interceptor binding annotations
        for (Method method : clazz.getDeclaredMethods()) {
            for (Annotation ann : method.getAnnotations()) {
                if (bindings.containsKey(ann.annotationType())) {
                    return true;
                }
            }
        }

        // Check aspects
        return advices.stream().anyMatch(advice -> advice.couldMatch(clazz));
    }

    /**
     * Returns statistics about registered aspects and interceptors.
     *
     * @return statistics string
     */
    public String getStatistics() {
        long aspectCount = advices.stream()
                .map(Advice::getAspectInstance)
                .distinct()
                .count();
        
        return String.format(
                "InterceptorRegistry: %d aspects, %d advices, %d interceptor bindings, %d cached methods",
                aspectCount, advices.size(), bindings.size(), interceptorCache.size());
    }

    /**
     * Clears all registrations.
     */
    public void clear() {
        advices.clear();
        bindings.clear();
        interceptorCache.clear();
    }
}
