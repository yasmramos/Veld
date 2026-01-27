package io.github.yasmramos.veld.aop.spi;

import io.github.yasmramos.veld.aop.AopExtension;
import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;

import java.util.*;

/**
 * Manejador para cargar y gestionar extensiones AOP de Veld mediante ServiceLoader.
 * 
 * <p>Esta clase es responsable de:</p>
 * <ul>
 *   <li>Descubrir implementaciones de {@link AopExtension} en el classpath</li>
 *   <li>Ordenar las extensiones por fase y orden de ejecución</li>
 *   <li>Proveer acceso a las extensiones cargadas</li>
 * </ul>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
final class SpiAopExtensionLoader {
    
    private final List<AopExtension> extensions;
    private final Map<ExtensionPhase, List<AopExtension>> extensionsByPhase;
    private final boolean loaded;
    private final List<String> errors;
    
    private SpiAopExtensionLoader(List<AopExtension> extensions, 
                                   Map<ExtensionPhase, List<AopExtension>> extensionsByPhase,
                                   boolean loaded, List<String> errors) {
        this.extensions = Collections.unmodifiableList(new ArrayList<>(extensions));
        this.extensionsByPhase = Collections.unmodifiableMap(
            new EnumMap<>(extensionsByPhase));
        this.loaded = loaded;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }
    
    /**
     * Carga las extensiones AOP desde el classpath utilizando ServiceLoader.
     * 
     * @return un nuevo SpiAopExtensionLoader con las extensiones cargadas
     */
    static SpiAopExtensionLoader loadExtensions() {
        List<AopExtension> extensions = new ArrayList<>();
        Map<ExtensionPhase, List<AopExtension>> extensionsByPhase = new EnumMap<>(ExtensionPhase.class);
        List<String> errors = new ArrayList<>();
        
        // Initialize phase maps
        for (ExtensionPhase phase : ExtensionPhase.values()) {
            extensionsByPhase.put(phase, new ArrayList<>());
        }
        
        try {
            ServiceLoader<AopExtension> serviceLoader = ServiceLoader.load(AopExtension.class);
            
            for (AopExtension extension : serviceLoader) {
                try {
                    ExtensionDescriptor descriptor = extension.getDescriptor();
                    
                    if (descriptor == null) {
                        errors.add("AOP Extension without descriptor: " + extension.getClass().getName());
                        continue;
                    }
                    
                    extensions.add(extension);
                    
                    ExtensionPhase phase = descriptor.getPhase();
                    extensionsByPhase.computeIfAbsent(phase, k -> new ArrayList<>()).add(extension);
                    
                } catch (Exception e) {
                    errors.add("Failed to get descriptor from AOP extension " + 
                              extension.getClass().getName() + ": " + e.getMessage());
                }
            }
            
            // Sort extensions within each phase by order
            for (List<AopExtension> phaseExtensions : extensionsByPhase.values()) {
                phaseExtensions.sort(Comparator.comparingInt(ext -> {
                    try {
                        return ext.getDescriptor().getOrder();
                    } catch (Exception e) {
                        return Integer.MAX_VALUE;
                    }
                }));
            }
            
        } catch (ServiceConfigurationError e) {
            errors.add("ServiceLoader configuration error: " + e.getMessage());
        } catch (Exception e) {
            errors.add("Failed to load AOP extensions: " + e.getMessage());
        }
        
        return new SpiAopExtensionLoader(extensions, extensionsByPhase, true, errors);
    }
    
    /**
     * Returns la lista de todas las extensiones AOP cargadas.
     */
    List<AopExtension> getExtensions() {
        return extensions;
    }
    
    /**
     * Returns las extensiones para una fase específica.
     */
    List<AopExtension> getExtensionsForPhase(ExtensionPhase phase) {
        return extensionsByPhase.getOrDefault(phase, Collections.emptyList());
    }
    
    /**
     * Verifica si se cargaron extensiones.
     */
    boolean hasExtensions() {
        return !extensions.isEmpty();
    }
    
    /**
     * Verifica si hubo errores durante la carga.
     */
    boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Returns los errores ocurridos durante la carga.
     */
    List<String> getErrors() {
        return errors;
    }
    
    /**
     * Returns el número total de extensiones cargadas.
     */
    int getExtensionCount() {
        return extensions.size();
    }
    
    /**
     * Returns una descripción de las extensiones cargadas para depuración.
     */
    String getExtensionsInfo() {
        if (extensions.isEmpty()) {
            return "No AOP extensions loaded";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Loaded ").append(extensions.size()).append(" AOP extension(s):\n");
        
        for (ExtensionPhase phase : ExtensionPhase.values()) {
            List<AopExtension> phaseExtensions = extensionsByPhase.get(phase);
            if (!phaseExtensions.isEmpty()) {
                sb.append("  ").append(phase.name()).append(":\n");
                for (AopExtension ext : phaseExtensions) {
                    try {
                        ExtensionDescriptor desc = ext.getDescriptor();
                        sb.append("    - ").append(desc.getExtensionId())
                          .append(" (order=").append(desc.getOrder()).append(")\n");
                    } catch (Exception e) {
                        sb.append("    - ").append(ext.getClass().getName())
                          .append(" (descriptor error)\n");
                    }
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Encuentra la primera extensión que quiere sobrescribir la generación default.
     * Si hay múltiples, usa la de menor orden.
     */
    Optional<AopExtension> findOverridingExtension() {
        return extensions.stream()
            .filter(AopExtension::overridesDefaultGeneration)
            .min(Comparator.comparingInt(ext -> {
                try {
                    return ext.getDescriptor().getOrder();
                } catch (Exception e) {
                    return Integer.MAX_VALUE;
                }
            }));
    }
}
