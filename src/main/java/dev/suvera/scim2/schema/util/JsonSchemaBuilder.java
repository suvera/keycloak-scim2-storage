package dev.suvera.scim2.schema.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.suvera.scim2.schema.data.Attribute;
import dev.suvera.scim2.schema.data.schema.Schema;
import dev.suvera.scim2.schema.data.schema.SchemaExtension;

import java.util.*;

/**
 * author: suvera
 * date: 9/6/2020 4:10 PM
 */
@SuppressWarnings("unused")
public class JsonSchemaBuilder {
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private final Schema scimSchema;
    private final Collection<SchemaExtension> extensions;

    public JsonSchemaBuilder(Schema scimSchema, Collection<SchemaExtension> extensions) {
        this.scimSchema = scimSchema;
        this.extensions = extensions;
    }

    public String build() {
        Xmap root = Xmap.q();
        root.k("$schema", "http://json-schema.org/draft-04/schema#")
                .k("title", scimSchema.getName())
                .k("description", scimSchema.getDescription())
                .k("type", "object")
                .k("additionalItems", false)
        ;

        buildProperties(root);

        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root.get());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String buildList() {
        Xmap root = Xmap.q();

        Xmap wrapper = Xmap.q();
        wrapper.k("$schema", "http://json-schema.org/draft-04/schema#")
                .k("title", scimSchema.getName())
                .k("description", scimSchema.getDescription())
                .k("type", "object")
                .k("additionalItems", false)
        ;

        Set<String> allUrnIds = new HashSet<>();
        allUrnIds.add(scimSchema.getId());
        if (extensions != null) {
            for (SchemaExtension extension : extensions) {
                allUrnIds.add(extension.getSchema().getId());
            }
        }

        Xmap props = Xmap.q();
        props.k("schemas", Xmap.q()
                .k("type", "array")
                .k("items", Xmap.q()
                        .k("type", "string")
                        .k("enum", allUrnIds)
                        .get()
                )
                .get()
        );
        props.k("totalResults", Xmap.q()
                .k("type", "integer")
                .get()
        );
        props.k("startIndex", Xmap.q()
                .k("type", "integer")
                .get()
        );
        props.k("itemsPerPage", Xmap.q()
                .k("type", "integer")
                .get()
        );
        props.k("Resources", Xmap.q()
                .k("type", "array")
                .k("minItems", 0)
                .k("items", root.get())
                .get()
        );

        root.k("type", "object");
        wrapper.k("properties", props.get());

        buildProperties(root);

        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper.get());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void buildProperties(Xmap propNode) {
        Set<String> allUrnIds = new HashSet<>();
        allUrnIds.add(scimSchema.getId());
        if (extensions != null) {
            for (SchemaExtension extension : extensions) {
                allUrnIds.add(extension.getSchema().getId());
            }
        }

        Xmap props = Xmap.q();
        props.k("schemas", Xmap.q()
                .k("type", "array")
                .k("items", Xmap.q()
                        .k("type", "string")
                        .k("enum", allUrnIds)
                        .get()
                )
                .get()
        );
        props.k("id", Xmap.q()
                .k("type", "string")
                .get()
        );
        props.k("externalId", Xmap.q()
                .k("type", "string")
                .get()
        );
        propNode.k("properties", props.get());

        for (Attribute attr : scimSchema.getAttributes()) {
            props.k(attr.getName(), buildAttribute(attr));
        }
        List<String> reqItems = buildRequiredAttrs(scimSchema.getAttributes());

        // Build JsonSchema for scim schema extensions
        if (extensions != null) {
            for (SchemaExtension extension : extensions) {
                if (extension.isRequired()) {
                    reqItems.add(extension.getSchema().getId());
                }

                Xmap extnProps = Xmap.q();
                Xmap extn = Xmap.q()
                        .k("type", "object")
                        .k("properties", extnProps.get());
                props.k(extension.getSchema().getId(), extn.get());
                for (Attribute extnAttr : extension.getSchema().getAttributes()) {
                    extnProps.k(extnAttr.getName(), buildAttribute(extnAttr));
                }
            }
        }
        if (!reqItems.isEmpty()) {
            propNode.k("required", reqItems);
        }
    }

    private Map<String, Object> buildAttribute(Attribute attr) {
        Xmap map = Xmap.q();

        switch (attr.getType()) {
            case COMPLEX:
                if (attr.isMultiValued()) {
                    map.k("type", "array");
                    map.k("minItems", 0);
                    map.k("items", buildArray(attr));
                } else {
                    map.k("type", "object");
                    map.k("properties", buildObject(attr));
                    List<String> reqItems = buildRequiredAttrs(attr.getSubAttributes());
                    if (!reqItems.isEmpty()) {
                        map.k("required", reqItems);
                    }
                }
                break;

            case BOOLEAN:
                map.k("type", "boolean");
                break;

            case DECIMAL:
                map.k("type", "number");
                break;

            case INTEGER:
                map.k("type", "integer");
                break;

            default:
                map.k("type", "string");
                break;
        }

        if (attr.getCanonicalValues() != null && !attr.getCanonicalValues().isEmpty()) {
            map.k("enum", attr.getCanonicalValues());
        }

        return map.get();
    }

    private Map<String, Object> buildObject(Attribute attr) {
        Xmap props = Xmap.q();

        if (attr.getSubAttributes() != null) {
            for (Attribute subAttr : attr.getSubAttributes()) {
                props.k(subAttr.getName(), buildAttribute(subAttr));
            }
        }

        return props.get();
    }

    private Map<String, Object> buildArray(Attribute attr) {
        Xmap map = Xmap.q();
        map.k("type", "object");

        Xmap props = Xmap.q();
        map.k("properties", props.get());

        if (attr.getSubAttributes() != null) {
            for (Attribute subAttr : attr.getSubAttributes()) {
                props.k(subAttr.getName(), buildAttribute(subAttr));
            }
        }

        List<String> reqItems = buildRequiredAttrs(attr.getSubAttributes());
        if (!reqItems.isEmpty()) {
            map.k("required", reqItems);
        }

        return map.get();
    }

    private List<String> buildRequiredAttrs(Collection<Attribute> attributes) {
        List<String> list = new ArrayList<>();

        if (attributes == null) {
            return list;
        }

        for (Attribute attr : attributes) {
            if (attr.isRequired()) {
                list.add(attr.getName());
            }
        }

        return list;
    }
}
