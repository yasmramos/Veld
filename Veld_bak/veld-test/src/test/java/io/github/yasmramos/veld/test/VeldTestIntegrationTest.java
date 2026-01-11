package io.github.yasmramos.veld.test;

import io.github.yasmramos.veld.test.context.TestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the veld-test module.
 *
 * <p>These tests verify the correct functioning of the
 * annotations and JUnit 5 extension.</p>
 */
class VeldTestIntegrationTest {

    /**
     * Basic test for real bean injection.
     */
    @Nested
    @DisplayName("Basic Injection Tests")
    class BasicInjectionTests {

        @Test
        @DisplayName("Should inject real beans correctly")
        void shouldInjectRealBeans() {
            try (TestContext context = TestContext.Builder.create()
                    .withProfile("test")
                    .build()) {
                GreetingService service = context.getBean(GreetingService.class);
                assertThat(service).isNotNull();
                assertThat(service.greet("World")).isEqualTo("Hello, World!");
            }
        }

        @Test
        @DisplayName("Should allow access to multiple beans")
        void shouldAllowMultipleBeans() {
            try (TestContext context = TestContext.Builder.create()
                    .withProfile("test")
                    .build()) {
                GreetingService service = context.getBean(GreetingService.class);
                CalculatorService calculator = context.getBean(CalculatorService.class);
                assertThat(service).isNotNull();
                assertThat(calculator).isNotNull();

                assertThat(service.greet("Test")).isEqualTo("Hello, Test!");
                assertThat(calculator.add(2, 3)).isEqualTo(5);
            }
        }
    }

    /**
     * Test for direct TestContext usage.
     */
    @Nested
    @DisplayName("TestContext Tests")
    class TestContextTests {

        @Test
        @DisplayName("Should allow direct TestContext usage")
        void shouldAllowDirectTestContextUsage() {
            try (TestContext context = TestContext.Builder.create()
                    .withProfile("test")
                    .build()) {
                GreetingService service = context.getBean(GreetingService.class);
                assertThat(service).isNotNull();
                assertThat(service.greet("Direct")).isEqualTo("Hello, Direct!");
            }
        }

        @Test
        @DisplayName("Should allow manual mock registration")
        void shouldAllowManualMockRegistration() {
            MessageRepository mockRepo = org.mockito.Mockito.mock(MessageRepository.class);

            try (TestContext context = TestContext.Builder.create()
                    .withMock("mockRepository", mockRepo)
                    .build()) {
                MessageService service = context.getBean(MessageService.class);
                org.mockito.Mockito.when(mockRepo.getMessage("manual")).thenReturn("Manually Mocked");

                assertThat(service.getMessage("manual")).isEqualTo("Manually Mocked");
            }
        }
    }
}
