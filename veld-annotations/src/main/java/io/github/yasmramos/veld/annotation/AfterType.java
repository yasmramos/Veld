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

/**
 * Specifies when an {@link After} advice should execute.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 * @see After
 */
public enum AfterType {

    /**
     * Execute after the method returns normally (no exception).
     * The advice can access the return value.
     */
    RETURNING,

    /**
     * Execute after the method throws an exception.
     * The advice can access the thrown exception.
     */
    THROWING,

    /**
     * Execute after the method completes, regardless of outcome.
     * Similar to a finally block.
     */
    FINALLY
}
