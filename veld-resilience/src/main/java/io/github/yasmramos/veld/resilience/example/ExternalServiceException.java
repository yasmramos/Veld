package io.github.yasmramos.veld.resilience.example;

public class ExternalServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class TransientException extends ExternalServiceException {

        private static final long serialVersionUID = 1L;

        public TransientException(String message) {
            super(message);
        }

        public TransientException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class PermanentException extends ExternalServiceException {

        private static final long serialVersionUID = 1L;

        public PermanentException(String message) {
            super(message);
        }

        public PermanentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
