package dev.suvera.scim2.schema.data.misc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.suvera.scim2.schema.data.BaseRecord;
import lombok.Data;

/**
 * author: suvera
 * date: 10/17/2020 12:36 PM
 */
@Data
public class PatchResponse<T extends BaseRecord> {
    // 200-OK, 204-No Content
    private int status;

    private T resource;

    @JsonIgnore
    private final Class<T> recordType;

    public PatchResponse(Class<T> recordType) {
        this.recordType = recordType;
    }
}
