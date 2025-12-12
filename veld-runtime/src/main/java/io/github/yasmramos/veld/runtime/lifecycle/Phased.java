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
 * Interface for objects that participate in ordered startup/shutdown.
 *
 * <p>The phase value determines the order:
 * <ul>
 *   <li>Lower phase = starts earlier, stops later</li>
 *   <li>Higher phase = starts later, stops earlier</li>
 * </ul>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see SmartLifecycle
 */
public interface Phased {
    
    /**
     * Returns the phase value for this object.
     *
     * @return the phase value
     */
    int getPhase();
}
