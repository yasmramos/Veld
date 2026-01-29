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
package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called when the application context starts.
 *
 * <p>This annotation is used for components that need to perform actions
 * when the application starts, such as starting background tasks,
 * establishing connections, or initializing resources.
 *
 * <h2>Lifecycle Order</h2>
 * <ol>
 *   <li>Container initialization</li>
 *   <li>{@code @PostConstruct} methods</li>
 *   <li>{@code @PostInitialize} methods</li>
 *   <li>Context start ({@code @OnStart} methods - this annotation)</li>
 *   <li>Application running</li>
 *   <li>Context stop ({@code @OnStop} methods)</li>
 *   <li>{@code @PreDestroy} methods</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class SchedulerService {
 *     
 *     private ScheduledExecutorService executor;
 *     
 *     @OnStart
 *     public void startScheduler() {
 *         executor = Executors.newScheduledThreadPool(4);
 *         executor.scheduleAtFixedRate(
 *             this::runTask, 0, 1, TimeUnit.MINUTES
 *         );
 *     }
 *     
 *     @OnStop
 *     public void stopScheduler() {
 *         executor.shutdown();
 *     }
 * }
 * }</pre>
 *
 * <h2>Method Requirements</h2>
 * <ul>
 *   <li>Must be public or package-private</li>
 *   <li>Must have no parameters</li>
 *   <li>Return type is typically void</li>
 *   <li>May throw exceptions</li>
 * </ul>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see OnStop
 * @see PostInitialize
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface OnStart {
    
    /**
     * Execution order among multiple {@code @OnStart} methods.
     * Lower values execute first.
     *
     * @return the order value (default 0)
     */
    int order() default 0;
}
