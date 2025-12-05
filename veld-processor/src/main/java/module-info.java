/**
 * Veld Processor Module.
 * Annotation processor that generates bytecode using ASM.
 */
module com.veld.processor {
    // Required JDK modules
    requires java.compiler;
    
    // Required Veld modules
    requires com.veld.annotation;
    requires com.veld.runtime;
    
    // ASM modules for bytecode generation
    requires org.objectweb.asm;
    
    // Export processor package
    exports com.veld.processor;
    
    // Provide annotation processor service
    provides javax.annotation.processing.Processor 
        with com.veld.processor.VeldProcessor;
}
