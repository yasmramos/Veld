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

import java.lang.annotation.*;

/**
 * Records the execution time of a method as a metric.
 *
 * <p>The timing information is exposed via the metrics registry and can be
 * exported to monitoring systems like Prometheus, Graphite, etc.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class OrderService {
 *     
 *     @Timed("orders.create")
 *     public Order createOrder(OrderRequest request) {
 *         return orderRepository.save(new Order(request));
 *     }
 *     
 *     @Timed(value = "orders.process", description = "Time to process an order",
 *            percentiles = {0.5, 0.95, 0.99})
 *     public void processOrder(Order order) {
 *         // Processing logic
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {
    
    /**
     * Metric name.
     *
     * @return metric name
     */
    String value() default "";
    
    /**
     * Description of the metric.
     *
     * @return description
     */
    String description() default "";
    
    /**
     * Additional tags for the metric.
     * Format: {"key1", "value1", "key2", "value2"}
     *
     * @return extra tags
     */
    String[] extraTags() default {};
    
    /**
     * Percentiles to compute (e.g., 0.5, 0.95, 0.99).
     *
     * @return percentiles
     */
    double[] percentiles() default {};
    
    /**
     * Whether to publish histogram data.
     *
     * @return true to publish histogram
     */
    boolean histogram() default false;
    
    /**
     * Whether to record long task timing.
     *
     * @return true for long task timing
     */
    boolean longTask() default false;
}
