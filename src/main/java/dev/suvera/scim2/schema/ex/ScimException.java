package dev.suvera.scim2.schema.ex;

import dev.suvera.scim2.schema.data.ErrorRecord;

/**
 * author: suvera
 * date: 10/17/2020 10:36 AM
 */
@SuppressWarnings("unused")
public class ScimException extends Exception {
    private ErrorRecord error;

    public ScimException(ErrorRecord error) {
        this(error.toString());
        this.error = error;
    }

    public ScimException(String message) {
        super(message);
    }

    public ScimException(String message, ErrorRecord error) {
        this((error != null) ? message + ". Scim Error: " + error.toString() : message);
    }

    public ScimException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScimException(Exception e) {
        super(e.getMessage(), e);
    }

    public ErrorRecord getError() {
        return error;
    }
}
