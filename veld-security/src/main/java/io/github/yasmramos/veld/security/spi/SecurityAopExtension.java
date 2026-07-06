package io.github.yasmramos.veld.security.spi;

import io.github.yasmramos.veld.aop.AopComponentNode;
import io.github.yasmramos.veld.aop.AopExtension;
import io.github.yasmramos.veld.aop.AopGenerationContext;
import io.github.yasmramos.veld.annotation.DenyAll;
import io.github.yasmramos.veld.annotation.PermitAll;
import io.github.yasmramos.veld.annotation.PreAuthorize;
import io.github.yasmramos.veld.annotation.RolesAllowed;
import io.github.yasmramos.veld.annotation.Secured;
import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

/**
 * Extensión AOP para Veld Security que integra la generación de código de seguridad
 * en los wrappers AOP generados por Veld AOP.
 * 
 * <p>Esta extensión permite que las anotaciones de seguridad (@Secured, @RolesAllowed,
 * @DenyAll, @PermitAll, @PreAuthorize) sean procesadas en tiempo de compilación junto
 * con el sistema AOP, generando código de verificación de seguridad directamente en
 * los wrappers AOP.</p>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>{@code
 * @Component
 * @Secured
 * public class UserService {
 *     
 *     @RolesAllowed({"ROLE_ADMIN"})
 *     public void deleteUser(Long id) {
 *         // Código protegido - solo ADMIN puede acceder
 *     }
 *     
 *     @PermitAll
 *     public List<User> getAllUsers() {
 *         // Código accesible para todos
 *     }
 * }
 * }</pre>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public class SecurityAopExtension extends AopExtension {
    
    private static final String EXTENSION_ID = "veld/security-aop";
    private static final int ORDER = 50; // Se ejecuta después de la extensión default
    
    // Annotation FQCNs
    private static final String SECURED_ANNOTATION = "io.github.yasmramos.veld.annotation.Secured";
    private static final String ROLES_ALLOWED_ANNOTATION = "io.github.yasmramos.veld.annotation.RolesAllowed";
    private static final String DENY_ALL_ANNOTATION = "io.github.yasmramos.veld.annotation.DenyAll";
    private static final String PERMIT_ALL_ANNOTATION = "io.github.yasmramos.veld.annotation.PermitAll";
    private static final String PRE_AUTHORIZE_ANNOTATION = "io.github.yasmramos.veld.annotation.PreAuthorize";
    
    private Types typeUtils;
    private Elements elementUtils;
    
    @Override
    public ExtensionDescriptor getDescriptor() {
        return new ExtensionDescriptor(
            EXTENSION_ID,
            ExtensionPhase.GENERATION,
            ORDER
        );
    }
    
    @Override
    public void beforeAopGeneration(List<AopComponentNode> components, AopGenerationContext context) {
        this.typeUtils = context.getTypeUtils();
        this.elementUtils = context.getElementUtils();
        
        context.reportNote("Security AOP Extension: Processing " + components.size() + " components for security annotations");
        
        // Validar componentes con anotaciones de seguridad
        for (AopComponentNode component : components) {
            if (hasSecurityAnnotations(component)) {
                context.reportNote("Security found in: " + component.getClassName());
            }
        }
    }
    
    @Override
    public Map<String, String> generateAopWrappers(List<AopComponentNode> components, AopGenerationContext context) {
        Map<String, String> result = new HashMap<>();
        
        for (AopComponentNode component : components) {
            if (hasSecurityAnnotations(component)) {
                // Generar código de seguridad para este componente
                generateSecurityCode(component, context);
            }
        }
        
        return result;
    }
    
    /**
     * Verifica si un componente tiene anotaciones de seguridad.
     */
    private boolean hasSecurityAnnotations(AopComponentNode component) {
        TypeMirror typeMirror = component.getTypeMirror();
        if (typeMirror == null || typeUtils == null) {
            return false;
        }
        
        TypeElement typeElement = (TypeElement) typeUtils.asElement(typeMirror);
        if (typeElement == null) {
            return false;
        }
        
        // Check class-level annotations
        for (AnnotationMirror annotation : typeElement.getAnnotationMirrors()) {
            String annotationName = annotation.getAnnotationType().toString();
            if (isSecurityAnnotation(annotationName)) {
                return true;
            }
        }
        
        // Check method-level annotations
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                for (AnnotationMirror annotation : enclosed.getAnnotationMirrors()) {
                    String annotationName = annotation.getAnnotationType().toString();
                    if (isSecurityAnnotation(annotationName)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private boolean isSecurityAnnotation(String annotationName) {
        return SECURED_ANNOTATION.equals(annotationName) ||
               ROLES_ALLOWED_ANNOTATION.equals(annotationName) ||
               DENY_ALL_ANNOTATION.equals(annotationName) ||
               PERMIT_ALL_ANNOTATION.equals(annotationName) ||
               PRE_AUTHORIZE_ANNOTATION.equals(annotationName);
    }
    
    /**
     * Genera código de seguridad para un componente.
     * 
     * <p>Este método actualmente solo reporta información de seguridad encontrada.
     * La generación real del código se hace mediante la inyección de interceptores
     * en el sistema AOP existente.</p>
     */
    private void generateSecurityCode(AopComponentNode component, AopGenerationContext context) {
        TypeMirror typeMirror = component.getTypeMirror();
        if (typeMirror == null || typeUtils == null) {
            return;
        }
        
        TypeElement typeElement = (TypeElement) typeUtils.asElement(typeMirror);
        if (typeElement == null) {
            return;
        }
        
        // Recopilar información de seguridad para logging/debugging
        Map<String, List<String>> securedMethods = new HashMap<>();
        
        // Check class-level security
        List<String> classLevelRoles = getClassLevelSecurity(typeElement);
        if (!classLevelRoles.isEmpty()) {
            securedMethods.put("<class>", classLevelRoles);
        }
        
        // Check method-level security
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                List<String> methodRoles = getMethodSecurity(method);
                if (!methodRoles.isEmpty()) {
                    securedMethods.put(method.getSimpleName().toString(), methodRoles);
                }
            }
        }
        
        // Reportar información de seguridad
        if (!securedMethods.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Security constraints for ").append(component.getClassName()).append(":\n");
            for (Map.Entry<String, List<String>> entry : securedMethods.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(": ")
                  .append(String.join(", ", entry.getValue())).append("\n");
            }
            context.reportNote(sb.toString());
        }
    }
    
    private List<String> getClassLevelSecurity(TypeElement typeElement) {
        List<String> roles = new ArrayList<>();
        
        for (AnnotationMirror annotation : typeElement.getAnnotationMirrors()) {
            String annotationName = annotation.getAnnotationType().toString();
            
            if (SECURED_ANNOTATION.equals(annotationName)) {
                roles.add("AUTHENTICATED");
                extractRolesFromAnnotation(annotation, roles);
            } else if (ROLES_ALLOWED_ANNOTATION.equals(annotationName)) {
                extractRolesFromAnnotation(annotation, roles);
            } else if (DENY_ALL_ANNOTATION.equals(annotationName)) {
                roles.add("DENIED");
            } else if (PERMIT_ALL_ANNOTATION.equals(annotationName)) {
                roles.add("PERMITTED");
            }
        }
        
        return roles;
    }
    
    private List<String> getMethodSecurity(ExecutableElement method) {
        List<String> roles = new ArrayList<>();
        
        for (AnnotationMirror annotation : method.getAnnotationMirrors()) {
            String annotationName = annotation.getAnnotationType().toString();
            
            if (SECURED_ANNOTATION.equals(annotationName)) {
                roles.add("AUTHENTICATED");
                extractRolesFromAnnotation(annotation, roles);
            } else if (ROLES_ALLOWED_ANNOTATION.equals(annotationName)) {
                extractRolesFromAnnotation(annotation, roles);
            } else if (DENY_ALL_ANNOTATION.equals(annotationName)) {
                roles.add("DENIED");
            } else if (PERMIT_ALL_ANNOTATION.equals(annotationName)) {
                roles.add("PERMITTED");
            } else if (PRE_AUTHORIZE_ANNOTATION.equals(annotationName)) {
                String expression = extractPreAuthorizeExpression(annotation);
                if (expression != null && !expression.isEmpty()) {
                    roles.add("EXPRESSION:" + expression);
                }
            }
        }
        
        return roles;
    }
    
    private void extractRolesFromAnnotation(AnnotationMirror annotation, List<String> roles) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : 
             annotation.getElementValues().entrySet()) {
            if ("value".equals(entry.getKey().getSimpleName().toString())) {
                Object value = entry.getValue().getValue();
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<? extends AnnotationValue> roleValues = (List<? extends AnnotationValue>) value;
                    for (AnnotationValue roleValue : roleValues) {
                        roles.add(roleValue.getValue().toString());
                    }
                }
            }
        }
    }
    
    private String extractPreAuthorizeExpression(AnnotationMirror annotation) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : 
             annotation.getElementValues().entrySet()) {
            if ("value".equals(entry.getKey().getSimpleName().toString())) {
                return entry.getValue().getValue().toString();
            }
        }
        return null;
    }
    
    @Override
    public void afterAopGeneration(Map<String, String> generatedWrappers, AopGenerationContext context) {
        context.reportNote("Security AOP Extension: Completed processing");
    }
}
