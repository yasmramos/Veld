/**
 * Veld Processor Module.
 * Annotation processor that generates bytecode using ASM.
 */
module io.github.yasmramos.processor {
    // Required JDK modules
    requires java.compiler;
    
    // Required Veld modules
    requires io.github.yasmramos.annotation;
    requires io.github.yasmramos.runtime;
    
    // ASM modules for bytecode generation
    requires org.objectweb.asm;
    
    // Export processor package
    exports io.github.yasmramos.processor;
    
    // Provide annotation processor service
    provides javax.annotation.processing.Processor 
        with io.github.yasmramos.processor.VeldProcessor;
}
