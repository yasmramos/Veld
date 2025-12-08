package com.veld.generated;

import java.util.ArrayList;
import java.util.List;

import com.veld.benchmark.veld.*;

/**
 * Ultra-fast DI container - manually written to match VeldSourceGenerator output.
 * All singletons initialized in static block (thread-safe, zero runtime overhead).
 * 
 * This is what VeldSourceGenerator produces for real projects.
 * For benchmarks, we use this manual version to avoid circular dependency with the processor.
 */
public final class Veld {

    // === SINGLETON FIELDS (static final = JIT inlines) ===
    private static final VeldLogger _veldLogger;
    private static final VeldValidator _veldValidator;
    private static final VeldRepository _veldRepository;
    private static final VeldSimpleService _veldSimpleService;
    private static final VeldComplexService _veldComplexService;

    // === LOOKUP ARRAYS ===
    private static final Class<?>[] _types;
    private static final Object[] _instances;

    // === STATIC INITIALIZER (JVM guarantees thread-safety) ===
    static {
        _veldLogger = new VeldLogger();
        _veldValidator = new VeldValidator();
        _veldRepository = new VeldRepository();
        _veldSimpleService = new VeldSimpleService(_veldLogger);
        _veldComplexService = new VeldComplexService(_veldRepository, _veldLogger, _veldValidator);

        _types = new Class<?>[] {
            VeldLogger.class,
            com.veld.benchmark.common.Logger.class,
            VeldValidator.class,
            VeldRepository.class,
            VeldSimpleService.class,
            com.veld.benchmark.common.Service.class,
            VeldComplexService.class
        };
        _instances = new Object[] {
            _veldLogger,
            _veldLogger,
            _veldValidator,
            _veldRepository,
            _veldSimpleService,
            _veldSimpleService,
            _veldComplexService
        };
    }

    private Veld() {}

    // === DIRECT GETTERS (ultra-fast) ===
    public static VeldLogger veldLogger() {
        return _veldLogger;
    }

    public static VeldValidator veldValidator() {
        return _veldValidator;
    }

    public static VeldRepository veldRepository() {
        return _veldRepository;
    }

    public static VeldSimpleService veldSimpleService() {
        return _veldSimpleService;
    }

    public static VeldComplexService veldComplexService() {
        return _veldComplexService;
    }

    // === CONTAINER API (O(1) lookup via if-else chain) ===

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        // Direct reference comparison - JIT inlines this completely
        if (type == VeldLogger.class || type == com.veld.benchmark.common.Logger.class) {
            return (T) _veldLogger;
        }
        if (type == VeldSimpleService.class || type == com.veld.benchmark.common.Service.class) {
            return (T) _veldSimpleService;
        }
        if (type == VeldComplexService.class) {
            return (T) _veldComplexService;
        }
        if (type == VeldValidator.class) {
            return (T) _veldValidator;
        }
        if (type == VeldRepository.class) {
            return (T) _veldRepository;
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
        return 5;
    }
}
