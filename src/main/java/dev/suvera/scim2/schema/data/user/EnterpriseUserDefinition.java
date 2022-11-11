package dev.suvera.scim2.schema.data.user;

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
public class EnterpriseUserDefinition extends AttributeSet {
    private static final EnterpriseUserDefinition instance = new EnterpriseUserDefinition();

    public static EnterpriseUserDefinition getInstance() {
        return instance;
    }

    private EnterpriseUserDefinition() {
        addAttribute(Attribute.of("employeeNumber")
                .setDescription("Numeric or alphanumeric identifier assigned to a person, typically based on order of hire or association with an organization.")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readWrite")
                .setRequired(false)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("costCenter")
                .setDescription("Identifies the name of a cost center.")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readWrite")
                .setRequired(false)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("organization")
                .setDescription("Identifies the name of an organization.")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readWrite")
                .setRequired(false)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("division")
                .setDescription("Identifies the name of a division.")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readWrite")
                .setRequired(false)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("department")
                .setDescription("Identifies the name of a department.")
                .setType("string")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readWrite")
                .setRequired(false)
                .setReturned("default")
                .setUniqueness("none")
        );

        addAttribute(Attribute.of("manager")
                .setDescription("The User's manager.  A complex type that optionally allows service providers to represent organizational hierarchy by referencing the 'id' attribute of another User.")
                .setType("complex")
                .setCaseExact(false)
                .setMultiValued(false)
                .setMutability("readWrite")
                .setRequired(false)
                .setReturned("default")
                .setUniqueness("none")
                .addSubAttribute(Attribute.of("value")
                        .setDescription("The id of the SCIM resource representing the User's manager. REQUIRED.")
                        .setType("string")
                        .setCaseExact(false)
                        .setMultiValued(false)
                        .setMutability("readWrite")
                        .setRequired(true)
                        .setReturned("default")
                        .setUniqueness("none")
                )
                .addSubAttribute(Attribute.of("$ref")
                        .setDescription("The URI of the SCIM resource representing the User's manager.")
                        .setType("reference")
                        .setCaseExact(false)
                        .setMultiValued(false)
                        .setMutability("readWrite")
                        .setRequired(false)
                        .setReturned("default")
                        .setUniqueness("none")
                )
                .addSubAttribute(Attribute.of("displayName")
                        .setDescription("The displayName of the User's manager.")
                        .setType("string")
                        .setCaseExact(false)
                        .setMultiValued(false)
                        .setMutability("readOnly")
                        .setRequired(false)
                        .setReturned("default")
                        .setUniqueness("none")
                )
        );

    }
}
