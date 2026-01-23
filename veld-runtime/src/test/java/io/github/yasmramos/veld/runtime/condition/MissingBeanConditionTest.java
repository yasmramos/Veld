package io.github.yasmramos.veld.runtime.condition;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for MissingBeanCondition.
 */
@DisplayName("MissingBeanCondition Tests")
class MissingBeanConditionTest {
    
    @Mock
    private ConditionContext mockContext;
    
    private AutoCloseable mocks;
    
    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }
    
    @Test
    @DisplayName("Should create condition for types")
    void shouldCreateConditionForTypes() {
        MissingBeanCondition condition = MissingBeanCondition.forTypes("com.example.Type1", "com.example.Type2");
        
        assertEquals(2, condition.getBeanTypes().size());
        assertTrue(condition.getBeanTypes().contains("com.example.Type1"));
        assertTrue(condition.getBeanTypes().contains("com.example.Type2"));
        assertTrue(condition.getBeanNames().isEmpty());
    }
    
    @Test
    @DisplayName("Should create condition for names")
    void shouldCreateConditionForNames() {
        MissingBeanCondition condition = MissingBeanCondition.forNames("bean1", "bean2");
        
        assertEquals(2, condition.getBeanNames().size());
        assertTrue(condition.getBeanNames().contains("bean1"));
        assertTrue(condition.getBeanNames().contains("bean2"));
        assertTrue(condition.getBeanTypes().isEmpty());
    }
    
    @Test
    @DisplayName("Should match when type is missing")
    void shouldMatchWhenTypeIsMissing() {
        when(mockContext.containsBeanType("com.example.Missing")).thenReturn(false);
        
        MissingBeanCondition condition = MissingBeanCondition.forTypes("com.example.Missing");
        
        assertTrue(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should not match when type is present")
    void shouldNotMatchWhenTypeIsPresent() {
        when(mockContext.containsBeanType("com.example.Present")).thenReturn(true);
        
        MissingBeanCondition condition = MissingBeanCondition.forTypes("com.example.Present");
        
        assertFalse(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should match when name is missing")
    void shouldMatchWhenNameIsMissing() {
        when(mockContext.containsBeanName("missingBean")).thenReturn(false);
        
        MissingBeanCondition condition = MissingBeanCondition.forNames("missingBean");
        
        assertTrue(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should not match when name is present")
    void shouldNotMatchWhenNameIsPresent() {
        when(mockContext.containsBeanName("presentBean")).thenReturn(true);
        
        MissingBeanCondition condition = MissingBeanCondition.forNames("presentBean");
        
        assertFalse(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should match when all types are missing")
    void shouldMatchWhenAllTypesAreMissing() {
        when(mockContext.containsBeanType(anyString())).thenReturn(false);
        
        MissingBeanCondition condition = MissingBeanCondition.forTypes("type1", "type2", "type3");
        
        assertTrue(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should not match when any type is present")
    void shouldNotMatchWhenAnyTypeIsPresent() {
        when(mockContext.containsBeanType("type1")).thenReturn(false);
        when(mockContext.containsBeanType("type2")).thenReturn(true);
        when(mockContext.containsBeanType("type3")).thenReturn(false);
        
        MissingBeanCondition condition = MissingBeanCondition.forTypes("type1", "type2", "type3");
        
        assertFalse(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should match with both types and names when all missing")
    void shouldMatchWithBothTypesAndNamesWhenAllMissing() {
        when(mockContext.containsBeanType("type1")).thenReturn(false);
        when(mockContext.containsBeanName("name1")).thenReturn(false);
        
        MissingBeanCondition condition = new MissingBeanCondition(
                List.of("type1"),
                List.of("name1")
        );
        
        assertTrue(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should provide description with types")
    void shouldProvideDescriptionWithTypes() {
        MissingBeanCondition condition = MissingBeanCondition.forTypes("com.example.Type");
        
        String description = condition.getDescription();
        
        assertTrue(description.contains("@ConditionalOnMissingBean"));
        assertTrue(description.contains("com.example.Type"));
    }
    
    @Test
    @DisplayName("Should provide description with names")
    void shouldProvideDescriptionWithNames() {
        MissingBeanCondition condition = MissingBeanCondition.forNames("myBean");
        
        String description = condition.getDescription();
        
        assertTrue(description.contains("@ConditionalOnMissingBean"));
        assertTrue(description.contains("myBean"));
    }
    
    @Test
    @DisplayName("Should match when no types or names specified")
    void shouldMatchWhenNoTypesOrNamesSpecified() {
        MissingBeanCondition condition = new MissingBeanCondition(
                Collections.emptyList(),
                Collections.emptyList()
        );
        
        assertTrue(condition.matches(mockContext));
    }

    @Nested
    @DisplayName("Failure Reason Tests")
    class FailureReasonTests {

        @Test
        @DisplayName("Should list found bean types that should be absent")
        void shouldListFoundBeanTypes() {
            when(mockContext.containsBeanType("com.example.DataSource")).thenReturn(true);

            MissingBeanCondition condition = MissingBeanCondition.forTypes("com.example.DataSource");

            String reason = condition.getFailureReason(mockContext);

            assertTrue(reason.contains("Found beans that should be absent"));
            assertTrue(reason.contains("Found bean types"));
            assertTrue(reason.contains("com.example.DataSource"));
        }

        @Test
        @DisplayName("Should list found bean names that should be absent")
        void shouldListFoundBeanNames() {
            when(mockContext.containsBeanName("myBean")).thenReturn(true);

            MissingBeanCondition condition = MissingBeanCondition.forNames("myBean");

            String reason = condition.getFailureReason(mockContext);

            assertTrue(reason.contains("Found bean names"));
            assertTrue(reason.contains("myBean"));
        }

        @Test
        @DisplayName("Should return empty when no beans found")
        void shouldReturnEmptyWhenNoBeansFound() {
            when(mockContext.containsBeanType("com.example.Missing")).thenReturn(false);

            MissingBeanCondition condition = MissingBeanCondition.forTypes("com.example.Missing");

            String reason = condition.getFailureReason(mockContext);

            assertEquals("", reason);
        }

        @Test
        @DisplayName("Should list both types and names when both found")
        void shouldListBothTypesAndNamesWhenBothFound() {
            when(mockContext.containsBeanType("com.example.Type")).thenReturn(true);
            when(mockContext.containsBeanName("beanName")).thenReturn(true);

            MissingBeanCondition condition = new MissingBeanCondition(
                List.of("com.example.Type"),
                List.of("beanName")
            );

            String reason = condition.getFailureReason(mockContext);

            assertTrue(reason.contains("Found bean types"));
            assertTrue(reason.contains("com.example.Type"));
            assertTrue(reason.contains("Found bean names"));
            assertTrue(reason.contains("beanName"));
        }
    }
}
