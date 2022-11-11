package dev.suvera.scim2.schema.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * author: suvera
 * date: 10/16/2020 11:16 PM
 */
@SuppressWarnings("unused")
public enum AttributeType {
    STRING, BOOLEAN, DECIMAL, INTEGER, DATETIME, BINARY, REFERENCE, COMPLEX;

    @JsonCreator
    public static AttributeType fromString(String key) {
        return key == null
                ? null
                : AttributeType.valueOf(key.toUpperCase());
    }

    @JsonValue
    public String getKey() {
        return name().toLowerCase();
    }
}
