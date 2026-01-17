package io.github.yasmramos.veld.test.context;

import io.github.yasmramos.veld.test.mock.MockFactory;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lightweight test container that manages the lifecycle
 * of Veld beans for testing purposes.
 *
 * <p>This class provides a wrapper around the Veld container
 * with additional functionalities specific to testing:</p>
 * <ul>
 *   <li>Registering mocks before container startup</li>
 *   <li>Lifecycle management (start/stop)</li>
 *   <li>Bean injection in test fields</li>
 *   <li>Access to registered beans and mocks</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * TestContext context = TestContextBuilder.create()
 *     .withProfile("test")
 *     .withMock("myMock", mockObject)
 *     .build();
 *
 * try {
 *     MyService service = context.getBean(MyService.class);
 *     // Run tests...
 * } finally {
 *     context.close();
 * }
 * }</pre>
 *
 * NOTE: This class needs to be updated to work with the new Veld API
 * where beans are accessed via generated static methods instead of Veld.get().
 *
 * @author Veld Framework
 * @since 1.0.0
 */
public final class TestContext implements AutoCloseable {

    private final Map<Class<?>, Object> mocks;
    private final Map<String, Object> namedMocks;
    private final String activeProfile;
    private boolean closed = false;

    private TestContext(Map<Class<?>, Object> mocks, Map<String, Object> namedMocks, String profile) {
        this.mocks = mocks;
        this.namedMocks = namedMocks;
        this.activeProfile = profile;
    }

    /**
     * Get a bean from the test context.
     * NOTE: This method needs to be updated for the new Veld API.
     *
     * @param <T> bean type
     * @param type bean class
     * @return bean of the specified type
     * @throws IllegalStateException if the context is closed
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        checkOpen();

        // Check if we have a mock for this type
        if (mocks.containsKey(type)) {
            return (T) mocks.get(type);
        }

        // Check if we have a named mock that can be cast
        for (Object mock : namedMocks.values()) {
            if (type.isInstance(mock)) {
                return (T) mock;
            }
        }

        // For the new Veld API, beans are accessed via generated static methods
        // This method needs to be updated to use the new API
        throw new UnsupportedOperationException(
            "getBean() requires update for new Veld API. " +
            "Use generated static methods like Veld.myService_123456789() instead."
        );
    }

    public void close() {
        closed = true;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("TestContext is closed");
        }
    }

    // Builder class for creating TestContext instances
    public static final class Builder {
        private Map<Class<?>, Object> mocks = new HashMap<>();
        private Map<String, Object> namedMocks = new HashMap<>();
        private String profile = "test";

        public static Builder create() {
            return new Builder();
        }

        public Builder withProfile(String profile) {
            this.profile = profile;
            return this;
        }

        public <T> Builder withMock(Class<T> type, T mock) {
            this.mocks.put(type, mock);
            return this;
        }

        public <T> Builder withNamedMock(String name, T mock) {
            this.namedMocks.put(name, mock);
            return this;
        }

        public TestContext build() {
            return new TestContext(mocks, namedMocks, profile);
        }
    }
}
