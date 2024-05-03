package dev.suvera.keycloak.scim2.storage.ex;

public class SyncException extends Exception {
    public SyncException(String message) {
        super(message);
    }

    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public SyncException(String format, Object... args) {
        super(String.format(format, args));
    }
}
