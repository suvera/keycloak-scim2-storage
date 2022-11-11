package dev.suvera.scim2.schema.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * author: suvera
 * date: 10/16/2020 11:21 PM
 */
@SuppressWarnings("unused")
public enum Mutability {
    READ_ONLY("readOnly"),
    READ_WRITE("readWrite"),
    IMMUTABLE("immutable"),
    WRITE_ONLY("writeOnly");

    private final String key;

    Mutability(String key) {
        this.key = key;
    }

    @JsonCreator
    public static Mutability fromString(String key) {
        for (Mutability val : Mutability.values()) {
            if (val.getKey().equals(key)) {
                return val;
            }
        }

        return null;
    }

    @JsonValue
    public String getKey() {
        return key;
    }
}
