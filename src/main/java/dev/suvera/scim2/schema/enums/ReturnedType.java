package dev.suvera.scim2.schema.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * author: suvera
 * date: 10/16/2020 11:21 PM
 */
@SuppressWarnings("unused")
public enum ReturnedType {
    ALWAYS("always"),
    NEVER("never"),
    DEFAULT("default"),
    REQUEST("request");

    private final String key;

    ReturnedType(String key) {
        this.key = key;
    }

    @JsonCreator
    public static ReturnedType fromString(String key) {
        for (ReturnedType val : ReturnedType.values()) {
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
