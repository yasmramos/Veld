package io.github.yasmramos.veld.test;

/**
 * Repository interface for message operations.
 */
public interface MessageRepository {

    String getMessage(String key);

    void saveMessage(String key, String value);
}
