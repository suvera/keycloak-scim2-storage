package dev.suvera.scim2.schema.data.meta;

import dev.suvera.scim2.schema.data.Attribute;
import dev.suvera.scim2.schema.data.AttributeSet;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: suvera
 * date: 10/16/2020 11:12 PM
 */
@SuppressWarnings("unused")
@EqualsAndHashCode(callSuper = true)
@Data
public class MetaDefinition extends AttributeSet {
    private static final MetaDefinition instance = new MetaDefinition();

    private MetaDefinition() {
        addAttribute(
                Attribute.ofReadOnlyAlways("resourceType")
                        .setDescription("The resource Type")
        );

        addAttribute(
                Attribute.ofReadOnlyAlways("created")
                        .setDescription("Date and time the resource was created")
        );

        addAttribute(
                Attribute.ofReadOnlyAlways("lastModified")
                        .setDescription("Date and time the resource was last modified")
        );

        addAttribute(
                Attribute.ofReadOnlyAlways("location")
                        .setDescription("The location (URI) of the resource")
        );

        addAttribute(
                Attribute.ofReadOnlyAlways("version")
                        .setDescription("The version of the resource")
        );
    }

    public static MetaDefinition getInstance() {
        return instance;
    }
}
