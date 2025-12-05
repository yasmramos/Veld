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

import com.veld.benchmark.common.*;
import com.veld.runtime.ComponentFactory;
import com.veld.runtime.Scope;
import com.veld.runtime.fast.FastContainer;
import com.veld.runtime.fast.FastRegistry;

/**
 * Helper to create FastContainer with manually registered components for benchmarks.
 * Uses ultra-fast array-based lookups for maximum performance.
 */
public class FastBenchmarkHelper {
    
    private FastBenchmarkHelper() {}
    
    /**
     * Creates a FastContainer with simple service dependencies.
     */
    public static FastContainer createSimpleContainer() {
        return new FastContainer(new SimpleFastRegistry());
    }
    
    /**
     * Creates a FastContainer with complex service dependencies.
     */
    public static FastContainer createComplexContainer() {
        return new FastContainer(new ComplexFastRegistry());
    }
    
    /**
     * Creates a FastContainer with prototype service for throughput testing.
     */
    public static FastContainer createPrototypeContainer() {
        return new FastContainer(new PrototypeFastRegistry());
    }
    
    // ==================== Simple Registry (Logger + SimpleService) ====================
    
    private static class SimpleFastRegistry implements FastRegistry {
        // Index mappings:
        // 0 = Logger (VeldLogger)
        // 1 = SimpleService (VeldSimpleService)
        
        private static final int COMPONENT_COUNT = 2;
        private static final Scope[] SCOPES = { Scope.SINGLETON, Scope.SINGLETON };
        private static final boolean[] LAZY = { false, false };
        
        @Override
        public int getIndex(Class<?> type) {
            if (type == Logger.class || type == VeldLogger.class) return 0;
            if (type == VeldSimpleService.class || type == Service.class) return 1;
            return -1;
        }
        
        @Override
        public int getIndex(String name) {
            switch (name) {
                case "logger": return 0;
                case "simpleService": return 1;
                default: return -1;
            }
        }
        
        @Override
        public ComponentFactory<?> getFactory(int index) {
            return null; // Not used in fast mode
        }
        
        @Override
        public Scope getScope(int index) {
            return SCOPES[index];
        }
        
        @Override
        public int getComponentCount() {
            return COMPONENT_COUNT;
        }
        
        @Override
        public int[] getIndicesForType(Class<?> type) {
            if (type == Logger.class) return new int[] { 0 };
            if (type == Service.class) return new int[] { 1 };
            return new int[0];
        }
        
        @Override
        public boolean isLazy(int index) {
            return LAZY[index];
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T create(int index, FastContainer container) {
            switch (index) {
                case 0: return (T) new VeldLogger();
                case 1: return (T) new VeldSimpleService(container.get(Logger.class));
                default: throw new IllegalArgumentException("Invalid index: " + index);
            }
        }
        
        @Override
        public void invokePostConstruct(int index, Object instance) {
            // No post-construct methods in benchmark components
        }
        
        @Override
        public void invokePreDestroy(int index, Object instance) {
            // No pre-destroy methods in benchmark components
        }
    }
    
    // ==================== Complex Registry (Full dependency tree) ====================
    
    private static class ComplexFastRegistry implements FastRegistry {
        // Index mappings:
        // 0 = Logger
        // 1 = Repository
        // 2 = Validator
        // 3 = ComplexService
        
        private static final int COMPONENT_COUNT = 4;
        private static final Scope[] SCOPES = { Scope.SINGLETON, Scope.SINGLETON, Scope.SINGLETON, Scope.SINGLETON };
        private static final boolean[] LAZY = { false, false, false, false };
        
        @Override
        public int getIndex(Class<?> type) {
            if (type == Logger.class || type == VeldLogger.class) return 0;
            if (type == Repository.class || type == VeldRepository.class) return 1;
            if (type == Validator.class || type == VeldValidator.class) return 2;
            if (type == VeldComplexService.class || type == ComplexService.class) return 3;
            return -1;
        }
        
        @Override
        public int getIndex(String name) {
            switch (name) {
                case "logger": return 0;
                case "repository": return 1;
                case "validator": return 2;
                case "complexService": return 3;
                default: return -1;
            }
        }
        
        @Override
        public ComponentFactory<?> getFactory(int index) {
            return null;
        }
        
        @Override
        public Scope getScope(int index) {
            return SCOPES[index];
        }
        
        @Override
        public int getComponentCount() {
            return COMPONENT_COUNT;
        }
        
        @Override
        public int[] getIndicesForType(Class<?> type) {
            if (type == Logger.class) return new int[] { 0 };
            if (type == Repository.class) return new int[] { 1 };
            if (type == Validator.class) return new int[] { 2 };
            if (type == ComplexService.class) return new int[] { 3 };
            return new int[0];
        }
        
        @Override
        public boolean isLazy(int index) {
            return LAZY[index];
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T create(int index, FastContainer container) {
            switch (index) {
                case 0: return (T) new VeldLogger();
                case 1: return (T) new VeldRepository();
                case 2: return (T) new VeldValidator();
                case 3: return (T) new VeldComplexService(
                    container.get(Repository.class),
                    container.get(Logger.class),
                    container.get(Validator.class)
                );
                default: throw new IllegalArgumentException("Invalid index: " + index);
            }
        }
        
        @Override
        public void invokePostConstruct(int index, Object instance) {
        }
        
        @Override
        public void invokePreDestroy(int index, Object instance) {
        }
    }
    
    // ==================== Prototype Registry ====================
    
    private static class PrototypeFastRegistry implements FastRegistry {
        // Index mappings:
        // 0 = Logger (singleton)
        // 1 = SimpleService (prototype)
        
        private static final int COMPONENT_COUNT = 2;
        private static final Scope[] SCOPES = { Scope.SINGLETON, Scope.PROTOTYPE };
        private static final boolean[] LAZY = { false, false };
        
        @Override
        public int getIndex(Class<?> type) {
            if (type == Logger.class || type == VeldLogger.class) return 0;
            if (type == VeldSimpleService.class || type == Service.class) return 1;
            return -1;
        }
        
        @Override
        public int getIndex(String name) {
            switch (name) {
                case "logger": return 0;
                case "simpleService": return 1;
                default: return -1;
            }
        }
        
        @Override
        public ComponentFactory<?> getFactory(int index) {
            return null;
        }
        
        @Override
        public Scope getScope(int index) {
            return SCOPES[index];
        }
        
        @Override
        public int getComponentCount() {
            return COMPONENT_COUNT;
        }
        
        @Override
        public int[] getIndicesForType(Class<?> type) {
            if (type == Logger.class) return new int[] { 0 };
            if (type == Service.class) return new int[] { 1 };
            return new int[0];
        }
        
        @Override
        public boolean isLazy(int index) {
            return LAZY[index];
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T create(int index, FastContainer container) {
            switch (index) {
                case 0: return (T) new VeldLogger();
                case 1: return (T) new VeldSimpleService(container.get(Logger.class));
                default: throw new IllegalArgumentException("Invalid index: " + index);
            }
        }
        
        @Override
        public void invokePostConstruct(int index, Object instance) {
        }
        
        @Override
        public void invokePreDestroy(int index, Object instance) {
        }
    }
}
