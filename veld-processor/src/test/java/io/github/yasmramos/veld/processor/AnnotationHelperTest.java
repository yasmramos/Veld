package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.processor.AnnotationHelper.InjectSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AnnotationHelper class.
 * Tests the detection and handling of JSR-330, Jakarta, and Veld annotations.
 */
@DisplayName("AnnotationHelper Tests")
class AnnotationHelperTest {
    
    @Nested
    @DisplayName("InjectSource Enum Tests")
    class InjectSourceTests {
        
        @Test
        @DisplayName("VELD source should have correct package")
        void veldSourceHasCorrectPackage() {
            assertEquals("io.github.yasmramos.veld.annotation", InjectSource.VELD.getPackageName());
        }
        
        @Test
        @DisplayName("JAVAX source should have correct package")
        void javaxSourceHasCorrectPackage() {
            assertEquals("javax.inject", InjectSource.JAVAX.getPackageName());
        }
        
        @Test
        @DisplayName("JAKARTA source should have correct package")
        void jakartaSourceHasCorrectPackage() {
            assertEquals("jakarta.inject", InjectSource.JAKARTA.getPackageName());
        }
        
        @Test
        @DisplayName("NONE source should have null package")
        void noneSourceHasNullPackage() {
            assertNull(InjectSource.NONE.getPackageName());
        }
        
        @Test
        @DisplayName("JAVAX and JAKARTA should be standard")
        void standardSourcesAreCorrectlyIdentified() {
            assertTrue(InjectSource.JAVAX.isStandard());
            assertTrue(InjectSource.JAKARTA.isStandard());
            assertFalse(InjectSource.VELD.isStandard());
            assertFalse(InjectSource.NONE.isStandard());
        }
    }
    
    @Nested
    @DisplayName("Provider Type Detection Tests")
    class ProviderTypeTests {
        
        @Test
        @DisplayName("Should detect javax.inject.Provider")
        void shouldDetectJavaxProvider() {
            assertTrue(AnnotationHelper.isProviderType("javax.inject.Provider"));
        }
        
        @Test
        @DisplayName("Should detect jakarta.inject.Provider")
        void shouldDetectJakartaProvider() {
            assertTrue(AnnotationHelper.isProviderType("jakarta.inject.Provider"));
        }
        
        @Test
        @DisplayName("Should not detect non-Provider types")
        void shouldNotDetectNonProviderTypes() {
            assertFalse(AnnotationHelper.isProviderType("java.util.function.Supplier"));
            assertFalse(AnnotationHelper.isProviderType("com.custom.Provider"));
            assertFalse(AnnotationHelper.isProviderType("Provider"));
        }
    }
}
