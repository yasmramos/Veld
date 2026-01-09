package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.annotation.ScopeType;
import java.util.List;

/**
 * Generates BeanFactory source code for @Bean methods in @Factory classes.
 *
 * <p>Each @Bean method gets its own factory class that invokes the factory method
 * to produce the bean instance. The generated factory implements ComponentFactory
 * and handles all aspects of bean creation including lifecycle callbacks.</p>
 */
public final class BeanFactorySourceGenerator {

    private final FactoryInfo factory;
    private final FactoryInfo.BeanMethod beanMethod;
    private final int beanIndex;

    public BeanFactorySourceGenerator(FactoryInfo factory, FactoryInfo.BeanMethod beanMethod, int beanIndex) {
        this.factory = factory;
        this.beanMethod = beanMethod;
        this.beanIndex = beanIndex;
    }

    public String getFactoryClassName() {
        // Generate unique factory class name: ReturnType$$VeldBeanFactory$index
        String returnTypeSimple = getSimpleName(beanMethod.getReturnType());
        return beanMethod.getReturnType() + "$$VeldBeanFactory$" + beanIndex;
    }

    public String generate() {
        StringBuilder sb = new StringBuilder();

        String packageName = getPackageName(beanMethod.getReturnType());
        String returnTypeSimple = getSimpleName(beanMethod.getReturnType());
        String factorySimpleName = returnTypeSimple + "$$VeldBeanFactory$" + beanIndex;
        String factoryClassName = beanMethod.getReturnType() + "$$VeldBeanFactory$" + beanIndex;
        String factoryInternalName = beanMethod.getReturnType().replace('.', '/') + "$$VeldBeanFactory$" + beanIndex;

        // Package declaration
        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        // Imports
        sb.append("import io.github.yasmramos.veld.Veld;\n");
        sb.append("import io.github.yasmramos.veld.runtime.ComponentFactory;\n");
        sb.append("import io.github.yasmramos.veld.annotation.ScopeType;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Arrays;\n\n");

        // Class declaration
        sb.append("/**\n");
        sb.append(" * Generated factory for @Bean method: ").append(beanMethod.getMethodName()).append("\n");
        sb.append(" * Factory class: ").append(factory.getFactoryClassName()).append("\n");
        sb.append(" * Bean type: ").append(beanMethod.getReturnType()).append("\n");
        sb.append(" */\n");
        sb.append("@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");
        sb.append("public final class ").append(factorySimpleName);
        sb.append(" implements ComponentFactory<").append(beanMethod.getReturnType()).append("> {\n\n");

        // Factory instance reference (singleton pattern for the factory class itself)
        sb.append("    // Factory class singleton for invoking @Bean method\n");
        sb.append("    private static volatile ").append(factory.getFactoryClassName()).append(" factoryInstance;\n\n");

        // Constructor
        sb.append("    public ").append(factorySimpleName).append("() {}\n\n");

        // getFactoryClass() - returns the factory class that contains this @Bean method
        sb.append("    @Override\n");
        sb.append("    public Class<?> getFactoryClass() {\n");
        sb.append("        return ").append(factory.getFactoryClassName()).append(".class;\n");
        sb.append("    }\n\n");

        // getBeanMethodName() - returns the name of the @Bean method
        sb.append("    @Override\n");
        sb.append("    public String getBeanMethodName() {\n");
        sb.append("        return \"").append(beanMethod.getMethodName()).append("\";\n");
        sb.append("    }\n\n");

        // create() method - invokes the @Bean method
        generateCreateMethod(sb, factorySimpleName, factoryClassName);

        // getComponentType()
        sb.append("    @Override\n");
        sb.append("    public Class<").append(beanMethod.getReturnType()).append("> getComponentType() {\n");
        sb.append("        return ").append(beanMethod.getReturnType()).append(".class;\n");
        sb.append("    }\n\n");

        // getComponentName()
        sb.append("    @Override\n");
        sb.append("    public String getComponentName() {\n");
        sb.append("        return \"").append(beanMethod.getBeanName()).append("\";\n");
        sb.append("    }\n\n");

        // getScope() - uses the scope from @Bean annotation
        sb.append("    @Override\n");
        sb.append("    public ScopeType getScope() {\n");
        String scopeName = beanMethod.getScope() == ScopeType.PROTOTYPE ? "PROTOTYPE" : "SINGLETON";
        sb.append("        return ScopeType.").append(scopeName).append(";\n");
        sb.append("    }\n\n");

        // getQualifier() - returns the qualifier name if present
        if (beanMethod.hasQualifier()) {
            sb.append("    @Override\n");
            sb.append("    public String getQualifier() {\n");
            sb.append("        return \"").append(beanMethod.getQualifier()).append("\";\n");
            sb.append("    }\n\n");
        }

        // isLazy()
        sb.append("    @Override\n");
        sb.append("    public boolean isLazy() {\n");
        sb.append("        return false;\n");
        sb.append("    }\n\n");

        // isPrimary()
        if (beanMethod.isPrimary()) {
            sb.append("    @Override\n");
            sb.append("    public boolean isPrimary() {\n");
            sb.append("        return true;\n");
            sb.append("    }\n\n");
        }

        // getFactoryMethodParameters() - returns parameter types for dependency resolution
        sb.append("    @Override\n");
        sb.append("    public List<Class<?>> getFactoryMethodParameters() {\n");
        if (beanMethod.getParameterTypes().isEmpty()) {
            sb.append("        return List.of();\n");
        } else {
            sb.append("        return Arrays.asList(\n");
            for (int i = 0; i < beanMethod.getParameterTypes().size(); i++) {
                sb.append("            ").append(beanMethod.getParameterTypes().get(i)).append(".class");
                if (i < beanMethod.getParameterTypes().size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("        );\n");
        }
        sb.append("    }\n\n");

        // invokePostConstruct() - lifecycle callback after bean creation
        sb.append("    @Override\n");
        sb.append("    public void invokePostConstruct(").append(beanMethod.getReturnType()).append(" instance) {\n");
        if (beanMethod.hasPostConstruct()) {
            sb.append("        instance.").append(beanMethod.getPostConstructMethodName()).append("();\n");
        }
        sb.append("    }\n\n");

        // invokePreDestroy() - lifecycle callback before bean destruction
        sb.append("    @Override\n");
        sb.append("    public void invokePreDestroy(").append(beanMethod.getReturnType()).append(" instance) {\n");
        if (beanMethod.hasPreDestroy()) {
            sb.append("        instance.").append(beanMethod.getPreDestroyMethodName()).append("();\n");
        }
        sb.append("    }\n\n");

        // Close class
        sb.append("}\n");

        return sb.toString();
    }

    private void generateCreateMethod(StringBuilder sb, String factorySimpleName, String factoryClassName) {
        sb.append("    @Override\n");
        sb.append("    public ").append(beanMethod.getReturnType()).append(" create(Veld veld) {\n");

        // Get or create factory instance (singleton pattern)
        sb.append("        // Get or create the factory class instance\n");
        sb.append("        if (factoryInstance == null) {\n");
        sb.append("            synchronized (").append(factorySimpleName).append(".class) {\n");
        sb.append("                if (factoryInstance == null) {\n");
        sb.append("                    factoryInstance = new ").append(factoryClassName).append("();\n");
        sb.append("                    // Inject dependencies into the factory instance\n");
        sb.append("                    veld.inject(factoryInstance);\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        }\n\n");

        // Invoke the @Bean method
        sb.append("        // Invoke the @Bean method to produce the bean\n");
        sb.append("        return factoryInstance.").append(beanMethod.getMethodName()).append("(");

        // Pass resolved dependencies as parameters
        List<String> paramTypes = beanMethod.getParameterTypes();
        if (paramTypes.isEmpty()) {
            sb.append(");\n");
        } else {
            sb.append("\n");
            for (int i = 0; i < paramTypes.size(); i++) {
                String paramType = paramTypes.get(i);
                String paramName = "param" + i;
                sb.append("            veld.getBean(").append(paramType).append(".class)");
                if (i < paramTypes.size() - 1) {
                    sb.append(",\n");
                }
            }
            sb.append("\n        );\n");
        }

        sb.append("    }\n\n");
    }

    private String getPackageName(String fullClassName) {
        // For inner classes (Outer$Inner), find the package by looking for $ first
        int dollarSign = fullClassName.lastIndexOf('$');
        if (dollarSign > 0) {
            // It's an inner class, find the last dot before the $
            int lastDot = fullClassName.lastIndexOf('.', dollarSign - 1);
            return lastDot > 0 ? fullClassName.substring(0, lastDot) : null;
        }
        // Regular class - find the last dot
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot > 0 ? fullClassName.substring(0, lastDot) : null;
    }

    private String getSimpleName(String fullClassName) {
        // For inner classes (Outer$Inner), get just the inner class name
        int lastDollar = fullClassName.lastIndexOf('$');
        if (lastDollar >= 0) {
            return fullClassName.substring(lastDollar + 1);
        }
        // For regular classes, get the class name after the last dot
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot > 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
}
