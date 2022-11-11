package dev.suvera.scim2.schema.data.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.suvera.scim2.schema.data.schema.Schema;
import lombok.Data;

/**
 * author: suvera
 * date: 10/17/2020 10:20 AM
 */
@Data
public class SchemaExt {
    private String schema;
    private boolean required;

    @JsonIgnore
    private Schema schemaObject;

}
