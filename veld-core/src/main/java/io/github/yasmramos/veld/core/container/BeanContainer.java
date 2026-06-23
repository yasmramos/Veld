package io.github.yasmramos.veld.core.container;

import io.github.yasmramos.veld.core.bean.BeanDefinition;
import io.github.yasmramos.veld.core.bean.BeanFactory;
import io.github.yasmramos.veld.core.bean.Scope;
import io.github.yasmramos.veld.core.proxy.LazyProxyFactory;
import io.github.yasmramos.veld.core.resolver.DependencyResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanContainer {

    private final Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    private final DependencyResolver resolver;
    private final BeanFactory beanFactory;
    private final LazyProxyFactory lazyProxyFactory;
    private volatile boolean started = false;

    public BeanContainer() {
        this(new DependencyResolver(), new BeanFactory(), new LazyProxyFactory());
    }

    public BeanContainer(DependencyResolver resolver, BeanFactory beanFactory, LazyProxyFactory lazyProxyFactory) {
        this.resolver = resolver;
        this.beanFactory = beanFactory;
        this.lazyProxyFactory = lazyProxyFactory;
    }

    public void register(BeanDefinition definition) {
        if (started) {
            throw new IllegalStateException("Cannot register beans after container has started: " + definition.getName());
        }
        if (definitions.containsKey(definition.getName())) {
            throw new IllegalStateException("Duplicate bean definition: " + definition.getName());
        }
        definitions.put(definition.getName(), definition);
    }

    public void start() {
        if (started) {
            return;
        }

        List<BeanDefinition> ordered = resolver.resolve(definitions);

        for (BeanDefinition definition : ordered) {
            if (definition.isLazy()) {
                Object proxy = lazyProxyFactory.createProxy(definition, this);
                if (definition.getScope() == Scope.SINGLETON) {
                    singletons.put(definition.getName(), proxy);
                }
            } else if (definition.getScope() == Scope.SINGLETON) {
                Object instance = beanFactory.create(definition, this);
                singletons.put(definition.getName(), instance);
            }
        }

        started = true;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition definition = definitions.get(name);
        if (definition == null) {
            throw new IllegalArgumentException("No bean registered with name: " + name);
        }
        if (definition.getScope() == Scope.SINGLETON) {
            Object existing = singletons.get(name);
            if (existing != null) {
                return (T) existing;
            }
            synchronized (singletons) {
                existing = singletons.get(name);
                if (existing != null) {
                    return (T) existing;
                }
                Object instance = beanFactory.create(definition, this);
                singletons.put(name, instance);
                return (T) instance;
            }
        }
        return (T) beanFactory.create(definition, this);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        for (BeanDefinition definition : definitions.values()) {
            if (type.isAssignableFrom(definition.getType())) {
                return (T) getBean(definition.getName());
            }
        }
        throw new IllegalArgumentException("No bean registered for type: " + type.getName());
    }

    public BeanDefinition getDefinition(String name) {
        return definitions.get(name);
    }

    public List<BeanDefinition> getDefinitions() {
        return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
    }

    public boolean isStarted() {
        return started;
    }
}
