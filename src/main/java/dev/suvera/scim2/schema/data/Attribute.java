package dev.suvera.scim2.schema.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;
import dev.suvera.scim2.schema.enums.AttributeType;
import dev.suvera.scim2.schema.enums.Mutability;
import dev.suvera.scim2.schema.enums.ReturnedType;
import dev.suvera.scim2.schema.enums.UniquenessType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * author: suvera
 * date: 10/16/2020 11:14 PM
 */
@SuppressWarnings("unused")
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Attribute {
    @NotBlank(message = "name cannot be empty")
    private String name;
    private String description;
    @NotNull(message = "type cannot be empty")
    private AttributeType type = AttributeType.STRING;
    private boolean caseExact = false;
    private boolean multiValued = false;
    private Mutability mutability = Mutability.READ_WRITE;
    private boolean required = false;
    private ReturnedType returned = ReturnedType.DEFAULT;
    private UniquenessType uniqueness = UniquenessType.NONE;
    private Set<Object> canonicalValues = new HashSet<>();
    private Set<String> referenceTypes = new HashSet<>();

    private Set<Attribute> subAttributes = new HashSet<>();

    public static Attribute of(String name) {
        return new Attribute().setName(name);
    }

    public static Attribute ofReadOnlyAlways(String name) {
        return new Attribute().setName(name)
                .setMutability(Mutability.READ_ONLY)
                .setReturned(ReturnedType.ALWAYS);
    }

    public static Attribute of(String name, AttributeType type) {
        return new Attribute().setName(name).setType(type);
    }

    public Attribute setName(String name) {
        this.name = name;
        return this;
    }

    public Attribute setDescription(String description) {
        this.description = description;
        return this;
    }

    public Attribute setType(AttributeType type) {
        this.type = type;
        return this;
    }

    public Attribute setType(String type) {
        return setType(AttributeType.fromString(type));
    }

    public Attribute setCaseExact(boolean caseExact) {
        this.caseExact = caseExact;
        return this;
    }

    public Attribute setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
        return this;
    }

    public Attribute setMutability(Mutability mutability) {
        this.mutability = mutability;
        return this;
    }

    public Attribute setMutability(String type) {
        return setMutability(Mutability.fromString(type));
    }

    public Attribute setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public Attribute setReturned(ReturnedType returned) {
        this.returned = returned;
        return this;
    }

    public Attribute setReturned(String type) {
        return setReturned(ReturnedType.fromString(type));
    }

    public Attribute setUniqueness(UniquenessType uniqueness) {
        this.uniqueness = uniqueness;
        return this;
    }

    public Attribute setUniqueness(String type) {
        return setUniqueness(UniquenessType.fromString(type));
    }

    public Attribute setSubAttributes(Set<Attribute> subAttributes) {
        this.subAttributes = subAttributes;
        return this;
    }

    public Attribute addSubAttribute(Attribute subAttribute) {
        subAttributes.add(subAttribute);
        return this;
    }

    public Attribute setCanonicalValues(Collection<Object> canonicalValues) {
        if (canonicalValues != null) {
            this.canonicalValues.addAll(canonicalValues);
        } else {
            this.canonicalValues.clear();
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        return Objects.equal(name, attribute.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
