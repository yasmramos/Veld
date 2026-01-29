/*
 * Copyright 2024-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.annotation;

/**
 * Service Provider Interface for event registration in the Veld framework.
 *
 * <p>This interface is used by the framework to register event handlers
 * at compile time, eliminating the need for reflection at runtime.</p>
 *
 * <p>The generated implementation provides type-safe event handler registration
 * by generating specific instanceof checks and cast operations for each
 * component that subscribes to events.</p>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
public interface EventRegistrationSPI {

    /**
     * Registers all event handlers for the given component instance.
     *
     * <p>This method is called by the framework during initialization to
     * register all @Subscribe annotated methods from the component.</p>
     *
     * @param bus the event bus to register handlers with (cast to actual EventBus type at runtime)
     * @param component the component instance to process
     */
    void registerEvents(Object bus, Object component);

    /**
     * Returns the total number of event handlers registered by this registry.
     *
     * <p>This count can be used for logging, testing, or validation purposes
     * to verify that all expected handlers were registered.</p>
     *
     * @return the number of registered event handlers
     */
    int getHandlerCount();

}
