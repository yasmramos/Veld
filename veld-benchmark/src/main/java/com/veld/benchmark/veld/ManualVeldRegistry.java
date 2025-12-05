/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.veld.benchmark.veld;

import com.veld.runtime.ComponentFactory;
import com.veld.runtime.ComponentRegistry;
import com.veld.runtime.Scope;
import com.veld.runtime.VeldContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ultra-optimized manual ComponentRegistry implementation for benchmarks.
 * Uses IdentityHashMap and indexed lookups for maximum performance.
 */
public class ManualVeldRegistry implements ComponentRegistry {
    
    private final List<ComponentFactory<?>> factories = new ArrayList<>();
    private final IdentityHashMap<Class<?>, Integer> typeIndices = new IdentityHashMap<>();
    private final Map<String, Integer> nameIndices = new HashMap<>();
    private final IdentityHashMap<Class<?>, ComponentFactory<?>> byType = new IdentityHashMap<>();
    private final Map<String, ComponentFactory<?>> byName = new HashMap<>();
    
    // Pre-computed arrays for fast access
    private Scope[] scopes = new Scope[0];
    private boolean[] lazyFlags = new boolean[0];
    
    /**
     * Registers a singleton component.
     */
    public <T> ManualVeldRegistry singleton(Class<T> type, Function<VeldContainer, T> creator) {
        return singleton(type, type.getSimpleName(), creator);
    }
    
    /**
     * Registers a singleton component with a name.
     */
    public <T> ManualVeldRegistry singleton(Class<T> type, String name, Function<VeldContainer, T> creator) {
        int index = factories.size();
        ComponentFactory<T> factory = new IndexedFactory<>(type, name, Scope.SINGLETON, creator, index);
        register(factory, type, index);
        return this;
    }
    
    /**
     * Registers a prototype component.
     */
    public <T> ManualVeldRegistry prototype(Class<T> type, Function<VeldContainer, T> creator) {
        return prototype(type, type.getSimpleName(), creator);
    }
    
    /**
     * Registers a prototype component with a name.
     */
    public <T> ManualVeldRegistry prototype(Class<T> type, String name, Function<VeldContainer, T> creator) {
        int index = factories.size();
        ComponentFactory<T> factory = new IndexedFactory<>(type, name, Scope.PROTOTYPE, creator, index);
        register(factory, type, index);
        return this;
    }
    
    private <T> void register(ComponentFactory<T> factory, Class<T> type, int index) {
        factories.add(factory);
        typeIndices.put(type, index);
        nameIndices.put(factory.getComponentName(), index);
        byType.put(type, factory);
        byName.put(factory.getComponentName(), factory);
        
        // Rebuild arrays
        int size = factories.size();
        scopes = new Scope[size];
        lazyFlags = new boolean[size];
        for (int i = 0; i < size; i++) {
            ComponentFactory<?> f = factories.get(i);
            scopes[i] = f.getScope();
            lazyFlags[i] = f.isLazy();
        }
    }
    
    // ==================== Ultra-Fast Index-Based Methods ====================
    
    @Override
    public int getIndex(Class<?> type) {
        Integer idx = typeIndices.get(type);
        return idx != null ? idx : -1;
    }
    
    @Override
    public int getIndex(String name) {
        Integer idx = nameIndices.get(name);
        return idx != null ? idx : -1;
    }
    
    @Override
    public int getComponentCount() {
        return factories.size();
    }
    
    @Override
    public Scope getScope(int index) {
        return scopes[index];
    }
    
    @Override
    public boolean isLazy(int index) {
        return lazyFlags[index];
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(int index, VeldContainer container) {
        return (T) factories.get(index).create(container);
    }
    
    @Override
    public int[] getIndicesForType(Class<?> type) {
        Integer idx = typeIndices.get(type);
        if (idx != null) {
            return new int[] { idx };
        }
        return new int[0];
    }
    
    @Override
    public void invokePostConstruct(int index, Object instance) {
        @SuppressWarnings("unchecked")
        ComponentFactory<Object> factory = (ComponentFactory<Object>) factories.get(index);
        factory.invokePostConstruct(instance);
    }
    
    @Override
    public void invokePreDestroy(int index, Object instance) {
        @SuppressWarnings("unchecked")
        ComponentFactory<Object> factory = (ComponentFactory<Object>) factories.get(index);
        factory.invokePreDestroy(instance);
    }
    
    // ==================== Legacy Methods (for compatibility) ====================
    
    @Override
    public List<ComponentFactory<?>> getAllFactories() {
        return new ArrayList<>(factories);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> ComponentFactory<T> getFactory(Class<T> type) {
        return (ComponentFactory<T>) byType.get(type);
    }
    
    @Override
    public ComponentFactory<?> getFactory(String name) {
        return byName.get(name);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<ComponentFactory<? extends T>> getFactoriesForType(Class<T> type) {
        return factories.stream()
                .filter(f -> type.isAssignableFrom(f.getComponentType()))
                .map(f -> (ComponentFactory<? extends T>) f)
                .collect(Collectors.toList());
    }
    
    /**
     * Indexed ComponentFactory implementation with getIndex() support.
     */
    private static class IndexedFactory<T> implements ComponentFactory<T> {
        private final Class<T> type;
        private final String name;
        private final Scope scope;
        private final Function<VeldContainer, T> creator;
        private final int index;
        
        IndexedFactory(Class<T> type, String name, Scope scope, 
                       Function<VeldContainer, T> creator, int index) {
            this.type = type;
            this.name = name;
            this.scope = scope;
            this.creator = creator;
            this.index = index;
        }
        
        @Override
        public int getIndex() {
            return index;
        }
        
        @Override
        public Class<T> getComponentType() {
            return type;
        }
        
        @Override
        public String getComponentName() {
            return name;
        }
        
        @Override
        public Scope getScope() {
            return scope;
        }
        
        @Override
        public boolean isLazy() {
            return false;
        }
        
        @Override
        public T create(VeldContainer container) {
            return creator.apply(container);
        }
        
        @Override
        public void invokePostConstruct(T instance) {
            // No-op for benchmarks
        }
        
        @Override
        public void invokePreDestroy(T instance) {
            // No-op for benchmarks
        }
    }
}
