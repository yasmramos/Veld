package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.LegacyScope;
import java.util.*;

/**
 * Generates VeldRegistry.java source code instead of bytecode.
 */
public final class RegistrySourceGenerator {
    
    private final List<ComponentInfo> components;
    private final List<FactoryInfo> factories;
    private final Map<String, List<Integer>> supertypeIndices = new HashMap<>();
    
    public RegistrySourceGenerator(List<ComponentInfo> components) {
        this(components, new ArrayList<>());
    }
    
    public RegistrySourceGenerator(List<ComponentInfo> components, List<FactoryInfo> factories) {
        this.components = components;
        this.factories = factories;
        buildSupertypeIndices();
    }
    
    private void buildSupertypeIndices() {
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            addSupertypeIndex(comp.getClassName(), i);
            for (String iface : comp.getImplementedInterfaces()) {
                addSupertypeIndex(iface, i);
            }
        }
    }
    
    private void addSupertypeIndex(String type, int index) {
        supertypeIndices.computeIfAbsent(type, k -> new ArrayList<>()).add(index);
    }
    
    public String getClassName() {
        return "io.github.yasmramos.veld.generated.VeldRegistry";
    }
    
    public String generate() {
        StringBuilder sb = new StringBuilder();
        
        // Package declaration
        sb.append("package io.github.yasmramos.veld.generated;\n\n");
        
        // Imports
        sb.append("import io.github.yasmramos.veld.runtime.ComponentFactory;\n");
        sb.append("import io.github.yasmramos.veld.runtime.ComponentRegistry;\n");
        sb.append("import io.github.yasmramos.veld.runtime.LegacyScope;\n");
        sb.append("import java.util.*;\n\n");
        
        // Class declaration
        sb.append("/**\n");
        sb.append(" * Generated component registry for Veld DI container.\n");
        sb.append(" * This class is auto-generated - do not modify.\n");
        sb.append(" */\n");
        sb.append("@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");
        sb.append("public final class VeldRegistry implements ComponentRegistry {\n\n");
        
        // Static fields
        sb.append("    private static final IdentityHashMap<Class<?>, Integer> TYPE_INDICES = new IdentityHashMap<>();\n");
        sb.append("    private static final HashMap<String, Integer> NAME_INDICES = new HashMap<>();\n");
        sb.append("    private static final String[] SCOPES;\n");
        sb.append("    private static final boolean[] LAZY_FLAGS;\n");
        sb.append("    private static final HashMap<Class<?>, int[]> SUPERTYPE_INDICES = new HashMap<>();\n\n");
        
        // Instance fields
        sb.append("    private final ComponentFactory<?>[] factories;\n");
        sb.append("    private final Map<Class<?>, ComponentFactory<?>> factoriesByType = new HashMap<>();\n");
        sb.append("    private final Map<String, ComponentFactory<?>> factoriesByName = new HashMap<>();\n");
        sb.append("    private final Map<Class<?>, List<ComponentFactory<?>>> factoriesBySupertype = new HashMap<>();\n\n");
        
        // Static initializer
        generateStaticInitializer(sb);
        
        // Constructor
        generateConstructor(sb);
        
        // Methods
        generateGetIndexByType(sb);
        generateGetIndexByName(sb);
        generateGetComponentCount(sb);
        generateGetScope(sb);
        generateIsLazy(sb);
        generateCreate(sb);
        generateGetIndicesForType(sb);
        generateInvokePostConstruct(sb);
        generateInvokePreDestroy(sb);
        generateGetAllFactories(sb);
        generateGetFactoryByType(sb);
        generateGetFactoryByName(sb);
        generateGetFactoriesForType(sb);
        
        // Close class
        sb.append("}\n");
        
        return sb.toString();
    }
    
    private void generateStaticInitializer(StringBuilder sb) {
        sb.append("    static {\n");
        
        // TYPE_INDICES
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            sb.append("        TYPE_INDICES.put(").append(comp.getClassName()).append(".class, ").append(i).append(");\n");
            for (String iface : comp.getImplementedInterfaces()) {
                sb.append("        TYPE_INDICES.put(").append(iface).append(".class, ").append(i).append(");\n");
            }
        }
        sb.append("\n");
        
        // NAME_INDICES
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            sb.append("        NAME_INDICES.put(\"").append(comp.getComponentName()).append("\", ").append(i).append(");\n");
        }
        sb.append("\n");
        
        // SCOPES
        sb.append("        SCOPES = new String[] {\n");
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            String scopeId = comp.getScopeId();
            sb.append("            \"").append(scopeId).append("\"");
            if (i < components.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("        };\n\n");
        
        // LAZY_FLAGS
        sb.append("        LAZY_FLAGS = new boolean[] {");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(components.get(i).isLazy());
        }
        sb.append("};\n\n");
        
        // SUPERTYPE_INDICES
        for (Map.Entry<String, List<Integer>> entry : supertypeIndices.entrySet()) {
            String type = entry.getKey();
            List<Integer> indices = entry.getValue();
            sb.append("        SUPERTYPE_INDICES.put(").append(type).append(".class, new int[] {");
            for (int i = 0; i < indices.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(indices.get(i));
            }
            sb.append("});\n");
        }
        
        sb.append("    }\n\n");
    }
    
    private void generateConstructor(StringBuilder sb) {
        sb.append("    public VeldRegistry() {\n");
        sb.append("        factories = new ComponentFactory[").append(components.size()).append("];\n");
        
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            
            // For components, use the class directly as a simple factory
            // This avoids needing to generate factory class files
            sb.append("        final Class<?> _compClass_").append(i).append(" = ").append(comp.getClassName()).append(".class;\n");
            sb.append("        factories[").append(i).append("] = new ComponentFactory<").append(comp.getClassName()).append(">() {\n");
            sb.append("            @SuppressWarnings(\"unchecked\")\n");
            sb.append("            @Override\n");
            sb.append("            public ").append(comp.getClassName()).append(" create() {\n");
            sb.append("                try {\n");
            sb.append("                    return (").append(comp.getClassName()).append(") _compClass_").append(i).append(".getDeclaredConstructor().newInstance();\n");
            sb.append("                } catch (Exception e) {\n");
            sb.append("                    throw new RuntimeException(\"Failed to create component: ").append(comp.getClassName()).append("\", e);\n");
            sb.append("                }\n");
            sb.append("            }\n");
            sb.append("            @Override\n");
            sb.append("            public void invokePostConstruct(").append(comp.getClassName()).append(" instance) {\n");
            if (comp.hasPostConstruct()) {
                sb.append("                instance.").append(comp.getPostConstructMethod()).append("();\n");
            }
            sb.append("            }\n");
            sb.append("            @Override\n");
            sb.append("            public void invokePreDestroy(").append(comp.getClassName()).append(" instance) {\n");
            if (comp.hasPreDestroy()) {
                sb.append("                instance.").append(comp.getPreDestroyMethod()).append("();\n");
            }
            sb.append("            }\n");
            sb.append("            @Override\n");
            sb.append("            public LegacyScope getScope() {\n");
            sb.append("                return LegacyScope.fromId(SCOPES[").append(i).append("]);\n");
            sb.append("            }\n");
            sb.append("            @Override\n");
            sb.append("            public Class<").append(comp.getClassName()).append("> getComponentType() {\n");
            sb.append("                return ").append(comp.getClassName()).append(".class;\n");
            sb.append("            }\n");
            sb.append("            @Override\n");
            sb.append("            public String getComponentName() {\n");
            sb.append("                return \"").append(comp.getComponentName()).append("\";\n");
            sb.append("            }\n");
            sb.append("        };\n");
            
            sb.append("        factoriesByType.put(").append(comp.getClassName()).append(".class, factories[").append(i).append("]);\n");
            sb.append("        factoriesByName.put(\"").append(comp.getComponentName()).append("\", factories[").append(i).append("]);\n");
            
            // Register interfaces
            for (String iface : comp.getImplementedInterfaces()) {
                sb.append("        factoriesByType.put(").append(iface).append(".class, factories[").append(i).append("]);\n");
            }
            
            // Register in supertype map
            sb.append("        factoriesBySupertype.computeIfAbsent(").append(comp.getClassName())
              .append(".class, k -> new ArrayList<>()).add(factories[").append(i).append("]);\n");
            for (String iface : comp.getImplementedInterfaces()) {
                sb.append("        factoriesBySupertype.computeIfAbsent(").append(iface)
                  .append(".class, k -> new ArrayList<>()).add(factories[").append(i).append("]);\n");
            }
        }
        
        sb.append("    }\n\n");
    }
    
    private void generateGetIndexByType(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public int getIndex(Class<?> type) {\n");
        sb.append("        Integer idx = TYPE_INDICES.get(type);\n");
        sb.append("        return idx != null ? idx : -1;\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetIndexByName(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public int getIndex(String name) {\n");
        sb.append("        Integer idx = NAME_INDICES.get(name);\n");
        sb.append("        return idx != null ? idx : -1;\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetComponentCount(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public int getComponentCount() {\n");
        sb.append("        return ").append(components.size()).append(";\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetScope(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public LegacyScope getScope(int index) {\n");
        sb.append("        return LegacyScope.fromId(SCOPES[index]);\n");
        sb.append("    }\n\n");
    }
    
    private void generateIsLazy(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public boolean isLazy(int index) {\n");
        sb.append("        return LAZY_FLAGS[index];\n");
        sb.append("    }\n\n");
    }
    
    private void generateCreate(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public <T> T create(int index) {\n");
        sb.append("        return (T) factories[index].create();\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetIndicesForType(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public int[] getIndicesForType(Class<?> type) {\n");
        sb.append("        int[] indices = SUPERTYPE_INDICES.get(type);\n");
        sb.append("        return indices != null ? indices : new int[0];\n");
        sb.append("    }\n\n");
    }
    
    private void generateInvokePostConstruct(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public void invokePostConstruct(int index, Object instance) {\n");
        sb.append("        ((ComponentFactory<Object>) factories[index]).invokePostConstruct(instance);\n");
        sb.append("    }\n\n");
    }
    
    private void generateInvokePreDestroy(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public void invokePreDestroy(int index, Object instance) {\n");
        sb.append("        ((ComponentFactory<Object>) factories[index]).invokePreDestroy(instance);\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetAllFactories(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public List<ComponentFactory<?>> getAllFactories() {\n");
        sb.append("        return new ArrayList<>(Arrays.asList(factories));\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetFactoryByType(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public <T> ComponentFactory<T> getFactory(Class<T> type) {\n");
        sb.append("        return (ComponentFactory<T>) factoriesByType.get(type);\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetFactoryByName(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public ComponentFactory<?> getFactory(String name) {\n");
        sb.append("        return factoriesByName.get(name);\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetFactoriesForType(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public <T> List<ComponentFactory<? extends T>> getFactoriesForType(Class<T> type) {\n");
        sb.append("        List<ComponentFactory<?>> list = factoriesBySupertype.get(type);\n");
        sb.append("        if (list == null) {\n");
        sb.append("            return Collections.emptyList();\n");
        sb.append("        }\n");
        sb.append("        return (List) new ArrayList<>(list);\n");
        sb.append("    }\n\n");
    }
}
