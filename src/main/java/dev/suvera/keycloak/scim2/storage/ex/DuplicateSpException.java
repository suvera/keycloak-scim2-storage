package dev.suvera.keycloak.scim2.storage.ex;

/**
 * author: suvera
 * date: 10/14/2020 4:13 PM
 */
public class DuplicateSpException extends Exception {
    public DuplicateSpException(String message) {
        super(message);
    }

    public DuplicateSpException(String message, Throwable cause) {
        super(message, cause);
    }
}
