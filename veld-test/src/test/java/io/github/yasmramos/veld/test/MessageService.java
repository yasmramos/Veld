package io.github.yasmramos.veld.test;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Message service for integration testing.
 */
@Singleton
@Component
public class MessageService {

    private final MessageRepository repository;

    @Inject
    public MessageService(MessageRepository repository) {
        this.repository = repository;
    }

    public String getMessage(String key) {
        return repository.getMessage(key);
    }

    public void saveMessage(String key, String value) {
        repository.saveMessage(key, value);
    }
}
