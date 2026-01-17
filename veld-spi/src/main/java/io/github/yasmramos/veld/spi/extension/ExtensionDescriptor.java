package io.github.yasmramos.veld.spi.extension;

import java.util.Objects;

/**
 * Descriptor de metadatos para una extensión de Veld.
 * 
 * <p>Esta clase encapsula toda la información de configuración necesaria para que
 * VeldProcessor pueda gestionar correctamente una extensión, incluyendo su identidad,
 * la fase de procesamiento en la que debe ejecutarse y su orden de ejecución.</p>
 * 
 * <p><strong>Identificador único:</strong></p>
 * <p>El identificador debe seguir el formato de nombre cualificado estilo Maven/OSGi
 * (por ejemplo, "com.example/my-extension") para evitar colisiones entre extensiones
 * de diferentes proveedores.</p>
 * 
 * <p><strong>Orden de ejecución:</strong></p>
 * <p>Las extensiones con números de orden menores se ejecutan primero dentro de la
 * misma fase. Se recomienda utilizar valores con intervalos para permitir que terceros
 * inserten extensiones entre las existentes si es necesario.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public final class ExtensionDescriptor {
    
    private final String extensionId;
    private final ExtensionPhase phase;
    private final int order;
    
    /**
     * Crea un nuevo descriptor de extensión.
     * 
     * @param extensionId identificador único de la extensión (formato: grupo/nombre)
     * @param phase la fase en la que debe ejecutarse la extensión
     * @param order el orden de ejecución dentro de la fase
     * @throws NullPointerException si extensionId o phase son nulos
     * @throws IllegalArgumentException si order es negativo
     */
    public ExtensionDescriptor(String extensionId, ExtensionPhase phase, int order) {
        this.extensionId = Objects.requireNonNull(extensionId, "extensionId cannot be null");
        this.phase = Objects.requireNonNull(phase, "phase cannot be null");
        if (order < 0) {
            throw new IllegalArgumentException("order cannot be negative");
        }
        this.order = order;
    }
    
    /**
     * Returns the unique identifier of the extension.
     * 
     * @return the extension identifier
     */
    public String getExtensionId() {
        return extensionId;
    }
    
    /**
     * Returns the processing phase in which this extension should execute.
     * 
     * @return the extension phase
     */
    public ExtensionPhase getPhase() {
        return phase;
    }
    
    /**
     * Returns the execution order of this extension within its phase.
     * 
     * <p>Extensions with lower order values execute first. This allows controlling
     * the relative timing of extensions that need to run in a specific sequence.</p>
     * 
     * @return the execution order
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * Compara este descriptor con otro para determinar si son iguales.
     * 
     * <p>Dos descriptores son iguales si tienen el mismo identificador de extensión.
     * Esto permite utilizar conjuntos de descriptores para evitar duplicados.</p>
     * 
     * @param obj el objeto a comparar
     * @return true si los descriptores son iguales
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ExtensionDescriptor other = (ExtensionDescriptor) obj;
        return Objects.equals(extensionId, other.extensionId);
    }
    
    /**
     * Returns the hash code de este descriptor basado en su identificador.
     * 
     * @return el hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(extensionId);
    }
    
    /**
     * Returns una representación textual del descriptor.
     * 
     * @return string con formato "ExtensionDescriptor{id='id', phase=PHASE, order=N}"
     */
    @Override
    public String toString() {
        return String.format("ExtensionDescriptor{id='%s', phase=%s, order=%d}", 
                extensionId, phase, order);
    }
}
