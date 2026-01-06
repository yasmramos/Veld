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
package io.github.yasmramos.veld.runtime.event;

/**
 * Service Provider Interface for event registration.
 *
 * <p>This interface is implemented by generated code at compile time
 * to provide zero-reflection event subscription registration.</p>
 *
 * <h2>Purpose</h2>
 * <p>Instead of using reflection at runtime to discover @Subscribe methods,
 * this interface allows the framework to generate type-safe registration
 * code during compilation. This eliminates the need for reflection and
 * improves GraalVM Native Image compatibility.</p>
 *
 * <h2>Generated Code Example</h2>
 * <pre>{@code
 * public class VeldEventRegistryImpl implements EventRegistrationSPI {
 *     @Override
 *     public void registerEvents(EventBus bus, Object component) {
 *         if (component instanceof UserService) {
 *             UserService userService = (UserService) component;
 *             bus.register(1001, userService::onUserCreated);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Zero-Reflection Benefits</h2>
 * <ul>
 *   <li>No Class.getDeclaredMethods() at runtime</li>
 *   <li>No Method.getAnnotation() calls</li>
 *   <li>No Method.invoke() overhead</li>
 *   <li>Better GraalVM optimization</li>
 *   <li>Smaller native image</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see EventBus
 * @see io.github.yasmramos.veld.annotation.Subscribe
 */
public interface EventRegistrationSPI {

    /**
     * Registers all event subscribers from a component.
     *
     * <p>This method is called by {@link EventBus#register(Object)} to delegate
     * the subscription registration to generated code. The generated implementation
     * uses type checks and direct method references instead of reflection.</p>
     *
     * <h2>Implementation Pattern</h2>
     * <p>The generated code should follow this pattern:</p>
     * <pre>{@code
     * if (component instanceof SpecificComponent) {
     *     SpecificComponent typed = (SpecificComponent) component;
     *     bus.register(eventTypeId, typed::handlerMethod);
     * }
     * }</pre>
     *
     * @param bus       the EventBus to register subscriptions with
     * @param component the component object to scan for event handlers
     */
    void registerEvents(EventBus bus, Object component);

    /**
     * Returns the number of event handlers registered by this registry.
     *
     * @return the total count of registered event handlers
     */
    default int getHandlerCount() {
        return 0;
    }
}
