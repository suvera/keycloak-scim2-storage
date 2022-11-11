package dev.suvera.scim2.schema.data.schema;

import com.google.common.base.Objects;
import lombok.Data;

/**
 * author: suvera
 * date: 10/17/2020 10:20 AM
 */
@Data
public class SchemaExtension {
    private final Schema parentSchema;
    private final boolean required;
    private final Schema schema;

    public SchemaExtension(
            Schema schema,
            Schema parentSchema,
            boolean required
    ) {
        this.parentSchema = parentSchema;
        this.required = required;
        this.schema = schema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaExtension that = (SchemaExtension) o;
        return Objects.equal(parentSchema, that.parentSchema) &&
                Objects.equal(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parentSchema, schema);
    }
}
