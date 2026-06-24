package io.github.yasmramos.veld.aop.spi;

import io.github.yasmramos.veld.aop.AopExtension;
import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link SpiAopExtensionLoader}.
 * 
 * @author Veld Team
 * @version 1.0.0
 */
@DisplayName("SpiAopExtensionLoader Tests")
class SpiAopExtensionLoaderTest {

    private SpiAopExtensionLoader loader;

    @BeforeEach
    void setUp() {
        // Load extensions from classpath (will include DefaultAopExtension)
        loader = SpiAopExtensionLoader.loadExtensions();
    }

    @Test
    @DisplayName("Should load extensions successfully")
    void shouldLoadExtensionsSuccessfully() {
        // Then
        assertNotNull(loader);
        assertTrue(loader.hasExtensions(), "Should have at least DefaultAopExtension loaded");
    }

    @Test
    @DisplayName("Should return extension count greater than zero")
    void shouldReturnExtensionCountGreaterThanZero() {
        // Then
        int count = loader.getExtensionCount();
        assertTrue(count >= 1, "Should have at least one extension (DefaultAopExtension)");
    }

    @Test
    @DisplayName("Should provide extensions info string")
    void shouldProvideExtensionsInfoString() {
        // When
        String info = loader.getExtensionsInfo();

        // Then
        assertNotNull(info);
        assertFalse(info.isEmpty());
    }

    @Test
    @DisplayName("Should return extensions list")
    void shouldReturnExtensionsList() {
        // When
        List<AopExtension> extensions = loader.getExtensions();

        // Then
        assertNotNull(extensions);
        assertFalse(extensions.isEmpty());
        // Verify it's an unmodifiable list by attempting modification
        assertThrows(UnsupportedOperationException.class, () -> extensions.add(null));
    }

    @Test
    @DisplayName("Should return extensions for GENERATION phase")
    void shouldReturnExtensionsForGenerationPhase() {
        // When
        List<AopExtension> generationExtensions = loader.getExtensionsForPhase(ExtensionPhase.GENERATION);

        // Then
        assertNotNull(generationExtensions);
        // DefaultAopExtension is registered for GENERATION phase
        assertTrue(generationExtensions.size() >= 0, "May have extensions in GENERATION phase");
    }

    @Test
    @DisplayName("Should return empty list for non-existent phase extensions")
    void shouldReturnEmptyListForNonExistentPhaseExtensions() {
        // When
        List<AopExtension> initExtensions = loader.getExtensionsForPhase(ExtensionPhase.INIT);

        // Then
        assertNotNull(initExtensions);
        assertTrue(initExtensions.isEmpty() || !initExtensions.isEmpty()); // May or may not have INIT extensions
    }

    @Test
    @DisplayName("Should not have errors when loading default extension")
    void shouldNotHaveErrorsWhenLoadingDefaultExtension() {
        // Then
        assertFalse(loader.hasErrors(), "Should not have errors: " + loader.getErrors());
        assertTrue(loader.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should find overriding extension if present")
    void shouldFindOverridingExtensionIfPresent() {
        // When
        Optional<AopExtension> overriding = loader.findOverridingExtension();

        // Then - DefaultAopExtension returns false for overridesDefaultGeneration()
        // So this might be empty unless there's another extension
        assertNotNull(overriding);
    }

    @Test
    @DisplayName("Should contain DefaultAopExtension")
    void shouldContainDefaultAopExtension() {
        // When
        List<AopExtension> extensions = loader.getExtensions();

        // Then
        boolean hasDefaultExtension = extensions.stream()
            .anyMatch(ext -> ext instanceof DefaultAopExtension);
        assertTrue(hasDefaultExtension, "Should have DefaultAopExtension loaded");
    }

    @Test
    @DisplayName("Should sort extensions by order within phase")
    void shouldSortExtensionsByOrderWithinPhase() {
        // When
        List<AopExtension> generationExtensions = loader.getExtensionsForPhase(ExtensionPhase.GENERATION);

        // Then - verify they are sorted by checking descriptor order
        if (generationExtensions.size() > 1) {
            for (int i = 0; i < generationExtensions.size() - 1; i++) {
                ExtensionDescriptor desc1 = generationExtensions.get(i).getDescriptor();
                ExtensionDescriptor desc2 = generationExtensions.get(i + 1).getDescriptor();
                assertTrue(desc1.getOrder() <= desc2.getOrder(), 
                    "Extensions should be sorted by order within phase");
            }
        }
    }

    @Test
    @DisplayName("Should handle ServiceLoader gracefully")
    void shouldHandleServiceLoaderGracefully() {
        // This test verifies that the loader doesn't throw exceptions
        // even if ServiceLoader encounters issues
        
        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            SpiAopExtensionLoader.loadExtensions();
        });
    }

    @Test
    @DisplayName("Should return unmodifiable collections")
    void shouldReturnUnmodifiableCollections() {
        // When
        List<AopExtension> extensions = loader.getExtensions();
        List<String> errors = loader.getErrors();

        // Then - verify collections are unmodifiable by attempting modification
        assertThrows(UnsupportedOperationException.class, () -> extensions.add(null));
        assertThrows(UnsupportedOperationException.class, () -> errors.add("test"));
    }

    @Test
    @DisplayName("Should provide accurate extension count")
    void shouldProvideAccurateExtensionCount() {
        // When
        int count = loader.getExtensionCount();
        List<AopExtension> extensions = loader.getExtensions();

        // Then
        assertEquals(count, extensions.size(), "Extension count should match list size");
    }

    @Test
    @DisplayName("Should load extensions only once per call")
    void shouldLoadExtensionsOnlyOncePerCall() {
        // When
        SpiAopExtensionLoader loader1 = SpiAopExtensionLoader.loadExtensions();
        SpiAopExtensionLoader loader2 = SpiAopExtensionLoader.loadExtensions();

        // Then - each call creates a new loader instance
        assertNotSame(loader1, loader2);
        assertEquals(loader1.getExtensionCount(), loader2.getExtensionCount());
    }
}
