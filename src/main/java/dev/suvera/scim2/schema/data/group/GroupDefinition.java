package dev.suvera.scim2.schema.data.group;

import dev.suvera.scim2.schema.data.Attribute;
import dev.suvera.scim2.schema.data.AttributeSet;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;

/**
 * author: suvera
 * date: 10/17/2020 12:46 AM
 */
@SuppressWarnings("unused")
@EqualsAndHashCode(callSuper = true)
@Data
public class GroupDefinition extends AttributeSet {
    private static final GroupDefinition instance = new GroupDefinition();

    public static GroupDefinition getInstance() {
        return instance;
    }

    private GroupDefinition() {
        addAttribute(Attribute.of("displayName")
                .setDescription("A human-readable name for the Group. REQUIRED.")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readWrite")
                .setRequired(true)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("members")
                .setDescription("A list of members of the Group.")
                .setType("complex")
                .setCaseExact(false)
                .setMultiValued(true)
                .setMutability("readWrite")
                .setRequired(false)
                .setReturned("default")
                .setUniqueness("none")
                .addSubAttribute(Attribute.of("type")
                        .setDescription("A label indicating the type of resource, e.g., 'User' or 'Group'.")
                        .setType("string")
                        .setCaseExact(false)
                        .setMultiValued(false)
                        .setMutability("immutable")
                        .setRequired(false)
                        .setReturned("default")
                        .setUniqueness("none")
                        .setCanonicalValues(Arrays.asList("User", "Group"))
                )
                .addSubAttribute(Attribute.of("display")
                        .setDescription("Display name for the member")
                        .setType("string")
                        .setCaseExact(false)
                        .setMultiValued(false)
                        .setMutability("immutable")
                        .setRequired(false)
                        .setReturned("default")
                        .setUniqueness("none")
                )
                .addSubAttribute(Attribute.of("value")
                        .setDescription("Identifier of the member of this Group.")
                        .setType("string")
                        .setCaseExact(false)
                        .setMultiValued(false)
                        .setMutability("immutable")
                        .setRequired(false)
                        .setReturned("default")
                        .setUniqueness("none")
                )
                .addSubAttribute(Attribute.of("$ref")
                        .setDescription("The URI corresponding to a SCIM resource that is a member of this Group.")
                        .setType("reference")
                        .setCaseExact(false)
                        .setMultiValued(false)
                        .setMutability("immutable")
                        .setRequired(false)
                        .setReturned("default")
                        .setUniqueness("none")
                )
        );

    }
}
