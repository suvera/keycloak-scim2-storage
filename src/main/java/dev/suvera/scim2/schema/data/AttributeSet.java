package dev.suvera.scim2.schema.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * author: suvera
 * date: 10/16/2020 11:44 PM
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
@Data
public abstract class AttributeSet {
    protected Set<Attribute> attributes = new LinkedHashSet<>();

    @JsonIgnore
    public AttributeSet addAttribute(Attribute attribute) {
        attributes.add(attribute);
        return this;
    }

    @JsonIgnore
    public AttributeSet addAttributes(Collection<Attribute> attrs) {
        attributes.addAll(attrs);
        return this;
    }

}
