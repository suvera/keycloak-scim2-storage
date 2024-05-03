package dev.suvera.scim2.schema.data.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fge.jsonschema.main.JsonSchema;
import com.google.common.base.Objects;
import dev.suvera.scim2.schema.data.BaseRecord;
import dev.suvera.scim2.schema.data.schema.Schema;
import dev.suvera.scim2.schema.data.schema.SchemaExtension;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

/**
 * author: suvera
 * date: 10/17/2020 12:33 AM
 */
@Data
public class ResourceType extends BaseRecord {
    @NotBlank(message = "name cannot be empty")
    private String name;
    private String description;
    @NotBlank(message = "endPoint cannot be empty")
    @JsonProperty("endpoint")
    private String endPoint;
    @NotBlank(message = "schema cannot be empty")
    private String schema;
    private Set<SchemaExt> schemaExtensions = new HashSet<>();

    @JsonIgnore
    private Schema schemaObject;
    @JsonIgnore
    private Set<SchemaExtension> schemaExtensionObjects = new HashSet<>();
    @JsonIgnore
    private JsonSchema jsonSchema;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceType that = (ResourceType) o;
        return Objects.equal(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(schema);
    }
}
