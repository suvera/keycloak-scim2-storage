package dev.suvera.scim2.schema.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.BaseRecord;
import dev.suvera.scim2.schema.data.ErrorRecord;
import dev.suvera.scim2.schema.data.ScimResponse;
import dev.suvera.scim2.schema.data.misc.BulkResponse;
import dev.suvera.scim2.schema.data.misc.ListResponse;
import dev.suvera.scim2.schema.data.resource.ResourceType;
import dev.suvera.scim2.schema.data.resource.SchemaExt;
import dev.suvera.scim2.schema.data.schema.Schema;
import dev.suvera.scim2.schema.data.schema.SchemaExtension;
import dev.suvera.scim2.schema.data.sp.SpConfig;
import dev.suvera.scim2.schema.enums.ScimOperation;
import dev.suvera.scim2.schema.ex.ScimException;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * author: suvera
 * date: 10/18/2020 11:31 AM
 */
@SuppressWarnings({"unused"})
@Data
public class Scim2Protocol {
    private final static Logger log = LogManager.getLogger(Scim2Protocol.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator beanValidator;
    private static final boolean DEBUG = false;

    private SpConfig sp;
    private final Map<String, Schema> schemas = new HashMap<>();
    private final Map<String, ResourceType> resourceTypes = new HashMap<>();

    public Scim2Protocol(
            ScimResponse spResponse,
            ScimResponse resourceTypesResponse,
            ScimResponse schemasResponse
    ) throws ScimException {
        beanValidator = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()
                .getValidator();

        if (DEBUG) {
            log.info("spResponse {}", spResponse);
            log.info("resourceTypesResponse {}", resourceTypesResponse);
            log.info("schemasResponse {}", schemasResponse);
        }

        buildSpConfig(spResponse);
        buildResourceTypes(resourceTypesResponse);
        buildSchemas(schemasResponse);

        postProcess();
    }

    public Schema getSchema(String schemaId) {
        return schemas.get(schemaId);
    }

    public ResourceType getResourceType(String schemaId) {
        return resourceTypes.get(schemaId);
    }

    private void buildSpConfig(ScimResponse response) throws ScimException {
        String resource = "ServiceProviderConfig";
        basicResponseCheck(resource, response, ImmutableSet.of(200));

        try {
            sp = objectMapper.readValue(response.getBody(), SpConfig.class);
        } catch (JsonProcessingException e) {
            throw new ScimException("Could not parse response for " + resource, e);
        }

        verifySchemasInResponse(
                resource,
                sp.getSchemas(),
                ImmutableSet.of(ScimConstant.URN_SP_CONFIG),
                false
        );

        String error = beanValidator.validate(sp)
                .stream()
                .map(ConstraintViolation::getMessage)
                .reduce("", String::concat);

        if (error != null && !error.isEmpty()) {
            throw new ScimException("Service response for " + resource + " has invalid data values. " + error);
        }
    }

    private void buildSchemas(ScimResponse response) throws ScimException {
        String resource = "Schemas";
        basicResponseCheck(resource, response, ImmutableSet.of(200));

        ListResponse<Schema> listResponse;
        try {
            JavaType type = objectMapper.getTypeFactory().
                    constructParametricType(ListResponse.class, Schema.class);

            listResponse = objectMapper.readValue(response.getBody(), type);
        } catch (JsonProcessingException e) {
            throw new ScimException("Could not parse response for " + resource, e);
        }

        verifySchemasInResponse(
                resource,
                listResponse.getSchemas(),
                ImmutableSet.of(ScimConstant.URN_LIST_RESPONSE),
                true
        );
        if (listResponse.getResources() == null || listResponse.getResources().isEmpty()) {
            throw new ScimException("Could not find Resources in ListResponse for " + resource);
        }

        for (Schema schema : listResponse.getResources()) {
            verifySchemasInResponse(
                    resource,
                    schema.getSchemas(),
                    ImmutableSet.of(ScimConstant.URN_SCHEMA),
                    true
            );

            String error = beanValidator.validate(schema)
                    .stream()
                    .map(ConstraintViolation::getMessage)
                    .reduce("", String::concat);

            if (error != null && !error.isEmpty()) {
                throw new ScimException("Service response for " + resource + " has invalid data values. "
                        + error);
            }

            schemas.put(schema.getId(), schema);
        }
    }

    private void buildResourceTypes(ScimResponse response) throws ScimException {
        String resource = "ResourceTypes";
        basicResponseCheck(resource, response, ImmutableSet.of(200));

        ListResponse<ResourceType> listResponse;
        try {
            JavaType type = objectMapper.getTypeFactory().
                    constructParametricType(ListResponse.class, ResourceType.class);

            listResponse = objectMapper.readValue(response.getBody(), type);
        } catch (JsonProcessingException e) {
            throw new ScimException("Could not parse response for " + resource, e);
        }

        verifySchemasInResponse(
                resource,
                listResponse.getSchemas(),
                ImmutableSet.of(ScimConstant.URN_LIST_RESPONSE),
                true
        );
        if (listResponse.getResources() == null || listResponse.getResources().isEmpty()) {
            throw new ScimException("Could not find Resources in ListResponse for " + resource);
        }

        for (ResourceType resourceType : listResponse.getResources()) {
            verifySchemasInResponse(
                    resource,
                    resourceType.getSchemas(),
                    ImmutableSet.of(ScimConstant.URN_RESOURCE_TYPE),
                    true
            );

            String error = beanValidator.validate(resourceType)
                    .stream()
                    .map(ConstraintViolation::getMessage)
                    .reduce("", String::concat);

            if (error != null && !error.isEmpty()) {
                throw new ScimException("Service response for " + resource + " has invalid data values. "
                        + error);
            }

            resourceTypes.put(resourceType.getSchema(), resourceType);
        }
    }

    private void postProcess() throws ScimException {

        for (String defSchemaId : ScimConstant.SCIM_DEFAULT_SCHEMAS) {
            if (!schemas.containsKey(defSchemaId)) {
                throw new ScimException("Could not find default Schema for " + defSchemaId);
            }
        }

        for (String schemaId : resourceTypes.keySet()) {
            ResourceType resourceType = resourceTypes.get(schemaId);

            if (!schemas.containsKey(schemaId)) {
                throw new ScimException("Could not find Schema with id " + schemaId
                        + ", but mentioned in ResourceType " + resourceType.getName());
            }

            resourceType.setSchemaObject(schemas.get(schemaId));

            if (resourceType.getSchemaExtensions() != null) {
                Set<SchemaExtension> schemaExtObjects = new HashSet<>();

                for (SchemaExt schemaExt : resourceType.getSchemaExtensions()) {
                    if (!schemas.containsKey(schemaExt.getSchema())) {
                        throw new ScimException("Could not find Schema with id " + schemaId
                                + ", but mentioned in ResourceType.schemaExtensions "
                                + resourceType.getName());
                    }
                    schemaExtObjects.add(new SchemaExtension(
                            schemas.get(schemaExt.getSchema()),
                            schemas.get(schemaId),
                            schemaExt.isRequired()
                    ));
                }
                resourceType.setSchemaExtensionObjects(schemaExtObjects);
            }

            JsonSchemaBuilder builder = new JsonSchemaBuilder(
                    resourceType.getSchemaObject(),
                    resourceType.getSchemaExtensionObjects()
            );

            try {
                JsonNode sNode = JsonLoader.fromString(builder.build());
                if (DEBUG) {
                    log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sNode));
                }
                resourceType.setJsonSchema(JsonSchemaFactory.byDefault().getJsonSchema(sNode));
            } catch (Exception e) {
                throw new ScimException("JSON Schema generation failed", e);
            }

        } // for loop


    }

    public <T extends BaseRecord> void validateResponse(
            ScimOperation operation,
            ScimResponse response,
            ResourceType resourceType,
            Class<T> cls
    ) throws ScimException {

        switch (operation) {
            case DELETE:
                validateDeleteResponse(response, resourceType);
                break;

            case READ:
                validateReadResponse(response, resourceType);
                break;

            case SEARCH:
                validateSearchResponse(response, resourceType, cls);
                break;

            case CREATE:
                validateCreateResponse(response, resourceType);
                break;

            case PATCH:
                validateUpdateResponse(response, resourceType);
                break;

            case REPLACE:
                validateReplaceResponse(response, resourceType);
                break;

            case BULK:
                validateBulkResponse(response);
                break;
        }
    }

    private void validateDeleteResponse(
            ScimResponse response,
            ResourceType resourceType
    ) throws ScimException {
        if (response == null) {
            throw new ScimException("Empty HTTP Response received while Reading a resource "
                    + resourceType.getName());
        }
        if (!(response.getCode() == 204 || response.getCode() == 404)) {
            throw new ScimException("Unexpected http code received "
                    + response.getCode()
                    + " while deleting a resource of type " + resourceType.getName()
                    + " expected value is one of [204, 404]"
                    , buildErrorBody(response.getBody()));
        }
    }

    private void validateReadResponse(
            ScimResponse response,
            ResourceType resourceType
    ) throws ScimException {

        if (response == null) {
            throw new ScimException("Empty HTTP Response received while Reading a resource "
                    + resourceType.getName());
        }
        if (!(response.getCode() == 200)) {
            throw new ScimException("Unexpected http code received "
                    + response.getCode()
                    + " while deleting a resource of type " + resourceType.getName()
                    + " expected value is one of [204, 404]"
                    , buildErrorBody(response.getBody()));
        }

        try {
            JsonSchemaUtil.validate(response.getBody(), resourceType.getJsonSchema());
        } catch (Exception e) {
            throw new ScimException("Resource data object read for "
                    + resourceType.getName() + " is not following Schema.", e);
        }
    }

    private void validateCreateResponse(
            ScimResponse response,
            ResourceType resourceType
    ) throws ScimException {
        if (response == null) {
            throw new ScimException("Empty HTTP Response received while Reading a resource "
                    + resourceType.getName());
        }
        if (!(response.getCode() == 201)) {
            throw new ScimException("Unexpected http code received "
                    + response.getCode()
                    + " while creating a resource of type " + resourceType.getName()
                    + " expected value is one of [201]"
                    , buildErrorBody(response.getBody()));
        }

        try {
            JsonSchemaUtil.validate(response.getBody(), resourceType.getJsonSchema());
        } catch (Exception e) {
            throw new ScimException("Resource data object created for "
                    + resourceType.getName() + " is not following Schema.", e);
        }
    }

    private void validateReplaceResponse(
            ScimResponse response,
            ResourceType resourceType
    ) throws ScimException {
        if (response == null) {
            throw new ScimException("Empty HTTP Response received while Updating a resource "
                    + resourceType.getName());
        }
        if (!(response.getCode() == 200)) {
            throw new ScimException("Unexpected http code received "
                    + response.getCode()
                    + " while updating a resource of type " + resourceType.getName()
                    + " expected value is one of [200]"
                    , buildErrorBody(response.getBody()));
        }

        try {
            JsonSchemaUtil.validate(response.getBody(), resourceType.getJsonSchema());
        } catch (Exception e) {
            throw new ScimException("Resource data object updated for "
                    + resourceType.getName() + " is not following Schema.", e);
        }
    }

    private void validateUpdateResponse(
            ScimResponse response,
            ResourceType resourceType
    ) throws ScimException {
        
        if (!sp.getPatch().getSupported()) {
            return;
        }

        if (response == null) {
            throw new ScimException("Empty HTTP Response received while Patching a resource "
                    + resourceType.getName());
        }
        // Patching resource may return few params in object response, so not checking with full schema.
    }


    private <T extends BaseRecord> void validateSearchResponse(
            ScimResponse response,
            ResourceType resourceType,
            Class<T> cls
    ) throws ScimException {
        String resource = "Search";
        basicResponseCheck(resource, response, ImmutableSet.of(200));

        if (resourceType == null) {
            return;
        }

        ListResponse<T> listResponse;
        try {
            JavaType type = objectMapper.getTypeFactory().
                    constructParametricType(ListResponse.class, cls);

            listResponse = objectMapper.readValue(response.getBody(), type);
        } catch (JsonProcessingException e) {
            throw new ScimException("Could not parse response for " + resource, e);
        }

        verifySchemasInResponse(
                resource,
                listResponse.getSchemas(),
                ImmutableSet.of(ScimConstant.URN_LIST_RESPONSE),
                true
        );
    }

    private void validateBulkResponse(ScimResponse response) throws ScimException {
        if (!sp.getBulk().getSupported()) {
            return;
        }

        String resource = "Bulk";
        basicResponseCheck(resource, response, ImmutableSet.of(200));

        BulkResponse bulkResponse;
        try {
            bulkResponse = objectMapper.readValue(response.getBody(), BulkResponse.class);
        } catch (JsonProcessingException e) {
            throw new ScimException("Could not parse response for " + resource, e);
        }

        verifySchemasInResponse(
                resource,
                bulkResponse.getSchemas(),
                ImmutableSet.of(ScimConstant.URN_BULK_RESPONSE),
                true
        );
    }

    private void basicResponseCheck(
            String resource,
            ScimResponse response,
            Set<Integer> successCodes
    ) throws ScimException {
        if (response == null) {
            throw new ScimException("Empty HTTP Response for " + resource);
        }

        String contentType = response.header(ScimConstant.CONTENT_TYPE.toLowerCase());
        if (contentType != null) {
            String[] parts = StringUtils.split(contentType, ';');
            contentType = parts[0].trim();
        }
        if (contentType == null || !ScimConstant.SCIM_CONTENT_TYPES.contains(contentType)) {
            throw new ScimException("Invalid Response for " + resource + ", header "
                    + ScimConstant.CONTENT_TYPE + " is not received. expected value is one of ["
                    + Joiner.on(",").join(ScimConstant.SCIM_CONTENT_TYPES) + "]");
        }

        if (successCodes != null) {
            if (!successCodes.contains(response.getCode())) {
                throw new ScimException("Unexpected http code received "
                        + response.getCode() + " for " + resource + ", expected value is one of ["
                        + Joiner.on(",").join(successCodes) + "]", buildErrorBody(response.getBody()));
            }
        }
    }

    private void verifySchemasInResponse(
            String resource,
            Set<String> actual,
            Set<String> allowed,
            boolean strict
    ) throws ScimException {
        if (actual == null) {
            if (strict) {
                throw new ScimException("Empty schemas attribute found for " + resource
                        + ", Expected value is one of ["
                        + Joiner.on(",").join(allowed) + "]");
            }
            return;
        }

        for (String val : actual) {
            if (!allowed.contains(val)) {
                throw new ScimException("Unexpected schema value "
                        + val + " found for " + resource + ", allowed value is one of ["
                        + Joiner.on(",").join(allowed) + "]");
            }
        }
    }

    public static ErrorRecord buildErrorBody(String body) {
        try {
            return objectMapper.readValue(body, ErrorRecord.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
