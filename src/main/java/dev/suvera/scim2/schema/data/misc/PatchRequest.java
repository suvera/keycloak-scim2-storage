package dev.suvera.scim2.schema.data.misc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.BaseRecord;
import dev.suvera.scim2.schema.enums.PatchOp;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * author: suvera
 * date: 10/17/2020 12:22 PM
 */
@SuppressWarnings("unused")
@Data
public class PatchRequest<T extends BaseRecord> {
    private Set<String> schemas = Collections.singleton(ScimConstant.URN_PATCH_OP);
    @JsonIgnore
    private final Class<T> recordType;

    public PatchRequest(Class<T> recordType) {
        this.recordType = recordType;
    }

    @JsonProperty("Operations")
    private List<PatchOperation> operations;

    @JsonIgnore
    public void addOperation(PatchOperation op) {
        if (operations == null) {
            operations = new ArrayList<>();
        }
        operations.add(op);
    }

    @JsonIgnore
    public void addOperation(PatchOp op, String path, Object value) {
        if (operations == null) {
            operations = new ArrayList<>();
        }
        operations.add(new PatchOperation(op, path, value));
    }
}
