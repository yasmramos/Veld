package io.github.yasmramos.veld.aop;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

/**
 * Contexto de generación AOP proporcionado a las extensiones.
 * 
 * <p>Este contexto proporciona acceso a las herramientas del annotation processor
 * necesarias para generar código, reportar mensajes, y manipular tipos.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public final class AopGenerationContext {
    
    private final Messager messager;
    private final Elements elementUtils;
    private final Types typeUtils;
    private final Filer filer;
    private final List<String> warnings;
    private final List<String> errors;
    
    private AopGenerationContext(Messager messager, Elements elementUtils, 
                                  Types typeUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
        this.filer = filer;
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
    }
    
    /**
     * Crea un nuevo contexto de generación AOP.
     */
    public static AopGenerationContext create(Messager messager, Elements elementUtils,
                                               Types typeUtils, Filer filer) {
        return new AopGenerationContext(messager, elementUtils, typeUtils, filer);
    }
    
    /**
     * Returns el Messager para reportar mensajes.
     */
    public Messager getMessager() {
        return messager;
    }
    
    /**
     * Returns el Elements utility.
     */
    public Elements getElementUtils() {
        return elementUtils;
    }
    
    /**
     * Returns el Types utility.
     */
    public Types getTypeUtils() {
        return typeUtils;
    }
    
    /**
     * Returns el Filer para escribir archivos fuente.
     */
    public Filer getFiler() {
        return filer;
    }
    
    /**
     * Reporta una advertencia.
     * 
     * @param message el mensaje de advertencia
     * @param element el elemento opcional relacionado
     */
    public void reportWarning(String message, javax.lang.model.element.Element element) {
        warnings.add(message);
        messager.printMessage(Diagnostic.Kind.WARNING, "[AOP Extension] " + message, element);
    }
    
    /**
     * Reporta un error.
     * 
     * @param message el mensaje de error
     * @param element el elemento opcional relacionado
     */
    public void reportError(String message, javax.lang.model.element.Element element) {
        errors.add(message);
        messager.printMessage(Diagnostic.Kind.ERROR, "[AOP Extension] " + message, element);
    }
    
    /**
     * Reporta una nota informativa.
     * 
     * @param message el mensaje
     */
    public void reportNote(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, "[AOP Extension] " + message);
    }
    
    /**
     * Returns la lista de advertencias reportadas.
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    /**
     * Returns la lista de errores reportados.
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Indica si hubo errores durante la generación.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
