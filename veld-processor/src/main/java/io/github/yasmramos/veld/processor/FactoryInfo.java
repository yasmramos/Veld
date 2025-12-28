package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.Scope;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds metadata about a factory class and its @Bean methods.
 * Factory classes are used to programmatically create beans for the Veld container.
 */
public final class FactoryInfo {

    private final String factoryClassName;
    private final String factoryInternalName;
    private final String factoryName;
    private final List<BeanMethod> beanMethods = new ArrayList<>();

    // TypeElement for annotation processing (transient - not serialized)
    private transient TypeElement typeElement;

    public FactoryInfo(String factoryClassName, String factoryName) {
        this.factoryClassName = factoryClassName;
        this.factoryInternalName = factoryClassName.replace('.', '/');
        this.factoryName = factoryName;
    }

    public String getFactoryClassName() {
        return factoryClassName;
    }

    public String getFactoryInternalName() {
        return factoryInternalName;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public List<BeanMethod> getBeanMethods() {
        return beanMethods;
    }

    public void addBeanMethod(BeanMethod beanMethod) {
        this.beanMethods.add(beanMethod);
    }

    public boolean hasBeanMethods() {
        return !beanMethods.isEmpty();
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public void setTypeElement(TypeElement typeElement) {
        this.typeElement = typeElement;
    }

    /**
     * Represents a method annotated with @Bean inside a @Factory class.
     */
    public static class BeanMethod {

        private final String methodName;
        private final String methodDescriptor;
        private final String returnType;
        private final String returnTypeDescriptor;
        private final String beanName;
        private final boolean isPrimary;
        private final List<String> parameterTypes = new ArrayList<>();
        private String postConstructMethodName;
        private String postConstructDescriptor;
        private String preDestroyMethodName;
        private String preDestroyDescriptor;
        private Scope scope = Scope.SINGLETON;
        private String qualifier;  // For @Qualifier support

        public BeanMethod(String methodName, String methodDescriptor, String returnType,
                         String returnTypeDescriptor, String beanName, boolean isPrimary) {
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.returnType = returnType;
            this.returnTypeDescriptor = returnTypeDescriptor;
            this.beanName = beanName;
            this.isPrimary = isPrimary;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getMethodDescriptor() {
            return methodDescriptor;
        }

        public String getReturnType() {
            return returnType;
        }

        public String getReturnTypeDescriptor() {
            return returnTypeDescriptor;
        }

        public String getBeanName() {
            return beanName;
        }

        public boolean isPrimary() {
            return isPrimary;
        }

        public List<String> getParameterTypes() {
            return parameterTypes;
        }

        public void addParameterType(String parameterType) {
            this.parameterTypes.add(parameterType);
        }

        public boolean hasPostConstruct() {
            return postConstructMethodName != null && !postConstructMethodName.isEmpty();
        }

        public String getPostConstructMethodName() {
            return postConstructMethodName;
        }

        public void setPostConstruct(String methodName, String descriptor) {
            this.postConstructMethodName = methodName;
            this.postConstructDescriptor = descriptor;
        }

        public boolean hasPreDestroy() {
            return preDestroyMethodName != null && !preDestroyMethodName.isEmpty();
        }

        public String getPreDestroyMethodName() {
            return preDestroyMethodName;
        }

        public void setPreDestroy(String methodName, String descriptor) {
            this.preDestroyMethodName = methodName;
            this.preDestroyDescriptor = descriptor;
        }

        public Scope getScope() {
            return scope;
        }

        public void setScope(Scope scope) {
            this.scope = scope;
        }

        public boolean hasQualifier() {
            return qualifier != null && !qualifier.isEmpty();
        }

        public String getQualifier() {
            return qualifier;
        }

        public void setQualifier(String qualifier) {
            this.qualifier = qualifier;
        }

        public String getBeanClassName() {
            return returnType;
        }

        public String getBeanInternalName() {
            return returnType.replace('.', '/');
        }

        public String getFactoryInternalName() {
            return returnType.replace('.', '$') + "$$Bean";
        }

        public String getFactoryClassName() {
            // Construct factory class name from return type
            // Example: com.example.MyService -> com.example.MyService$$Bean
            int lastDot = returnType.lastIndexOf('.');
            if (lastDot >= 0) {
                return returnType.substring(0, lastDot + 1) + returnType.substring(lastDot + 1).replace('.', '$') + "$$Bean";
            }
            return returnType + "$$Bean";
        }

        @Override
        public String toString() {
            return "BeanMethod{" +
                    "methodName='" + methodName + '\'' +
                    ", beanName='" + beanName + '\'' +
                    ", returnType='" + returnType + '\'' +
                    ", isPrimary=" + isPrimary +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "FactoryInfo{" +
                "factoryClassName='" + factoryClassName + '\'' +
                ", factoryName='" + factoryName + '\'' +
                ", beanMethods=" + beanMethods.size() +
                '}';
    }
}
