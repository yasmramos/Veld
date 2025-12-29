package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.LegacyScope;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads bean metadata from external modules.
 * 
 * This class discovers and loads bean metadata files (META-INF/veld/*-beans.json)
 * from the classpath, allowing the annotation processor to resolve bean dependencies
 * that are defined in external modules.
 */
public final class BeanMetadataReader {

    private final ProcessingEnvironment processingEnv;
    private final Elements elementUtils;
    private final Types typeUtils;
    private final List<ExternalBeanInfo> externalBeans = new ArrayList<>();
    private final Map<String, TypeElement> cachedTypeElements = new HashMap<>();

    public BeanMetadataReader(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }

    /**
     * Loads all bean metadata from the classpath.
     * 
     * @return list of external beans that can be injected by this module
     * @throws IOException if reading fails
     */
    public List<ExternalBeanInfo> loadExternalBeans() throws IOException {
        externalBeans.clear();
        cachedTypeElements.clear();

        // Find all metadata files in META-INF/veld/
        Set<URL> metadataFiles = findMetadataFiles();
        
        if (metadataFiles.isEmpty()) {
            processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, 
                "[Veld] No external bean metadata found");
            return externalBeans;
        }

        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, 
            "[Veld] Found " + metadataFiles.size() + " external module(s) with bean metadata");

        for (URL url : metadataFiles) {
            try (InputStream is = url.openStream();
                 BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
                
                String content = reader.lines().collect(Collectors.joining("\n"));
                List<BeanMetadata> beans = parseMetadata(content, url.toString());
                
                for (BeanMetadata bean : beans) {
                    ExternalBeanInfo info = convertToExternalBean(bean);
                    if (info != null) {
                        externalBeans.add(info);
                    }
                }
            }
        }

        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, 
            "[Veld] Loaded " + externalBeans.size() + " external bean(s) for dependency resolution");

        return externalBeans;
    }

    /**
     * Finds all metadata files in the classpath.
     */
    private Set<URL> findMetadataFiles() throws IOException {
        // Use ClassLoader to find resources
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = getClass().getClassLoader();
        }

        java.util.Enumeration<URL> resources = loader.getResources("META-INF/veld/");
        
        java.util.Set<URL> allFiles = new java.util.HashSet<>();
        while (resources.hasMoreElements()) {
            URL dir = resources.nextElement();
            try (InputStream is = dir.openStream();
                 BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
                
                String fileName;
                while ((fileName = reader.readLine()) != null) {
                    if (fileName.endsWith("-beans.json")) {
                        try {
                            URL fileUrl = new URL(dir, fileName);
                            allFiles.add(fileUrl);
                        } catch (Exception e) {
                            // Skip invalid URLs
                        }
                    }
                }
            }
        }

        return allFiles;
    }

    /**
     * Parses JSON content into BeanMetadata objects.
     * Simple JSON parser for the specific schema used by Veld.
     */
    private List<BeanMetadata> parseMetadata(String json, String source) {
        List<BeanMetadata> beans = new ArrayList<>();
        
        // Simple extraction of bean entries
        int beansStart = json.indexOf("\"beans\"");
        if (beansStart < 0) {
            return beans;
        }

        int arrayStart = json.indexOf("[", beansStart);
        int arrayEnd = json.lastIndexOf("]");
        if (arrayStart < 0 || arrayEnd < 0) {
            return beans;
        }

        String beansArray = json.substring(arrayStart + 1, arrayEnd);
        
        // Split by "}, {" to get individual beans (simplified)
        String[] beanEntries = beansArray.split("\\},\\s*\\{");
        
        for (String entry : beanEntries) {
            entry = entry.trim();
            if (entry.startsWith("{")) entry = entry.substring(1);
            if (entry.endsWith("}")) entry = entry.substring(0, entry.length() - 1);
            
            BeanMetadata bean = parseBeanEntry(entry);
            if (bean != null) {
                beans.add(bean);
            }
        }

        return beans;
    }

    /**
     * Parses a single bean entry from JSON.
     */
    private BeanMetadata parseBeanEntry(String entry) {
        BeanMetadata bean = new BeanMetadata();
        
        // Extract name
        String name = extractStringValue(entry, "name");
        if (name == null) return null;
        bean = new BeanMetadata("external", name, extractStringValue(entry, "type"));
        
        // Extract factory class
        String factoryClass = extractStringValue(entry, "factoryClass");
        if (factoryClass != null && !factoryClass.isEmpty()) {
            // Extract method name from factory class
            int lastDot = factoryClass.lastIndexOf('.');
            String simpleName = lastDot >= 0 ? factoryClass.substring(lastDot + 1) : factoryClass;
            String methodName = "create" + simpleName.replace("Factory", "");
            bean = bean.withFactory(factoryClass, methodName, "()V" + factoryClass.replace('.', '/'));
        }
        
        // Extract scope
        String scopeStr = extractStringValue(entry, "scope");
        if (scopeStr != null) {
            try {
                bean = bean.withScope(LegacyScope.valueOf(scopeStr.toUpperCase()));
            } catch (Exception e) {
                bean = bean.withScope(LegacyScope.SINGLETON);
            }
        }
        
        // Extract qualifier
        String qualifier = extractStringValue(entry, "qualifier");
        if (qualifier != null && !qualifier.isEmpty()) {
            bean = bean.withQualifier(qualifier);
        }
        
        // Extract isPrimary
        if (entry.contains("\"isPrimary\": true")) {
            bean = bean.asPrimary();
        }
        
        // Extract dependencies
        int depsStart = entry.indexOf("\"dependencies\"");
        if (depsStart >= 0) {
            int arrayStart = entry.indexOf("[", depsStart);
            int arrayEnd = entry.indexOf("]", depsStart);
            if (arrayStart >= 0 && arrayEnd >= 0) {
                String depsArray = entry.substring(arrayStart + 1, arrayEnd);
                String[] deps = depsArray.split(",");
                for (String dep : deps) {
                    String cleanDep = dep.trim().replace("\"", "").trim();
                    if (!cleanDep.isEmpty()) {
                        bean.addDependency(cleanDep);
                    }
                }
            }
        }

        return bean;
    }

    /**
     * Extracts a string value from a JSON-like entry.
     */
    private String extractStringValue(String entry, String key) {
        String searchKey = "\"" + key + "\"";
        int keyPos = entry.indexOf(searchKey);
        if (keyPos < 0) return null;
        
        int colonPos = entry.indexOf(":", keyPos);
        if (colonPos < 0) return null;
        
        int valueStart = entry.indexOf("\"", colonPos);
        if (valueStart < 0) return null;
        
        int valueEnd = entry.indexOf("\"", valueStart + 1);
        if (valueEnd < 0) return null;
        
        return entry.substring(valueStart + 1, valueEnd);
    }

    /**
     * Converts BeanMetadata to ExternalBeanInfo for internal use.
     */
    private ExternalBeanInfo convertToExternalBean(BeanMetadata metadata) {
        TypeElement typeElement = findTypeElement(metadata.getBeanType());
        if (typeElement == null) {
            processingEnv.getMessager().printMessage(
                javax.tools.Diagnostic.Kind.WARNING,
                "[Veld] Cannot resolve external bean type: " + metadata.getBeanType());
            return null;
        }

        return new ExternalBeanInfo(
            metadata.getBeanName(),
            typeElement.asType(),
            metadata.getFactoryClassName(),
            metadata.getScope() != null ? metadata.getScope() : LegacyScope.SINGLETON,
            metadata.getQualifier(),
            metadata.isPrimary(),
            metadata.getDependencies()
        );
    }

    /**
     * Finds or caches a TypeElement for the given fully qualified name.
     */
    private TypeElement findTypeElement(String typeName) {
        return cachedTypeElements.computeIfAbsent(typeName, name -> {
            try {
                return elementUtils.getTypeElement(name);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Gets all loaded external beans.
     */
    public List<ExternalBeanInfo> getExternalBeans() {
        return Collections.unmodifiableList(externalBeans);
    }

    /**
     * Finds an external bean by type.
     */
    public ExternalBeanInfo findByType(TypeMirror type) {
        for (ExternalBeanInfo bean : externalBeans) {
            if (typeUtils.isSameType(type, bean.getBeanType())) {
                return bean;
            }
        }
        return null;
    }

    /**
     * Finds external beans by type (for cases with multiple implementations).
     */
    public List<ExternalBeanInfo> findAllByType(TypeMirror type) {
        List<ExternalBeanInfo> result = new ArrayList<>();
        for (ExternalBeanInfo bean : externalBeans) {
            if (typeUtils.isSameType(type, bean.getBeanType())) {
                result.add(bean);
            }
        }
        return result;
    }

    /**
     * Checks if any external bean matches the given type and optional qualifier.
     */
    public boolean hasMatchingBean(TypeMirror type, String qualifier) {
        for (ExternalBeanInfo bean : externalBeans) {
            if (typeUtils.isSameType(type, bean.getBeanType())) {
                if (qualifier == null) {
                    return true;
                }
                if (qualifier.equals(bean.getQualifier())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Represents information about an external bean that can be injected.
     */
    public static class ExternalBeanInfo {
        private final String beanName;
        private final TypeMirror beanType;
        private final String factoryClassName;
        private final LegacyScope scope;
        private final String qualifier;
        private final boolean isPrimary;
        private final List<String> dependencies;

        public ExternalBeanInfo(String beanName, TypeMirror beanType, String factoryClassName,
                               LegacyScope scope, String qualifier, boolean isPrimary,
                               List<String> dependencies) {
            this.beanName = beanName;
            this.beanType = beanType;
            this.factoryClassName = factoryClassName;
            this.scope = scope;
            this.qualifier = qualifier;
            this.isPrimary = isPrimary;
            this.dependencies = dependencies != null ? dependencies : Collections.emptyList();
        }

        public String getBeanName() {
            return beanName;
        }

        public TypeMirror getBeanType() {
            return beanType;
        }

        public String getFactoryClassName() {
            return factoryClassName;
        }

        public LegacyScope getScope() {
            return scope;
        }

        public String getQualifier() {
            return qualifier;
        }

        public boolean isPrimary() {
            return isPrimary;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

        public boolean hasQualifier() {
            return qualifier != null && !qualifier.isEmpty();
        }

        @Override
        public String toString() {
            return "ExternalBeanInfo{" +
                    "beanName='" + beanName + '\'' +
                    ", beanType=" + beanType +
                    ", factoryClassName='" + factoryClassName + '\'' +
                    ", scope=" + scope +
                    '}';
        }
    }
}
