package dev.suvera.scim2.schema.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import dev.suvera.scim2.schema.ScimConstant;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * author: suvera
 * date: 10/17/2020 10:37 AM
 */
@Data
@NoArgsConstructor
public class ErrorRecord {
    private Set<String> schemas = ImmutableSet.of(ScimConstant.URN_ERROR);
    private String scimType;
    private String detail;
    private int status;

    public ErrorRecord(int status, String detail) {
        this.detail = detail;
        this.status = status;
    }

    @Override
    @JsonIgnore
    public String toString() {
        return "ScimError{" +
                ", scimType='" + scimType + '\'' +
                ", detail='" + detail + '\'' +
                ", status=" + status +
                '}';
    }
}
