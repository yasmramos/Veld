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
 * Marks a method to be called when the application context stops.
 *
 * <p>This annotation is used for components that need to perform cleanup
 * actions when the application stops, such as stopping background tasks,
 * closing connections, or releasing resources.
 *
 * <h2>Lifecycle Order</h2>
 * <ol>
 *   <li>Application running</li>
 *   <li>Context stop requested</li>
 *   <li>{@code @OnStop} methods (this annotation) - in reverse order</li>
 *   <li>{@code @PreDestroy} methods</li>
 *   <li>Container closed</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class MessageConsumer {
 *     
 *     private volatile boolean running = true;
 *     private Thread consumerThread;
 *     
 *     @OnStart
 *     public void startConsuming() {
 *         consumerThread = new Thread(() -> {
 *             while (running) {
 *                 processMessages();
 *             }
 *         });
 *         consumerThread.start();
 *     }
 *     
 *     @OnStop
 *     public void stopConsuming() {
 *         running = false;
 *         consumerThread.interrupt();
 *     }
 * }
 * }</pre>
 *
 * <h2>Method Requirements</h2>
 * <ul>
 *   <li>Must be public or package-private</li>
 *   <li>Must have no parameters</li>
 *   <li>Return type is typically void</li>
 *   <li>Should not throw exceptions (log them instead)</li>
 * </ul>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see OnStart
 * @see PreDestroy
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface OnStop {
    
    /**
     * Execution order among multiple {@code @OnStop} methods.
     * Higher values execute first (reverse of start order).
     *
     * @return the order value (default 0)
     */
    int order() default 0;
}
