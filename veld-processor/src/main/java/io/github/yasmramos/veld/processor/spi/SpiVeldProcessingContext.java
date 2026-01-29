package io.github.yasmramos.veld.processor.spi;

import io.github.yasmramos.veld.spi.extension.VeldProcessingContext;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

/**
 * Implementation of {@link VeldProcessingContext} for the Veld processor.
 *
 * <p>This class provides access to processor tools and additional utilities
 * for extensions, including error management, code generation,
 * and access to Javac utilities.</p>
 *
 * @author Veld Team
 * @version 1.0.0
 */
final class SpiVeldProcessingContext implements VeldProcessingContext {
    
    private final Messager messager;
    private final Elements elementUtils;
    private final Types typeUtils;
    private final RoundEnvironment roundEnv;
    private final javax.annotation.processing.Filer filer;
    private final Set<String> supportedOptions;
    private final boolean debugEnabled;
    
    SpiVeldProcessingContext(Messager messager, Elements elementUtils, Types typeUtils,
                             RoundEnvironment roundEnv, javax.annotation.processing.Filer filer,
                             Set<String> supportedOptions, boolean debugEnabled) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
        this.roundEnv = roundEnv;
        this.filer = filer;
        this.supportedOptions = supportedOptions;
        this.debugEnabled = debugEnabled;
    }
    
    @Override
    public void reportError(String message, Element element) {
        reportError(message, element, Diagnostic.Kind.ERROR);
    }
    
    @Override
    public void reportError(String message, Element element, Diagnostic.Kind kind) {
        messager.printMessage(kind, "[Veld-Extension] " + message, element);
    }
    
    @Override
    public void reportWarning(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.WARNING, "[Veld-Extension] " + message, element);
    }
    
    @Override
    public void reportNote(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.NOTE, "[Veld-Extension] " + message, element);
    }
    
    @Override
    public CodeBuilder createSourceFile(String qualifiedFileName) throws IOException {
        JavaFileObject sourceFile = filer.createSourceFile(qualifiedFileName);
        return new SpiCodeBuilder(sourceFile);
    }
    
    @Override
    public PrintWriter createResourceFile(String resourcePath) throws IOException {
        javax.tools.FileObject resourceFile = filer.createResource(
            javax.tools.StandardLocation.CLASS_OUTPUT, "", resourcePath);
        return new PrintWriter(resourceFile.openWriter());
    }
    
    @Override
    public Elements getElementUtils() {
        return elementUtils;
    }
    
    @Override
    public Types getTypeUtils() {
        return typeUtils;
    }
    
    @Override
    public RoundEnvironment getRoundEnvironment() {
        return roundEnv;
    }
    
    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    @Override
    public Set<String> getSupportedOptions() {
        return supportedOptions;
    }
    
    @Override
    public String getOption(String optionName) {
        return supportedOptions.contains(optionName) ? optionName : null;
    }
    
    /**
     * CodeBuilder implementation for code generation.
     */
    private static final class SpiCodeBuilder implements CodeBuilder {
        private final JavaFileObject sourceFile;
        private final StringWriter stringWriter;
        private final PrintWriter writer;
        private int indentLevel = 0;
        private boolean closed = false;
        
        SpiCodeBuilder(JavaFileObject sourceFile) {
            this.sourceFile = sourceFile;
            this.stringWriter = new StringWriter();
            this.writer = new PrintWriter(stringWriter);
        }
        
        @Override
        public CodeBuilder append(String line) {
            writer.print(line);
            return this;
        }
        
        @Override
        public CodeBuilder appendLine(String line) {
            writer.print(line);
            writer.println();
            return this;
        }
        
        @Override
        public CodeBuilder appendLines(String... lines) {
            for (String line : lines) {
                appendLine(line);
            }
            return this;
        }
        
        @Override
        public CodeBuilder packageDeclaration(String packageName) {
            if (packageName != null && !packageName.isEmpty()) {
                appendLine("package " + packageName + ";");
                blankLine();
            }
            return this;
        }
        
        @Override
        public CodeBuilder importDeclaration(String className) {
            appendLine("import " + className + ";");
            return this;
        }
        
        @Override
        public CodeBuilder beginBlock() {
            appendLine("{");
            indentLevel++;
            return this;
        }
        
        @Override
        public CodeBuilder endBlock() {
            if (indentLevel > 0) {
                indentLevel--;
            }
            appendLine("}");
            return this;
        }
        
        @Override
        public CodeBuilder blankLine() {
            writer.println();
            return this;
        }
        
        @Override
        public String toString() {
            return stringWriter.toString();
        }
        
        @Override
        public void close() {
            if (!closed) {
                try {
                    writer.flush();
                    try (java.io.Writer fileWriter = sourceFile.openWriter()) {
                        fileWriter.write(stringWriter.toString());
                    }
                    closed = true;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write source file", e);
                }
            }
        }
    }
}
