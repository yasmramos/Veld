package io.github.yasmramos.veld.runtime.condition;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ConditionEvaluator.
 */
@DisplayName("ConditionEvaluator Tests")
class ConditionEvaluatorTest {
    
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
    
    // Helper method to create a simple condition
    private Condition createCondition(boolean result, String description) {
        return new Condition() {
            @Override
            public boolean matches(ConditionContext context) {
                return result;
            }
            
            @Override
            public String getDescription() {
                return description;
            }
        };
    }
    
    @Test
    @DisplayName("Should create evaluator with component name")
    void shouldCreateEvaluatorWithComponentName() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        
        assertEquals("testComponent", evaluator.getComponentName());
    }
    
    @Test
    @DisplayName("Should have no conditions initially")
    void shouldHaveNoConditionsInitially() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        
        assertFalse(evaluator.hasConditions());
        assertTrue(evaluator.getConditions().isEmpty());
    }
    
    @Test
    @DisplayName("Should add condition")
    void shouldAddCondition() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        Condition condition = createCondition(true, "test");
        
        evaluator.addCondition(condition);
        
        assertTrue(evaluator.hasConditions());
        assertEquals(1, evaluator.getConditions().size());
    }
    
    @Test
    @DisplayName("Should add property condition")
    void shouldAddPropertyCondition() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        
        evaluator.addPropertyCondition("test.prop", "expectedValue", false);
        
        assertTrue(evaluator.hasConditions());
    }
    
    @Test
    @DisplayName("Should add class condition")
    void shouldAddClassCondition() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        
        evaluator.addClassCondition("java.lang.String");
        
        assertTrue(evaluator.hasConditions());
    }
    
    @Test
    @DisplayName("Should add missing bean condition")
    void shouldAddMissingBeanCondition() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        
        evaluator.addMissingBeanCondition("com.example.SomeBean");
        
        assertTrue(evaluator.hasConditions());
    }
    
    @Test
    @DisplayName("Should add missing bean name condition")
    void shouldAddMissingBeanNameCondition() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        
        evaluator.addMissingBeanNameCondition("beanName");
        
        assertTrue(evaluator.hasConditions());
    }
    
    @Test
    @DisplayName("Should add profile condition")
    void shouldAddProfileCondition() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        
        evaluator.addProfileCondition("dev", "test");
        
        assertTrue(evaluator.hasConditions());
    }
    
    @Test
    @DisplayName("Should evaluate to true when no conditions")
    void shouldEvaluateToTrueWhenNoConditions() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        
        assertTrue(evaluator.evaluate(mockContext));
    }
    
    @Test
    @DisplayName("Should evaluate to true when all conditions pass")
    void shouldEvaluateToTrueWhenAllConditionsPass() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        evaluator.addCondition(createCondition(true, "condition1"));
        evaluator.addCondition(createCondition(true, "condition2"));
        
        assertTrue(evaluator.evaluate(mockContext));
    }
    
    @Test
    @DisplayName("Should evaluate to false when any condition fails")
    void shouldEvaluateToFalseWhenAnyConditionFails() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        evaluator.addCondition(createCondition(true, "passing"));
        evaluator.addCondition(createCondition(false, "failing"));
        
        assertFalse(evaluator.evaluate(mockContext));
    }
    
    @Test
    @DisplayName("Should support method chaining")
    void shouldSupportMethodChaining() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent")
                .addCondition(createCondition(true, "test"))
                .addPropertyCondition("prop", "val", false)
                .addClassCondition("java.lang.String")
                .addProfileCondition("dev");
        
        assertTrue(evaluator.hasConditions());
        assertEquals(4, evaluator.getConditions().size());
    }
    
    @Test
    @DisplayName("Should provide failure message for failing conditions")
    void shouldProvideFailureMessage() {
        ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
        evaluator.addCondition(createCondition(false, "Test condition failed"));
        
        String failureMessage = evaluator.getFailureMessage(mockContext);
        
        assertTrue(failureMessage.contains("testComponent"));
        assertTrue(failureMessage.contains("Test condition failed"));
    }

    @Nested
    @DisplayName("Detailed Failure Message Tests")
    class DetailedFailureMessageTests {

        @Test
        @DisplayName("Should use getFailureReason for detailed messages")
        void shouldUseGetFailureReasonForDetailedMessages() {
            Condition condition = new Condition() {
                @Override
                public boolean matches(ConditionContext context) {
                    return false;
                }

                @Override
                public String getDescription() {
                    return "@ConditionalOnTest";
                }

                @Override
                public String getFailureReason(ConditionContext context) {
                    return "Test failed because the magic number was 42 but expected 24";
                }
            };

            ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
            evaluator.addCondition(condition);

            String failureMessage = evaluator.getFailureMessage(mockContext);

            assertTrue(failureMessage.contains("testComponent"));
            assertTrue(failureMessage.contains("magic number was 42 but expected 24"));
        }

        @Test
        @DisplayName("Should combine multiple failure reasons")
        void shouldCombineMultipleFailureReasons() {
            when(mockContext.isClassPresent("com.fake.ClassA")).thenReturn(false);
            when(mockContext.getProperty("missing.prop")).thenReturn(null);

            ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
            evaluator.addClassCondition("com.fake.ClassA");
            evaluator.addPropertyCondition("missing.prop", "value", false);

            String failureMessage = evaluator.getFailureMessage(mockContext);

            assertTrue(failureMessage.contains("testComponent"));
            assertTrue(failureMessage.contains("Required class not found"));
            assertTrue(failureMessage.contains("com.fake.ClassA"));
            assertTrue(failureMessage.contains("missing.prop"));
            assertTrue(failureMessage.contains("not set"));
        }

        @Test
        @DisplayName("Should return empty string when all conditions pass")
        void shouldReturnEmptyWhenAllPass() {
            when(mockContext.isClassPresent("java.lang.String")).thenReturn(true);

            ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
            evaluator.addClassCondition("java.lang.String");

            String failureMessage = evaluator.getFailureMessage(mockContext);

            assertEquals("", failureMessage);
        }

        @Test
        @DisplayName("Should fallback to description when getFailureReason returns empty")
        void shouldFallbackToDescriptionWhenReasonEmpty() {
            Condition condition = new Condition() {
                @Override
                public boolean matches(ConditionContext context) {
                    return false;
                }

                @Override
                public String getDescription() {
                    return "@CustomCondition(fallback)";
                }

                @Override
                public String getFailureReason(ConditionContext context) {
                    return ""; // Empty, should fallback
                }
            };

            ConditionEvaluator evaluator = new ConditionEvaluator("testComponent");
            evaluator.addCondition(condition);

            String failureMessage = evaluator.getFailureMessage(mockContext);

            assertTrue(failureMessage.contains("@CustomCondition(fallback)"));
        }
    }
}
