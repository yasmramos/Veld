package io.github.yasmramos.veld.processor.spi;

import io.github.yasmramos.veld.aop.AopComponentNode;
import io.github.yasmramos.veld.aop.AopGenerator;
import io.github.yasmramos.veld.aop.AopGenerationContext;
import io.github.yasmramos.veld.processor.ComponentInfo;
import io.github.yasmramos.veld.processor.aop.AopClassGenerator;

import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link AopGenerator} para el annotation processor.
 * 
 * <p>Esta clase proporciona la generación de wrappers AOP usando {@link AopClassGenerator}.
 * Se registra via SPI para que el sistema AOP en veld-aop pueda descubrirla y utilizarla.</p>
 * 
 * <p>Esta implementación está registrada en:</p>
 * <ul>
 *   <li>{@code META-INF/services/io.github.yasmramos.veld.aop.AopGenerator}</li>
 * </ul>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public final class ProcessorAopGenerator implements AopGenerator {
    
    private final AopClassGenerator aopClassGenerator;
    
    public ProcessorAopGenerator() {
        this.aopClassGenerator = new AopClassGenerator();
    }
    
    @Override
    public Map<String, String> generateAopWrappers(
            List<? extends AopComponentNode> components,
            AopGenerationContext context) {
        
        if (components == null || components.isEmpty()) {
            return Map.of();
        }
        
        try {
            return aopClassGenerator.generateWrappers(components, context);
        } catch (Exception e) {
            context.reportError("Failed to generate AOP wrappers: " + e.getMessage(), null);
            return Map.of();
        }
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
}
