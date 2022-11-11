package dev.suvera.scim2.schema.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import dev.suvera.scim2.schema.ex.ScimException;

import java.io.IOException;

/**
 * author: suvera
 * date: 10/17/2020 10:46 AM
 */
@SuppressWarnings("unused")
public class JsonSchemaUtil {

    public static void validate(String data, String schema) throws ScimException {
        try {
            final JsonNode dataNode = JsonLoader.fromString(data);
            final JsonNode schemaNode = JsonLoader.fromString(schema);

            validate(dataNode, schemaNode);
        } catch (IOException e) {
            throw new ScimException("JSON data validation failed", e);
        }
    }

    public static void validate(JsonNode dataNode, JsonNode schemaNode) throws ScimException {
        try {
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(schemaNode);

            validate(dataNode, schema);
        } catch (ProcessingException e) {
            throw new ScimException("JSON data validation failed", e);
        }
    }

    public static void validate(JsonNode dataNode, JsonSchema schema) throws ScimException {
        try {
            ProcessingReport report = schema.validate(dataNode);

            if (!report.isSuccess()) {
                throw new ScimException("JSON data validation failed with report " + report.toString());
            }
        } catch (ProcessingException e) {
            throw new ScimException("JSON data validation failed", e);
        }
    }

    public static void validate(String data, JsonSchema schema) throws ScimException {
        try {
            final JsonNode dataNode = JsonLoader.fromString(data);

            validate(dataNode, schema);
        } catch (IOException e) {
            throw new ScimException("JSON data validation failed", e);
        }
    }

    public static void validate(String data, JsonNode schemaNode) throws ScimException {
        try {
            final JsonNode dataNode = JsonLoader.fromString(data);

            validate(dataNode, schemaNode);
        } catch (IOException e) {
            throw new ScimException("JSON data validation failed", e);
        }
    }

    public static void validate(JsonNode dataNode, String schema) throws ScimException {
        try {
            final JsonNode schemaNode = JsonLoader.fromString(schema);

            validate(dataNode, schemaNode);
        } catch (IOException e) {
            throw new ScimException("JSON data validation failed", e);
        }
    }
}
