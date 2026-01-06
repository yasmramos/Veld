package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EventRegistrationSPI to achieve coverage of default methods.
 */
@DisplayName("EventRegistrationSPI Tests")
class EventRegistrationSPITest {

    @Nested
    @DisplayName("Default getHandlerCount() Tests")
    class DefaultGetHandlerCountTests {

        @Test
        @DisplayName("Should return 0 for default implementation")
        void shouldReturnZeroForDefaultImplementation() {
            EventRegistrationSPI spi = new EventRegistrationSPI() {
                @Override
                public void registerEvents(EventBus bus, Object component) {
                    // No-op for testing
                }
            };

            assertEquals(0, spi.getHandlerCount());
        }

        @Test
        @DisplayName("Should return custom count when overridden")
        void shouldReturnCustomCountWhenOverridden() {
            EventRegistrationSPI spi = new EventRegistrationSPI() {
                @Override
                public void registerEvents(EventBus bus, Object component) {
                    // No-op for testing
                }

                @Override
                public int getHandlerCount() {
                    return 5;
                }
            };

            assertEquals(5, spi.getHandlerCount());
        }
    }

    @Nested
    @DisplayName("Interface Implementation Tests")
    class InterfaceImplementationTests {

        @Test
        @DisplayName("Should implement registerEvents without throwing")
        void shouldImplementRegisterEventsWithoutThrowing() {
            EventRegistrationSPI spi = new EventRegistrationSPI() {
                @Override
                public void registerEvents(EventBus bus, Object component) {
                    // Simple no-op implementation
                }
            };

            assertDoesNotThrow(() -> spi.registerEvents(EventBus.getInstance(), new Object()));
        }

        @Test
        @DisplayName("Should handle null bus gracefully in registerEvents")
        void shouldHandleNullBusGracefully() {
            EventRegistrationSPI spi = new EventRegistrationSPI() {
                @Override
                public void registerEvents(EventBus bus, Object component) {
                    // Should handle null bus without throwing
                    if (bus != null) {
                        bus.clear();
                    }
                }
            };

            assertDoesNotThrow(() -> spi.registerEvents(null, new Object()));
        }

        @Test
        @DisplayName("Should handle null component gracefully in registerEvents")
        void shouldHandleNullComponentGracefully() {
            EventBus bus = EventBus.getInstance();
            EventRegistrationSPI spi = new EventRegistrationSPI() {
                @Override
                public void registerEvents(EventBus eventBus, Object component) {
                    // Should handle null component without throwing
                    if (component != null) {
                        // Would register events here
                    }
                }
            };

            assertDoesNotThrow(() -> spi.registerEvents(bus, null));
        }
    }

    @Nested
    @DisplayName("Complete Implementation Tests")
    class CompleteImplementationTests {

        @Test
        @DisplayName("Should create complete SPI implementation")
        void shouldCreateCompleteSPIImplementation() {
            final int[] handlerCount = {0};

            EventRegistrationSPI spi = new EventRegistrationSPI() {
                @Override
                public void registerEvents(EventBus bus, Object component) {
                    handlerCount[0]++;
                }

                @Override
                public int getHandlerCount() {
                    return handlerCount[0];
                }
            };

            // Call registerEvents multiple times
            spi.registerEvents(EventBus.getInstance(), new Object());
            spi.registerEvents(EventBus.getInstance(), new Object());
            spi.registerEvents(EventBus.getInstance(), new Object());

            assertEquals(3, spi.getHandlerCount());
        }
    }
}
