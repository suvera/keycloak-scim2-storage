package dev.suvera.scim2.schema.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * author: suvera
 * date: 10/17/2020 12:28 PM
 */
@SuppressWarnings("unused")
public enum PatchOp {
    ADD, REMOVE, REPLACE;

    @JsonCreator
    public static PatchOp fromString(String key) {
        return key == null
                ? null
                : PatchOp.valueOf(key.toUpperCase());
    }

    @JsonValue
    public String getKey() {
        return name().toLowerCase();
    }
}
