package dev.suvera.scim2.schema.data.resource;

import dev.suvera.scim2.schema.data.Attribute;
import dev.suvera.scim2.schema.data.AttributeSet;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: suvera
 * date: 10/17/2020 12:46 AM
 */
@SuppressWarnings("unused")
@EqualsAndHashCode(callSuper = true)
@Data
public class ResourceTypeDefinition extends AttributeSet {
    private static final ResourceTypeDefinition instance = new ResourceTypeDefinition();

    public static ResourceTypeDefinition getInstance() {
        return instance;
    }

    private ResourceTypeDefinition() {
        addAttribute(Attribute.of("name")
                .setDescription("The resource type name. When applicable, service providers MUST specify the name, e.g., 'User'.")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readOnly")
                .setRequired(true)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("description")
                .setDescription("The resource type's human-readable description. When applicable, service providers MUST specify the description.")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readOnly")
                .setRequired(false)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("endpoint")
                .setDescription("The resource type's HTTP-addressable endpoint relative to the Base URL of the service provider, e.g., \"Users\".")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readOnly")
                .setRequired(true)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("schema")
                .setDescription("The resource type's primary/base schema URI.")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readOnly")
                .setRequired(true)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("schemaExtensions")
                .setDescription("A list of URIs of the resource type's schema extensions.")
                .setType("complex")
                .setCaseExact(false)
                .setMultiValued(true)
                .setMutability("readOnly")
                .setRequired(false)
                .setReturned("default")
                .setUniqueness("none")
                .addSubAttribute(Attribute.of("schema")
                        .setDescription("The URI of an extended schema, e.g., \"urn:edu:2.0:Staff\". This MUST be equal to the \"id\" attribute of a \"Schema\" resource.")
                        .setType("string")
                        .setCaseExact(false)
                        .setMultiValued(false)
                        .setMutability("readOnly")
                        .setRequired(true)
                        .setReturned("default")
                        .setUniqueness("none")
                )
                .addSubAttribute(Attribute.of("required")
                        .setDescription("A Boolean value that specifies whether or not the schema extension is required for the resource type. If true, a resource of this type MUST include this schema extension and also include any attributes declared as required in this schema extension. If false, a resource of this type MAY omit this schema extension.")
                        .setType("boolean")
                        .setCaseExact(false)
                        .setMultiValued(false)
                        .setMutability("readOnly")
                        .setRequired(true)
                        .setReturned("default")
                        .setUniqueness("none")
                )
        );

    }
}
