package dev.suvera.scim2.client;

import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.BaseRecord;
import dev.suvera.scim2.schema.data.group.GroupRecord;
import dev.suvera.scim2.schema.data.misc.*;
import dev.suvera.scim2.schema.data.resource.ResourceType;
import dev.suvera.scim2.schema.data.schema.Schema;
import dev.suvera.scim2.schema.data.sp.SpConfig;
import dev.suvera.scim2.schema.data.user.UserRecord;
import dev.suvera.scim2.schema.ex.ScimException;

import java.util.Collection;

/**
 * author: suvera
 * date: 10/17/2020 11:36 AM
 */
@SuppressWarnings("unused")
public interface Scim2Client {

    <T extends BaseRecord> T create(T record, ResourceType resourceType) throws ScimException;

    <T extends BaseRecord> T read(String id, Class<T> cls, ResourceType resourceType) throws ScimException;

    <T extends BaseRecord> T replace(String id, T record, ResourceType resourceType) throws ScimException;

    void delete(String id, ResourceType resourceType) throws ScimException;

    <T extends BaseRecord> PatchResponse<T> patch(String id, PatchRequest<T> request, ResourceType resourceType)
            throws ScimException;

    <T extends BaseRecord> ListResponse<T> search(SearchRequest request, Class<T> cls, ResourceType resourceType)
            throws ScimException;

    <T extends BaseRecord> ListResponse<T> filter(String property, String value, Class<T> cls, ResourceType resourceType)
            throws ScimException;


    /**
     * Following operations performed at root Level
     */
    MixedListResponse search(SearchRequest request) throws ScimException;

    BulkResponse bulk(BulkRequest request) throws ScimException;


    /**
     * Core SCIM 2.0 data points
     */
    ResourceType getResourceType(String schemaId);

    Schema getSchema(String schemaId);

    SpConfig getSpConfig();

    Collection<ResourceType> getResourceTypes();

    Collection<Schema> getSchemas();

    /**
     * User operations
     */
    default UserRecord createUser(UserRecord record) throws ScimException {
        return create(record, getResourceType(ScimConstant.URN_USER));
    }

    default UserRecord readUser(String id) throws ScimException {
        return read(id, UserRecord.class, getResourceType(ScimConstant.URN_USER));
    }

    default UserRecord replaceUser(String id, UserRecord record) throws ScimException {
        return replace(id, record, getResourceType(ScimConstant.URN_USER));
    }

    default void deleteUser(String id) throws ScimException {
        delete(id, getResourceType(ScimConstant.URN_USER));
    }

    default PatchResponse<UserRecord> patchUser(String id, PatchRequest<UserRecord> request)
            throws ScimException {
        return patch(id, request, getResourceType(ScimConstant.URN_USER));
    }

    default ListResponse<UserRecord> searchUser(SearchRequest request) throws ScimException {
        return search(request, UserRecord.class, getResourceType(ScimConstant.URN_USER));
    }

    default ListResponse<UserRecord> filterUser(String property, String value) throws ScimException {
        return filter(property, value, UserRecord.class, getResourceType(ScimConstant.URN_USER));
    }

    /**
     * Group operations
     */
    default GroupRecord createGroup(GroupRecord record) throws ScimException {
        return create(record, getResourceType(ScimConstant.URN_GROUP));
    }

    default GroupRecord readGroup(String id) throws ScimException {
        return read(id, GroupRecord.class, getResourceType(ScimConstant.URN_GROUP));
    }

    default GroupRecord replaceGroup(String id, GroupRecord record) throws ScimException {
        return replace(id, record, getResourceType(ScimConstant.URN_GROUP));
    }

    default void deleteGroup(String id) throws ScimException {
        delete(id, getResourceType(ScimConstant.URN_GROUP));
    }

    default PatchResponse<GroupRecord> patchGroup(String id, PatchRequest<GroupRecord> request)
            throws ScimException {
        return patch(id, request, getResourceType(ScimConstant.URN_GROUP));
    }

    default ListResponse<GroupRecord> searchGroup(SearchRequest request) throws ScimException {
        return search(request, GroupRecord.class, getResourceType(ScimConstant.URN_GROUP));
    }

    default ListResponse<GroupRecord> filterGroup(String property, String value) throws ScimException {
        return filter(property, value, GroupRecord.class, getResourceType(ScimConstant.URN_GROUP));
    }
}
