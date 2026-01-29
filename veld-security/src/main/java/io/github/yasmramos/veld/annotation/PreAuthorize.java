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
 * Pre-authorization with SpEL expression evaluation.
 *
 * <p>The expression is evaluated before method execution. If it returns false,
 * access is denied.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class DocumentService {
 *     
 *     @PreAuthorize("hasRole('ADMIN')")
 *     public void deleteDocument(Long id) {
 *         documentRepository.deleteById(id);
 *     }
 *     
 *     @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
 *     public Document getDocument(Long userId, Long docId) {
 *         return documentRepository.findById(docId);
 *     }
 *     
 *     @PreAuthorize("@securityService.canAccess(#resource)")
 *     public void accessResource(Resource resource) {
 *         // Access granted
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
public @interface PreAuthorize {
    
    /**
     * SpEL expression to evaluate for authorization.
     *
     * @return authorization expression
     */
    String value();
}
