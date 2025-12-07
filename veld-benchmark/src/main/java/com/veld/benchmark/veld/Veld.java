/*
 * Copyright 2024 Veld Framework
 *
 * Simulated generated class - exactly matches what VeldBootstrapGenerator produces.
 * Uses <clinit> static initialization for maximum performance.
 * THIS IS THE CONTAINER - ultra-fast with complete functionality.
 */
package com.veld.benchmark.veld;

import java.util.ArrayList;
import java.util.List;

/**
 * Ultra-fast DI container - same pattern as generated code.
 * No separate container class needed.
 */
public final class Veld {
    
    // === SINGLETON FIELDS (static final = JIT inlines) ===
    private static final VeldLogger _veldLogger;
    private static final VeldValidator _veldValidator;
    private static final VeldRepository _veldRepository;
    private static final VeldSimpleService _veldSimpleService;
    private static final VeldComplexService _veldComplexService;
    
    // === LOOKUP ARRAYS (for get(Class) and getAll(Class)) ===
    private static final Class<?>[] _types;
    private static final Object[] _instances;
    
    // <clinit> - JVM guarantees thread-safe initialization
    static {
        // Topologically sorted - dependencies first
        _veldLogger = new VeldLogger();
        _veldValidator = new VeldValidator();
        _veldRepository = new VeldRepository();
        _veldSimpleService = new VeldSimpleService(_veldLogger);
        _veldComplexService = new VeldComplexService(_veldRepository, _veldLogger, _veldValidator);
        
        // Initialize lookup arrays
        _types = new Class<?>[] {
            VeldLogger.class,
            VeldValidator.class,
            VeldRepository.class,
            VeldSimpleService.class,
            VeldComplexService.class
        };
        _instances = new Object[] {
            _veldLogger,
            _veldValidator,
            _veldRepository,
            _veldSimpleService,
            _veldComplexService
        };
    }
    
    private Veld() {}
    
    // === DIRECT GETTERS (ultra-fast: getstatic + areturn = 2 instructions) ===
    public static VeldLogger veldLogger() { return _veldLogger; }
    public static VeldValidator veldValidator() { return _veldValidator; }
    public static VeldRepository veldRepository() { return _veldRepository; }
    public static VeldSimpleService veldSimpleService() { return _veldSimpleService; }
    public static VeldComplexService veldComplexService() { return _veldComplexService; }
    
    // === CONTAINER API ===
    
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        // O(n) scan - ultra-fast for small n, no Maps
        for (int i = 0; i < _types.length; i++) {
            if (_types[i] == type) {
                return (T) _instances[i];
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> List<T> getAll(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < _types.length; i++) {
            if (type.isAssignableFrom(_types[i])) {
                result.add((T) _instances[i]);
            }
        }
        return result;
    }
    
    public static boolean contains(Class<?> type) {
        return get(type) != null;
    }
    
    public static int componentCount() {
        return _types.length;
    }
}
