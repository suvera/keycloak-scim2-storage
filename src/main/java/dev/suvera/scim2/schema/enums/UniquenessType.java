package dev.suvera.scim2.schema.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * author: suvera
 * date: 10/16/2020 11:21 PM
 */
@SuppressWarnings("unused")
public enum UniquenessType {
    NONE("none"),
    SERVER("server"),
    GLOBAL("global");

    private final String key;

    UniquenessType(String key) {
        this.key = key;
    }

    @JsonCreator
    public static UniquenessType fromString(String key) {
        for (UniquenessType val : UniquenessType.values()) {
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
