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
 * Marks a method as an event subscriber that will be invoked when
 * matching events are published to the EventBus.
 *
 * <p>The annotated method must have exactly one parameter, which is
 * the event type it subscribes to. The method will be called whenever
 * an event of that type (or a subtype) is published.
 *
 * <h2>Usage Examples</h2>
 *
 * <p>Basic subscription:
 * <pre>{@code
 * @Component
 * public class OrderEventHandler {
 *     @Subscribe
 *     public void onOrderCreated(OrderCreatedEvent event) {
 *         System.out.println("Order created: " + event.getOrderId());
 *     }
 * }
 * }</pre>
 *
 * <p>Async subscription with priority:
 * <pre>{@code
 * @Component
 * public class EmailNotificationService {
 *     @Subscribe(async = true, priority = 10)
 *     public void sendOrderConfirmation(OrderCreatedEvent event) {
 *         emailService.send(event.getCustomerEmail(), "Order confirmed!");
 *     }
 * }
 * }</pre>
 *
 * <p>Filtered subscription:
 * <pre>{@code
 * @Component
 * public class VIPOrderHandler {
 *     @Subscribe(filter = "event.amount > 1000")
 *     public void handleVIPOrder(OrderCreatedEvent event) {
 *         // Only called for orders over $1000
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see io.github.yasmramos.veld.runtime.event.EventBus
 * @see io.github.yasmramos.veld.runtime.event.Event
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * Whether this subscriber should be invoked asynchronously.
     *
     * <p>When {@code true}, the event handler will be executed in a
     * separate thread, allowing the publisher to continue without
     * waiting for the handler to complete.
     *
     * <p>Default is {@code false} (synchronous execution).
     *
     * @return {@code true} for async execution, {@code false} for sync
     */
    boolean async() default false;

    /**
     * The priority of this subscriber. Higher values indicate higher priority.
     *
     * <p>Subscribers with higher priority will be invoked before those
     * with lower priority when multiple subscribers handle the same event.
     *
     * <p>Default priority is {@code 0}.
     *
     * @return the priority value
     */
    int priority() default 0;

    /**
     * A SpEL-like filter expression to conditionally invoke this subscriber.
     *
     * <p>The expression has access to the {@code event} variable representing
     * the published event. Only when the expression evaluates to {@code true}
     * will the subscriber be invoked.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "event.amount > 100"} - filter by event property</li>
     *   <li>{@code "event.type == 'PREMIUM'"} - filter by string equality</li>
     *   <li>{@code "event.priority >= 5"} - filter by numeric comparison</li>
     * </ul>
     *
     * <p>Default is empty string (no filter, always invoke).
     *
     * @return the filter expression
     */
    String filter() default "";

    /**
     * Whether to catch and log exceptions thrown by this subscriber
     * instead of propagating them.
     *
     * <p>When {@code true}, exceptions are caught, logged, and the
     * event continues to be delivered to other subscribers.
     *
     * <p>When {@code false}, exceptions propagate and may prevent
     * delivery to subsequent subscribers.
     *
     * <p>Default is {@code true} (exceptions are caught).
     *
     * @return {@code true} to catch exceptions, {@code false} to propagate
     */
    boolean catchExceptions() default true;
}
