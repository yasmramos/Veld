package io.github.yasmramos.veld.aop.interceptor;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Logged annotation and its Level enum.
 */
class LoggedAnnotationTest {

    @Test
    void testLevelEnumValues() {
        Logged.Level[] values = Logged.Level.values();
        
        assertEquals(5, values.length);
        assertNotNull(Logged.Level.valueOf("TRACE"));
        assertNotNull(Logged.Level.valueOf("DEBUG"));
        assertNotNull(Logged.Level.valueOf("INFO"));
        assertNotNull(Logged.Level.valueOf("WARN"));
        assertNotNull(Logged.Level.valueOf("ERROR"));
    }

    @Test
    void testLevelEnumOrdinal() {
        assertEquals(0, Logged.Level.TRACE.ordinal());
        assertEquals(1, Logged.Level.DEBUG.ordinal());
        assertEquals(2, Logged.Level.INFO.ordinal());
        assertEquals(3, Logged.Level.WARN.ordinal());
        assertEquals(4, Logged.Level.ERROR.ordinal());
    }

    @Test
    void testLoggedHasRuntimeRetention() {
        Retention retention = Logged.class.getAnnotation(Retention.class);
        
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Logged
    static class DefaultLoggedClass {}

    @Test
    void testDefaultAnnotationValues() {
        Logged annotation = DefaultLoggedClass.class.getAnnotation(Logged.class);
        
        assertNotNull(annotation);
        assertEquals(Logged.Level.INFO, annotation.level());
        assertTrue(annotation.logArgs());
        assertTrue(annotation.logResult());
        assertFalse(annotation.logTime());
    }

    @Logged(level = Logged.Level.DEBUG, logArgs = false, logResult = false, logTime = true)
    static class CustomLoggedClass {}

    @Test
    void testCustomAnnotationValues() {
        Logged annotation = CustomLoggedClass.class.getAnnotation(Logged.class);
        
        assertNotNull(annotation);
        assertEquals(Logged.Level.DEBUG, annotation.level());
        assertFalse(annotation.logArgs());
        assertFalse(annotation.logResult());
        assertTrue(annotation.logTime());
    }
}
