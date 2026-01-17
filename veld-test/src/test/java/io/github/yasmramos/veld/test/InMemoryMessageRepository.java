package io.github.yasmramos.veld.test;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * In-memory implementation of MessageRepository for testing.
 */
@Singleton
@Component
public class InMemoryMessageRepository implements MessageRepository {

    @Override
    public String getMessage(String key) {
        return "Message for: " + key;
    }

    @Override
    public void saveMessage(String key, String value) {
        // No-op for testing
    }
}
