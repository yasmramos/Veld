package io.github.yasmramos.veld.core.lifecycle;

import io.github.yasmramos.veld.core.bean.BeanDefinition;

import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BeanDependencyAnalyzer {

    private BeanDependencyAnalyzer() {
    }

    public static Set<Class<?>> analyze(BeanDefinition definition) {
        if (definition == null) {
            return Collections.emptySet();
        }
        Class<?> beanClass = definition.getBeanClass();
        if (beanClass == null) {
            return Collections.emptySet();
        }

        Set<Class<?>> dependencies = new LinkedHashSet<>();
        collectConstructorDependencies(beanClass, dependencies);
        collectFieldDependencies(beanClass, dependencies);
        collectMethodDependencies(beanClass, dependencies);
        return dependencies;
    }

    private static void collectConstructorDependencies(Class<?> beanClass, Set<Class<?>> dependencies) {
        for (Constructor<?> constructor : beanClass.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                for (Class<?> parameterType : constructor.getParameterTypes()) {
                    dependencies.add(parameterType);
                }
            }
        }
    }

    private static void collectFieldDependencies(Class<?> beanClass, Set<Class<?>> dependencies) {
        Class<?> current = beanClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    dependencies.add(field.getType());
                }
            }
            current = current.getSuperclass();
        }
    }

    private static void collectMethodDependencies(Class<?> beanClass, Set<Class<?>> dependencies) {
        Class<?> current = beanClass;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Inject.class)) {
                    for (Class<?> parameterType : method.getParameterTypes()) {
                        dependencies.add(parameterType);
                    }
                }
            }
            current = current.getSuperclass();
        }
    }
}
