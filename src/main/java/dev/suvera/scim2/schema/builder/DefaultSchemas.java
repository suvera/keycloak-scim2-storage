package dev.suvera.scim2.schema.builder;

import com.google.common.collect.ImmutableSet;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.group.GroupDefinition;
import dev.suvera.scim2.schema.data.meta.MetaRecord;
import dev.suvera.scim2.schema.data.misc.ListResponse;
import dev.suvera.scim2.schema.data.resource.ResourceTypeDefinition;
import dev.suvera.scim2.schema.data.schema.Schema;
import dev.suvera.scim2.schema.data.schema.SchemaDefinition;
import dev.suvera.scim2.schema.data.sp.SpConfigDefinition;
import dev.suvera.scim2.schema.data.user.EnterpriseUserDefinition;
import dev.suvera.scim2.schema.data.user.UserDefinition;

import java.util.Date;

import static dev.suvera.scim2.schema.ScimConstant.*;

/**
 * author: suvera
 * date: 10/19/2020 1:04 PM
 */
public class DefaultSchemas {
    private Schema init(String id) {

        Schema rt = new Schema();
        rt.setId(id);
        rt.setSchemas(ImmutableSet.of(URN_SCHEMA));

        rt.setMeta(new MetaRecord(
                NAME_SCHEMA,
                DEFAULT_SERVER + PATH_SCHEMAS + id,
                new Date(1603095350000L),
                new Date(1603095350000L),
                "W/\"123\""
        ));
        return rt;
    }

    public ListResponse<Schema> schemas() {
        ListResponse<Schema> list = new ListResponse<>();
        list.setSchemas(ImmutableSet.of(ScimConstant.URN_LIST_RESPONSE));
        list.setTotalResults(6);
        list.setItemsPerPage(10);
        list.setStartIndex(1);

        list.addResource(serviceProviderConfig());
        list.addResource(resourceType());
        list.addResource(schema());
        list.addResource(user());
        list.addResource(enterpriseUser());
        list.addResource(group());

        return list;
    }

    public Schema resourceType() {
        Schema rt = init(URN_RESOURCE_TYPE);
        rt.setName(NAME_RESOURCETYPE);
        rt.setDescription("Resource Type");

        rt.setAttributes(ResourceTypeDefinition.getInstance().getAttributes());
        return rt;
    }

    public Schema serviceProviderConfig() {
        Schema rt = init(URN_SP_CONFIG);

        rt.setName(NAME_SP_CONFIG);
        rt.setDescription("Service Provider Configuration");

        rt.setAttributes(SpConfigDefinition.getInstance().getAttributes());

        return rt;
    }

    public Schema schema() {
        Schema rt = init(URN_SCHEMA);

        rt.setName(NAME_SCHEMA);
        rt.setDescription(NAME_SCHEMAS);

        rt.setAttributes(SchemaDefinition.getInstance().getAttributes());

        return rt;
    }

    public Schema user() {
        Schema rt = init(URN_USER);

        rt.setName(NAME_USER);
        rt.setDescription(NAME_USER + "s");

        rt.setAttributes(UserDefinition.getInstance().getAttributes());

        return rt;
    }

    public Schema enterpriseUser() {
        Schema rt = init(URN_ENTERPRISE_USER);

        rt.setName("EnterpriseUser");
        rt.setDescription("Enterprise Users");

        rt.setAttributes(EnterpriseUserDefinition.getInstance().getAttributes());

        return rt;
    }

    public Schema group() {
        Schema rt = init(URN_GROUP);

        rt.setName(NAME_GROUP);
        rt.setDescription(NAME_GROUP + "s");

        rt.setAttributes(GroupDefinition.getInstance().getAttributes());

        return rt;
    }

}
