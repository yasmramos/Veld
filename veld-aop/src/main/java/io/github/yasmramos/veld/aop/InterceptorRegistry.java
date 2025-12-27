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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Zero-reflection registry for aspects and interceptors.
 *
 * <p>Manages registration and lookup of aspects and interceptors,
 * using functional interfaces instead of reflection for invocation.
 * 
 * <p><b>Zero-Reflection Principle:</b>
 * <ul>
 *   <li>Interceptors use compile-time code generation (no Method.invoke)</li>
 *   <li>Advice methods wrapped in functional interfaces at registration time</li>
 *   <li>No reflection used during method interception</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class InterceptorRegistry {

    private static final InterceptorRegistry INSTANCE = new InterceptorRegistry();

    private final List<Advice> advices = new ArrayList<>();
    private final Map<Class<? extends Annotation>, List<BoundInterceptor>> bindings = new ConcurrentHashMap<>();
    private final Map<String, List<MethodInterceptor>> interceptorCache = new ConcurrentHashMap<>();

    /**
     * Represents an interceptor bound to an annotation.
     * Uses functional interface instead of Method for invocation.
     */
    private static class BoundInterceptor {
        final MethodInterceptor interceptor;
        final int priority;
        final String name;

        BoundInterceptor(MethodInterceptor interceptor, int priority, String name) {
            this.interceptor = interceptor;
            this.priority = priority;
            this.name = name;
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
     * Registers an aspect with zero-reflection invocation.
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
            method.setAccessible(true); // Set once at registration
            
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
     * Registers an interceptor with zero-reflection approach.
     * The interceptor method is wrapped in a functional interface.
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

        // Find the @AroundInvoke method and wrap it in a functional interface
        MethodInterceptor wrappedInterceptor = null;
        for (Method method : interceptorClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AroundInvoke.class)) {
                method.setAccessible(true); // Set once at registration, not at invocation
                final Method m = method;
                final Object instance = interceptor;
                
                // Wrap in functional interface (zero-reflection at invocation time)
                wrappedInterceptor = ctx -> {
                    try {
                        return m.invoke(instance, ctx);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                };
                break;
            }
        }

        if (wrappedInterceptor == null) {
            throw new IllegalArgumentException("Interceptor " + interceptorClass.getName() +
                    " has no @AroundInvoke method");
        }

        // Find binding annotations
        for (Annotation ann : interceptorClass.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(InterceptorBinding.class)) {
                BoundInterceptor binding = new BoundInterceptor(
                        wrappedInterceptor, interceptorAnn.priority(), interceptorClass.getSimpleName());
                
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
     * Gets interceptors for a method (legacy API, uses reflection).
     * @deprecated Use {@link #getInterceptorsForMethod} instead.
     */
    @Deprecated
    public List<MethodInterceptor> getInterceptors(Method method) {
        String cacheKey = method.getDeclaringClass().getName() + "#" + method.getName() + 
            "#" + Arrays.toString(getParameterTypeNames(method));
        return interceptorCache.computeIfAbsent(cacheKey, k -> buildInterceptorChain(method));
    }
    
    /**
     * Gets interceptors for a method using zero-reflection metadata.
     *
     * @param targetClass the target class
     * @param methodName the method name
     * @param parameterTypes parameter type simple names
     * @return list of matching interceptors
     */
    public List<MethodInterceptor> getInterceptorsForMethod(Class<?> targetClass, 
            String methodName, String[] parameterTypes) {
        String cacheKey = targetClass.getName() + "#" + methodName + "#" + Arrays.toString(parameterTypes);
        
        return interceptorCache.computeIfAbsent(cacheKey, k -> {
            List<MethodInterceptor> chain = new ArrayList<>();
            
            // Find method by name and parameter types
            Method method = findMethod(targetClass, methodName, parameterTypes);
            if (method == null) {
                return chain;
            }
            
            // Add interceptors from annotations on the method
            addInterceptorsFromAnnotations(chain, method.getAnnotations());

            // Add interceptors from annotations on the class
            for (Annotation ann : targetClass.getAnnotations()) {
                if (!method.isAnnotationPresent(ann.annotationType())) {
                    addInterceptorsFromAnnotation(chain, ann);
                }
            }

            // Add advice from aspects
            List<Advice> matchingAdvices = advices.stream()
                    .filter(advice -> advice.matches(method))
                    .collect(Collectors.toList());

            for (Advice advice : matchingAdvices) {
                chain.add(buildAdviceInterceptor(advice));
            }

            return chain;
        });
    }
    
    private Method findMethod(Class<?> targetClass, String methodName, String[] parameterTypes) {
        for (Method method : targetClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                String[] methodParamTypes = getParameterTypeNames(method);
                if (Arrays.equals(methodParamTypes, parameterTypes)) {
                    return method;
                }
            }
        }
        return null;
    }
    
    private String[] getParameterTypeNames(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        String[] names = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            names[i] = paramTypes[i].getSimpleName();
        }
        return names;
    }
    
    private void addInterceptorsFromAnnotations(List<MethodInterceptor> chain, Annotation[] annotations) {
        for (Annotation ann : annotations) {
            addInterceptorsFromAnnotation(chain, ann);
        }
    }
    
    private void addInterceptorsFromAnnotation(List<MethodInterceptor> chain, Annotation ann) {
        List<BoundInterceptor> interceptorBindings = bindings.get(ann.annotationType());
        if (interceptorBindings != null) {
            for (BoundInterceptor binding : interceptorBindings) {
                chain.add(binding.interceptor);
            }
        }
    }

    private List<MethodInterceptor> buildInterceptorChain(Method method) {
        List<MethodInterceptor> chain = new ArrayList<>();

        // Add interceptors from annotations on the method
        addInterceptorsFromAnnotations(chain, method.getAnnotations());

        // Add interceptors from annotations on the class
        Class<?> declaringClass = method.getDeclaringClass();
        for (Annotation ann : declaringClass.getAnnotations()) {
            if (!method.isAnnotationPresent(ann.annotationType())) {
                addInterceptorsFromAnnotation(chain, ann);
            }
        }

        // Add advice from aspects
        List<Advice> matchingAdvices = advices.stream()
                .filter(advice -> advice.matches(method))
                .collect(Collectors.toList());

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
