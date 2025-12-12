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

import java.lang.reflect.Method;

/**
 * Provides information about the current join point being intercepted.
 *
 * <p>A join point represents a point in the execution of the program,
 * such as a method call or field access. This interface provides
 * read-only access to join point information.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public interface JoinPoint {

    /**
     * Returns the target object on which the method is being invoked.
     *
     * @return the target object, or null for static methods
     */
    Object getTarget();

    /**
     * Returns the method being invoked.
     *
     * @return the target method
     */
    Method getMethod();

    /**
     * Returns the name of the method being invoked.
     *
     * @return the method name
     */
    String getMethodName();

    /**
     * Returns the arguments passed to the method.
     *
     * @return an array of arguments (may be empty, never null)
     */
    Object[] getArgs();

    /**
     * Returns the class that declares the method.
     *
     * @return the declaring class
     */
    Class<?> getDeclaringClass();

    /**
     * Returns the signature of the method as a string.
     *
     * @return the method signature
     */
    String getSignature();

    /**
     * Returns a short description of the join point.
     *
     * @return short string representation
     */
    String toShortString();

    /**
     * Returns a detailed description of the join point.
     *
     * @return long string representation
     */
    String toLongString();
}
