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
 * Tests for PresentBeanCondition.
 */
@DisplayName("PresentBeanCondition Tests")
class PresentBeanConditionTest {
    
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
        PresentBeanCondition condition = PresentBeanCondition.forTypes("com.example.Type1", "com.example.Type2");
        
        assertEquals(2, condition.getBeanTypes().size());
        assertTrue(condition.getBeanTypes().contains("com.example.Type1"));
        assertTrue(condition.getBeanTypes().contains("com.example.Type2"));
        assertTrue(condition.getBeanNames().isEmpty());
    }
    
    @Test
    @DisplayName("Should create condition for names")
    void shouldCreateConditionForNames() {
        PresentBeanCondition condition = PresentBeanCondition.forNames("bean1", "bean2");
        
        assertEquals(2, condition.getBeanNames().size());
        assertTrue(condition.getBeanNames().contains("bean1"));
        assertTrue(condition.getBeanNames().contains("bean2"));
        assertTrue(condition.getBeanTypes().isEmpty());
    }
    
    @Test
    @DisplayName("Should match when type is present")
    void shouldMatchWhenTypeIsPresent() {
        when(mockContext.containsBeanType("com.example.Present")).thenReturn(true);
        
        PresentBeanCondition condition = PresentBeanCondition.forTypes("com.example.Present");
        
        assertTrue(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should not match when type is missing")
    void shouldNotMatchWhenTypeIsMissing() {
        when(mockContext.containsBeanType("com.example.Missing")).thenReturn(false);
        
        PresentBeanCondition condition = PresentBeanCondition.forTypes("com.example.Missing");
        
        assertFalse(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should match when name is present")
    void shouldMatchWhenNameIsPresent() {
        when(mockContext.containsBeanName("presentBean")).thenReturn(true);
        
        PresentBeanCondition condition = PresentBeanCondition.forNames("presentBean");
        
        assertTrue(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should not match when name is missing")
    void shouldNotMatchWhenNameIsMissing() {
        when(mockContext.containsBeanName("missingBean")).thenReturn(false);
        
        PresentBeanCondition condition = PresentBeanCondition.forNames("missingBean");
        
        assertFalse(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should match when all types are present")
    void shouldMatchWhenAllTypesArePresent() {
        when(mockContext.containsBeanType(anyString())).thenReturn(true);
        
        PresentBeanCondition condition = PresentBeanCondition.forTypes("type1", "type2", "type3");
        
        assertTrue(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should not match when any type is missing")
    void shouldNotMatchWhenAnyTypeIsMissing() {
        when(mockContext.containsBeanType("type1")).thenReturn(true);
        when(mockContext.containsBeanType("type2")).thenReturn(false);
        when(mockContext.containsBeanType("type3")).thenReturn(true);
        
        PresentBeanCondition condition = PresentBeanCondition.forTypes("type1", "type2", "type3");
        
        assertFalse(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should match with both types and names when all present")
    void shouldMatchWithBothTypesAndNamesWhenAllPresent() {
        when(mockContext.containsBeanType("type1")).thenReturn(true);
        when(mockContext.containsBeanName("name1")).thenReturn(true);
        
        PresentBeanCondition condition = new PresentBeanCondition(
                List.of("type1"),
                List.of("name1")
        );
        
        assertTrue(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should not match when type present but name missing")
    void shouldNotMatchWhenTypePresentButNameMissing() {
        when(mockContext.containsBeanType("type1")).thenReturn(true);
        when(mockContext.containsBeanName("name1")).thenReturn(false);
        
        PresentBeanCondition condition = new PresentBeanCondition(
                List.of("type1"),
                List.of("name1")
        );
        
        assertFalse(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should provide description with types")
    void shouldProvideDescriptionWithTypes() {
        PresentBeanCondition condition = PresentBeanCondition.forTypes("com.example.Type");
        
        String description = condition.getDescription();
        
        assertTrue(description.contains("@ConditionalOnBean"));
        assertTrue(description.contains("com.example.Type"));
    }
    
    @Test
    @DisplayName("Should provide description with names")
    void shouldProvideDescriptionWithNames() {
        PresentBeanCondition condition = PresentBeanCondition.forNames("myBean");
        
        String description = condition.getDescription();
        
        assertTrue(description.contains("@ConditionalOnBean"));
        assertTrue(description.contains("myBean"));
    }
    
    @Test
    @DisplayName("Should match when no types or names specified")
    void shouldMatchWhenNoTypesOrNamesSpecified() {
        PresentBeanCondition condition = new PresentBeanCondition(
                Collections.emptyList(),
                Collections.emptyList()
        );
        
        assertTrue(condition.matches(mockContext));
    }
    
    @Test
    @DisplayName("Should be inverse of MissingBeanCondition")
    void shouldBeInverseOfMissingBeanCondition() {
        String typeName = "com.example.TestType";
        String beanName = "testBean";
        
        // When type is present
        when(mockContext.containsBeanType(typeName)).thenReturn(true);
        when(mockContext.containsBeanName(beanName)).thenReturn(true);
        
        PresentBeanCondition presentCondition = PresentBeanCondition.forTypes(typeName);
        MissingBeanCondition missingCondition = MissingBeanCondition.forTypes(typeName);
        
        assertTrue(presentCondition.matches(mockContext));
        assertFalse(missingCondition.matches(mockContext));
        
        reset(mockContext);
        
        // When type is missing
        when(mockContext.containsBeanType(typeName)).thenReturn(false);
        when(mockContext.containsBeanName(beanName)).thenReturn(false);
        
        assertFalse(presentCondition.matches(mockContext));
        assertTrue(missingCondition.matches(mockContext));
    }
}
