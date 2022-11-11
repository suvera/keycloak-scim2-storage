package dev.suvera.scim2.schema.builder;

import com.google.common.collect.ImmutableSet;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.meta.MetaRecord;
import dev.suvera.scim2.schema.data.misc.ListResponse;
import dev.suvera.scim2.schema.data.resource.ResourceType;

import static dev.suvera.scim2.schema.ScimConstant.*;

/**
 * author: suvera
 * date: 10/19/2020 1:04 PM
 */
public class DefaultResourceTypes {
    private ResourceType init() {
        ResourceType rt = new ResourceType();
        rt.setSchemas(ImmutableSet.of(URN_RESOURCE_TYPE));
        return rt;
    }

    public ListResponse<ResourceType> resourceTypes() {
        ListResponse<ResourceType> list = new ListResponse<>();
        list.setSchemas(ImmutableSet.of(ScimConstant.URN_LIST_RESPONSE));
        list.setTotalResults(5);
        list.setItemsPerPage(5);
        list.setStartIndex(1);

        list.addResource(serviceProviderConfig());
        list.addResource(resourceType());
        list.addResource(schema());
        list.addResource(user());
        list.addResource(group());

        return list;
    }

    public ResourceType resourceType() {
        ResourceType rt = init();
        rt.setSchema(URN_RESOURCE_TYPE);

        rt.setId(NAME_RESOURCETYPE);
        rt.setName(NAME_RESOURCETYPE);

        rt.setDescription("Resource Type");

        rt.setEndPoint(PATH_RESOURCETYPES);

        rt.setMeta(new MetaRecord(
                NAME_RESOURCETYPE,
                DEFAULT_SERVER + PATH_RESOURCETYPES + PATH_RESOURCETYPE
        ));

        return rt;
    }

    public ResourceType serviceProviderConfig() {
        ResourceType rt = init();
        rt.setSchema(URN_SP_CONFIG);

        rt.setId(NAME_SP_CONFIG);
        rt.setName(NAME_SP_CONFIG);

        rt.setDescription("Service Provider Configuration");

        rt.setEndPoint(PATH_SP);

        rt.setMeta(new MetaRecord(
                NAME_RESOURCETYPE,
                DEFAULT_SERVER + PATH_RESOURCETYPES + PATH_SP
        ));

        return rt;
    }

    public ResourceType schema() {
        ResourceType rt = init();
        rt.setSchema(URN_SCHEMA);

        rt.setId(NAME_SCHEMA);
        rt.setName(NAME_SCHEMA);

        rt.setDescription(NAME_SCHEMAS);

        rt.setEndPoint(PATH_SCHEMAS);

        rt.setMeta(new MetaRecord(
                NAME_RESOURCETYPE,
                DEFAULT_SERVER + PATH_RESOURCETYPES + PATH_SCHEMA
        ));

        return rt;
    }

    public ResourceType user() {
        ResourceType rt = init();
        rt.setSchema(URN_USER);

        rt.setId(NAME_USER);
        rt.setName(NAME_USER);

        rt.setDescription(NAME_USER + "s");

        rt.setEndPoint(PATH_USERS);

        rt.setMeta(new MetaRecord(
                NAME_RESOURCETYPE,
                DEFAULT_SERVER + PATH_RESOURCETYPES + PATH_USER
        ));

        return rt;
    }

    public ResourceType group() {
        ResourceType rt = init();
        rt.setSchema(URN_GROUP);

        rt.setId(NAME_GROUP);
        rt.setName(NAME_GROUP);

        rt.setDescription(NAME_GROUP + "s");

        rt.setEndPoint(PATH_GROUPS);

        rt.setMeta(new MetaRecord(
                NAME_RESOURCETYPE,
                DEFAULT_SERVER + PATH_RESOURCETYPES + PATH_GROUP
        ));

        return rt;
    }
}
