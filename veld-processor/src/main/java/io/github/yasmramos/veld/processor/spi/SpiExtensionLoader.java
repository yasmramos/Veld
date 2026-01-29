package io.github.yasmramos.veld.processor.spi;

import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;
import io.github.yasmramos.veld.spi.extension.VeldExtension;

import java.util.*;

/**
 * Manejador para cargar y gestionar extensiones de Veld mediante ServiceLoader.
 * 
 * <p>Esta clase es responsable de:</p>
 * <ul>
 *   <li>Descubrir implementaciones de {@link VeldExtension} en el classpath</li>
 *   <li>Ordenar las extensiones por fase y orden de ejecución</li>
 *   <li>Proveer acceso a las extensiones cargadas</li>
 * </ul>
 * 
 * <p>El descubrimiento se realiza mediante el mecanismo estándar de Java ServiceLoader,
 * que busca implementaciones registradas en {@code META-INF/services}.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
final class SpiExtensionLoader {
    
    private final List<VeldExtension> extensions;
    private final Map<ExtensionPhase, List<VeldExtension>> extensionsByPhase;
    private final boolean loaded;
    private final List<String> errors;
    
    private SpiExtensionLoader(List<VeldExtension> extensions, 
                               Map<ExtensionPhase, List<VeldExtension>> extensionsByPhase,
                               boolean loaded, List<String> errors) {
        this.extensions = Collections.unmodifiableList(new ArrayList<>(extensions));
        this.extensionsByPhase = Collections.unmodifiableMap(
            new EnumMap<>(extensionsByPhase));
        this.loaded = loaded;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }
    
    /**
     * Carga las extensiones desde el classpath utilizando ServiceLoader.
     * 
     * @return un nuevo SpiExtensionLoader con las extensiones cargadas
     */
    static SpiExtensionLoader loadExtensions() {
        List<VeldExtension> extensions = new ArrayList<>();
        Map<ExtensionPhase, List<VeldExtension>> extensionsByPhase = new EnumMap<>(ExtensionPhase.class);
        List<String> errors = new ArrayList<>();
        
        // Initialize phase maps
        for (ExtensionPhase phase : ExtensionPhase.values()) {
            extensionsByPhase.put(phase, new ArrayList<>());
        }
        
        try {
            ServiceLoader<VeldExtension> serviceLoader = ServiceLoader.load(VeldExtension.class);
            
            for (VeldExtension extension : serviceLoader) {
                try {
                    ExtensionDescriptor descriptor = extension.getDescriptor();
                    
                    if (descriptor == null) {
                        errors.add("Extension without descriptor: " + extension.getClass().getName());
                        continue;
                    }
                    
                    extensions.add(extension);
                    
                    ExtensionPhase phase = descriptor.getPhase();
                    extensionsByPhase.computeIfAbsent(phase, k -> new ArrayList<>()).add(extension);
                    
                } catch (Exception e) {
                    errors.add("Failed to get descriptor from extension " + 
                              extension.getClass().getName() + ": " + e.getMessage());
                }
            }
            
            // Sort extensions within each phase by order
            for (List<VeldExtension> phaseExtensions : extensionsByPhase.values()) {
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
            errors.add("Failed to load extensions: " + e.getMessage());
        }
        
        return new SpiExtensionLoader(extensions, extensionsByPhase, true, errors);
    }
    
    /**
     * Returns la lista de todas las extensiones cargadas.
     * 
     * @return lista inmutable de extensiones
     */
    List<VeldExtension> getExtensions() {
        return extensions;
    }
    
    /**
     * Returns las extensiones para una fase específica.
     * 
     * @param phase la fase de procesamiento
     * @return lista de extensiones para esa fase, ordenadas por orden de ejecución
     */
    List<VeldExtension> getExtensionsForPhase(ExtensionPhase phase) {
        return extensionsByPhase.getOrDefault(phase, Collections.emptyList());
    }
    
    /**
     * Verifica si se cargaron extensiones.
     * 
     * @return true si se cargaron una o más extensiones
     */
    boolean hasExtensions() {
        return !extensions.isEmpty();
    }
    
    /**
     * Verifica si hubo errores durante la carga.
     * 
     * @return true si hubo errores
     */
    boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Returns los errores ocurridos durante la carga.
     * 
     * @return lista de mensajes de error
     */
    List<String> getErrors() {
        return errors;
    }
    
    /**
     * Returns el número total de extensiones cargadas.
     * 
     * @return cantidad de extensiones
     */
    int getExtensionCount() {
        return extensions.size();
    }
    
    /**
     * Returns una descripción de las extensiones cargadas para depuración.
     * 
     * @return string con información de las extensiones
     */
    String getExtensionsInfo() {
        if (extensions.isEmpty()) {
            return "No extensions loaded";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Loaded ").append(extensions.size()).append(" extension(s):\n");
        
        for (ExtensionPhase phase : ExtensionPhase.values()) {
            List<VeldExtension> phaseExtensions = extensionsByPhase.get(phase);
            if (!phaseExtensions.isEmpty()) {
                sb.append("  ").append(phase.name()).append(":\n");
                for (VeldExtension ext : phaseExtensions) {
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
}
