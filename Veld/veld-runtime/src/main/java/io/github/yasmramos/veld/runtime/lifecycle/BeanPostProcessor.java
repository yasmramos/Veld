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
package io.github.yasmramos.veld.runtime.lifecycle;

/**
 * Interface for factories that can modify bean instances before and after
 * initialization.
 *
 * <p>Bean post-processors are powerful extension points that allow for
 * custom modification of beans. Common use cases include:
 * <ul>
 *   <li>Wrapping beans with proxies (AOP, transactions)</li>
 *   <li>Populating beans with additional properties</li>
 *   <li>Validating bean configuration</li>
 *   <li>Custom initialization logic</li>
 * </ul>
 *
 * <h2>Execution Order</h2>
 * <ol>
 *   <li>Bean instantiation</li>
 *   <li>Dependency injection</li>
 *   <li>{@code postProcessBeforeInitialization()}</li>
 *   <li>{@code InitializingBean.afterPropertiesSet()}</li>
 *   <li>{@code @PostConstruct} methods</li>
 *   <li>{@code postProcessAfterInitialization()}</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class LoggingBeanPostProcessor implements BeanPostProcessor {
 *     
 *     @Override
 *     public Object postProcessBeforeInitialization(Object bean, String beanName) {
 *         System.out.println("Before init: " + beanName);
 *         return bean;
 *     }
 *     
 *     @Override
 *     public Object postProcessAfterInitialization(Object bean, String beanName) {
 *         System.out.println("After init: " + beanName);
 *         // Optionally wrap with proxy
 *         if (bean.getClass().isAnnotationPresent(Logged.class)) {
 *             return createLoggingProxy(bean);
 *         }
 *         return bean;
 *     }
 * }
 * }</pre>
 *
 * <h2>Proxy Creation</h2>
 * <p>Post-processors can return a different object than the original bean.
 * This is commonly used for AOP proxy creation:
 * <pre>{@code
 * @Override
 * public Object postProcessAfterInitialization(Object bean, String beanName) {
 *     if (requiresProxy(bean)) {
 *         return proxyFactory.createProxy(bean);
 *     }
 *     return bean;
 * }
 * }</pre>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see InitializingBean
 */
public interface BeanPostProcessor {
    
    /**
     * Apply this processor to the given bean instance BEFORE any
     * initialization callbacks (like InitializingBean or @PostConstruct).
     *
     * <p>The returned bean instance may be the original or a wrapped one.
     * If {@code null} is returned, the original bean is used.
     *
     * @param bean the new bean instance
     * @param beanName the name of the bean
     * @return the bean instance to use (original or wrapped)
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
    
    /**
     * Apply this processor to the given bean instance AFTER any
     * initialization callbacks (like InitializingBean or @PostConstruct).
     *
     * <p>The returned bean instance may be the original or a wrapped one.
     * If {@code null} is returned, the original bean is used.
     *
     * <p>This is the ideal place to wrap beans with proxies, as all
     * initialization has completed.
     *
     * @param bean the new bean instance
     * @param beanName the name of the bean
     * @return the bean instance to use (original or wrapped/proxied)
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
    
    /**
     * Returns the order of this processor.
     * Lower values are processed first.
     *
     * @return the order value (default 0)
     */
    default int getOrder() {
        return 0;
    }
}
