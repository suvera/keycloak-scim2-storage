package dev.suvera.keycloak.scim2.storage.storage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.Session;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.user.SynchronizationResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;

import dev.suvera.scim2.client.Scim2Client;
import dev.suvera.scim2.client.Scim2ClientBuilder;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.group.GroupRecord;
import dev.suvera.scim2.schema.data.group.GroupRecord.GroupManager;
import dev.suvera.scim2.schema.data.group.GroupRecord.GroupMember;
import dev.suvera.scim2.schema.data.group.GroupRecord.GroupSubstituteUser;
import dev.suvera.scim2.schema.data.misc.ListResponse;
import dev.suvera.scim2.schema.data.misc.PatchRequest;
import dev.suvera.scim2.schema.data.misc.PatchResponse;
import dev.suvera.scim2.schema.data.user.UserRecord;
import dev.suvera.scim2.schema.data.user.UserRecord.UserAddress;
import dev.suvera.scim2.schema.data.user.UserRecord.UserEmail;
import dev.suvera.scim2.schema.data.user.UserRecord.UserName;
import dev.suvera.scim2.schema.data.user.UserRecord.UserPhoneNumber;
import dev.suvera.scim2.schema.enums.PatchOp;
import dev.suvera.scim2.schema.ex.ScimException;
import liquibase.pro.packaged.nu;

/**
 * author: suvera
 * date: 10/15/2020 11:25 AM
 */
@SuppressWarnings({ "FieldCanBeLocal", "unused" })
public class ScimClient2 {
    private static final Logger log = Logger.getLogger(ScimClient2.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ComponentModel componentModel;
    private Scim2Client scimService = null;
    private ScimException scimException = null;

    public ScimClient2(ComponentModel componentModel) {
        this.componentModel = componentModel;

        String endPoint = componentModel.get("endPoint");
        String authorityUrl = componentModel.get("authorityUrl");
        String username = componentModel.get("username");
        String password = componentModel.get("password");
        String clientId = componentModel.get("clientId");
        String clientSecret = componentModel.get("clientSecret");

        log.info("SCIM 2.0 endPoint: " + endPoint);
        endPoint = StringUtils.stripEnd(endPoint, " /");

        String resourceTypesJson = null;
        String schemasJson = null;

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream isResourceTypes = classLoader.getResourceAsStream("ResourceTypes.json");
        if (isResourceTypes == null) {
            log.error("file not found! ResourceTypes.json");
            throw new IllegalArgumentException("file not found! ResourceTypes.json");
        } else {
            resourceTypesJson = inputStreamToString(isResourceTypes);
            resourceTypesJson = resourceTypesJson.replaceAll("\\{SCIM_BASE}", endPoint);
        }

        InputStream isSchemas = classLoader.getResourceAsStream("Schemas.json");
        if (isSchemas == null) {
            log.error("file not found! Schemas.json");
            throw new IllegalArgumentException("file not found! Schemas.json");
        } else {
            schemasJson = inputStreamToString(isSchemas);
            schemasJson = schemasJson.replaceAll("\\{SCIM_BASE}", endPoint);
        }

        Scim2ClientBuilder builder = new Scim2ClientBuilder(endPoint)
                .allowSelfSigned(true)
                .resourceTypes(resourceTypesJson)
                .schemas(schemasJson)
                .clientSecret(authorityUrl, username, password, clientId, clientSecret);

        /*
         * if (bearerToken != null && !bearerToken.isEmpty()) {
         * builder.bearerToken(bearerToken);
         * } else {
         * builder.usernamePassword(username, password);
         * }
         */

        try {
            scimService = builder.build();
        } catch (ScimException e) {
            scimException = e;
        }
    }

    private String inputStreamToString(InputStream is) {
        return new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    public void validate() throws ScimException {
        if (scimException != null) {
            throw scimException;
        }
    }

    private UserRecord buildScimUser(ScimUserAdapter userAdapter) {
        UserModel userModel = userAdapter.getLocalUserModel();
        UserRecord user = new UserRecord();

        user.setUserName(userModel.getUsername());

        List<UserRecord.UserClaim> claims = new ArrayList<>();
        List<String> defaultClaims = Arrays.asList("firstName", "lastName", "username", "email");

        userModel.getAttributes().forEach((k, v) -> {
            UserRecord.UserClaim claim = new UserRecord.UserClaim();

            if (!defaultClaims.contains(k)) {
                claim.setAttributeKey(k);
                claim.setAttributeValue(v.get(0));

                claims.add(claim);
            }
        });

        user.setClaims(claims);

        UserRecord.UserName name = new UserRecord.UserName();
        name.setGivenName(userModel.getFirstName() == null ? userModel.getUsername()
                : userModel.getFirstName());
        name.setFamilyName(userModel.getLastName());
        user.setName(name);

        if (isAttributeNotNull(userModel, "honorificPrefix")) {
            name.setHonorificPrefix(userModel.getFirstAttribute("honorificPrefix"));
        }
        if (isAttributeNotNull(userModel, "honorificSuffix")) {
            name.setHonorificSuffix(userModel.getFirstAttribute("honorificSuffix"));
        }

        user.setName(name);

        if (userModel.getEmail() != null) {
            UserRecord.UserEmail email = new UserRecord.UserEmail();
            email.setType("work");
            email.setPrimary(true);
            email.setValue(userModel.getEmail());

            user.setEmails(Collections.singletonList(email));
        } else {
            user.setEmails(Collections.emptyList());
        }

        user.setSchemas(ImmutableSet.of(ScimConstant.URN_USER));
        user.setExternalId(userModel.getId());
        user.setActive(userModel.isEnabled());

        List<UserRecord.UserGroup> groups = new ArrayList<>();
        userAdapter.getScimGroupsStream().forEach(groupAdapter -> {
            try {
                createOrUpdateGroup(groupAdapter, List.of(), List.of());
            } catch (ScimException e) {
                log.error("", e);
            }

            UserRecord.UserGroup grp = new UserRecord.UserGroup();
            grp.setDisplay(groupAdapter.getGroupModel().getName());
            grp.setValue(groupAdapter.getGroupModel().getId());
            grp.setType("direct");

            groups.add(grp);
        });

        List<UserRecord.UserRole> roles = new ArrayList<>();
        for (RoleModel roleModel : userModel.getRoleMappings()) {
            UserRecord.UserRole role = new UserRecord.UserRole();
            role.setDisplay(roleModel.getName());
            role.setValue(roleModel.getId());
            role.setType("direct");
            role.setPrimary(false);

            roles.add(role);
        }
        user.setRoles(roles);

        if (isAttributeNotNull(userModel, "title")) {
            user.setTitle(userModel.getFirstAttribute("title"));
        }
        if (isAttributeNotNull(userModel, "displayName")) {
            user.setDisplayName(userModel.getFirstAttribute("displayName"));
        } else {
            user.setDisplayName((strVal(name.getGivenName()) + " " + strVal(name.getFamilyName())).trim());
        }

        if (isAttributeNotNull(userModel, "nickName")) {
            user.setNickName(userModel.getFirstAttribute("nickName"));
        }

        if (isAttributeNotNull(userModel, "addresses_primary")) {
            List<UserRecord.UserAddress> addresses = new ArrayList<>();
            try {
                UserRecord.UserAddress addr = objectMapper.readValue(
                        userModel.getFirstAttribute("addresses_primary"),
                        UserRecord.UserAddress.class);
                addresses.add(addr);
            } catch (JsonProcessingException e) {
                log.error("", e);
            }

            user.setAddresses(addresses);
        } else {
            user.setAddresses(Collections.emptyList());
        }

        if (isAttributeNotNull(userModel, "phoneNumbers_primary")) {
            List<UserRecord.UserPhoneNumber> phones = new ArrayList<>();
            try {
                UserRecord.UserPhoneNumber phone = objectMapper.readValue(
                        userModel.getFirstAttribute("phoneNumbers_primary"),
                        UserRecord.UserPhoneNumber.class);
                phones.add(phone);
            } catch (JsonProcessingException e) {
                log.error("", e);
            }

            user.setPhoneNumbers(phones);
        } else {
            user.setPhoneNumbers(Collections.emptyList());
        }

        user.setIms(Collections.emptyList());
        user.setPhotos(Collections.emptyList());
        user.setEntitlements(Collections.emptyList());
        user.setX509Certificates(Collections.emptyList());

        try {
            log.info("Scim User: " + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(user));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return user;
    }

    private boolean isAttributeNotNull(UserModel userModel, String name) {
        String val = userModel.getFirstAttribute(name);

        return !(val == null || val.isEmpty() || val.equals("null"));
    }

    private String strVal(String val) {
        return (val == null ? "" : val);
    }

    public void createUser(ScimUserAdapter userModel) throws ScimException {
        if (scimService == null) {
            return;
        }

        UserRecord scimUser = buildScimUser(userModel);
        UserRecord createdUser = scimService.createUser(scimUser);

        userModel.setExternalId(createdUser.getId());
        log.info("User record successfully sync'd to SKIM service provider. " + createdUser.getId());
    }

    public void createOrUpdateUser(ScimUserAdapter scimUser, SynchronizationResult result) throws ScimException {
        if (scimService == null) {
            return;
        }

        UserRecord user = null;
        try {
            user = findUserByUsername(scimUser.getUsername());
        } catch (ScimException e) {
            user = null;
        }

        if (user == null) {
            createUser(scimUser);
            if (result != null) {
                result.increaseAdded();
            }
        } else {
            updateUser(scimUser, user);
            if (result != null) {
                result.increaseUpdated();
            }
        }
    }

    public UserRecord findUserByUsername(String username) throws ScimException {
        if (scimService == null) {
            return null;
        }

        ListResponse<UserRecord> users = scimService.filterUser("userName", username);

        return users.getResources().stream().findFirst().orElse(null);
    }

    private void updateUser(ScimUserAdapter userModel, UserRecord originalUser) throws ScimException {
        UserRecord scimUser = buildScimUser(userModel);
        scimUser.setId(originalUser.getId());

        PatchRequest<UserRecord> patchRequest = UserRecordPatchBuilder.buildPatchRequest(scimUser, originalUser);

        PatchResponse<UserRecord> response = scimService.patchUser(scimUser.getId(), patchRequest);

        userModel.setExternalId(response.getResource().getId());
    }

    public void updateUser(ScimUserAdapter userModel) throws ScimException {
        if (scimService == null) {
            return;
        }

        UserRecord user = getUser(userModel);
        if (user == null) {
            return;
        }

        updateUser(userModel, user);
    }

    public UserRecord getUser(ScimUserAdapter userModel) throws ScimException {
        if (scimService == null) {
            return null;
        }

        String id = userModel.getExternalId();

        if (id == null) {
            log.infof("User with %s does not exist in the SCIM provider.", userModel.getUsername());
            return null;
        }

        return scimService.readUser(id);
    }

    public void deleteUser(String skssId) throws ScimException {
        if (scimService == null) {
            return;
        }
        if (skssId != null) {
            scimService.deleteUser(skssId);
        }
    }

    public void createOrUpdateGroup(ScimGroupAdapter scimGroup, List<String> groupManagersExternalIds,
            List<SubstituteUser> substituteUsers) throws ScimException {
        if (scimService == null) {
            return;
        }

        GroupRecord originalGroupRecord = null;
        try {
            originalGroupRecord = findGroupByGroupName(scimGroup.getGroupModel().getName());
        } catch (ScimException e) {
            originalGroupRecord = null;
        }

        if (originalGroupRecord == null) {
            createGroup(scimGroup);
        } else {
            if (scimGroup.getExternalId() == null) {
                // if there is no external id, just set it and do not replace group data
                scimGroup.setExternalId((originalGroupRecord.getId()));
                // TODO: maybe we need to patch group here
            } else {
                updateGroup(scimGroup, originalGroupRecord, groupManagersExternalIds, substituteUsers);
            }
        }
    }

    public GroupRecord findGroupByGroupName(String name) throws ScimException {
        if (scimService == null) {
            return null;
        }

        ListResponse<GroupRecord> users = scimService.filterGroup("displayName", name);

        return users.getResources().stream().findFirst().orElse(null);
    }

    public void createGroup(ScimGroupAdapter groupModel) throws ScimException {
        if (scimService == null) {
            return;
        }

        GroupRecord groupRecord = new GroupRecord();
        groupRecord.setDisplayName(groupModel.getGroupModel().getName());

        groupRecord = scimService.createGroup(groupRecord);

        groupModel.setExternalId(groupRecord.getId());
    }

    public String tryToSetExternalGroupIdFromOriginalGroup(String groupName, ScimGroupAdapter scimGroupAdapter) {
        String externalId = null;

        try {
            GroupRecord groupRecord = findGroupByGroupName(groupName);
            if (groupRecord != null) {
                externalId = groupRecord.getId();
                scimGroupAdapter.setExternalId(groupRecord.getId());
            }
        } catch (ScimException e) {
        }

        return externalId;
    }

    public String tryToSetExternalUserIdFromOriginalUser(String username, ScimUserAdapter userAdapter) {
        String externalId = null;

        try {
            UserRecord externalUserRecord = findUserByUsername(username);
            if (externalUserRecord != null) {
                externalId = externalUserRecord.getId();
                userAdapter.setExternalId(externalId);
            }
        } catch (ScimException e) {
        }

        return externalId;
    }

    private void updateGroupName(ScimGroupAdapter groupModel, GroupRecord groupRecord) throws ScimException {
        groupRecord.setDisplayName(groupModel.getGroupModel().getName());

        groupRecord = scimService.replaceGroup(groupRecord.getId(), groupRecord);

        groupModel.setExternalId(groupRecord.getId());
    }

    private void updateGroup(ScimGroupAdapter groupModel, GroupRecord originalGroupRecord,
            List<String> groupManagersExternalIds, List<SubstituteUser> substituteUsers) throws ScimException {
        String externalGroupId = groupModel.getExternalId();

        if (externalGroupId != null) {
            PatchRequest<GroupRecord> patchRequest = new PatchRequest<>(GroupRecord.class);
            addPatchGroupManagers(patchRequest, groupManagersExternalIds);
            addPatchSubstituteUsers(patchRequest, substituteUsers);

            PatchResponse<GroupRecord> response = scimService.patchGroup(externalGroupId, patchRequest);

            if (response.getStatus() >= 200 && response.getStatus() <= 299) {
                log.infof("Group %s patch request succedded with http status code %d.",
                        groupModel.getGroupModel().getName(), response.getStatus());
            } else {
                log.errorf("Group %s update failed with http status code %d.", groupModel.getGroupModel().getName(),
                        response.getStatus());
            }
        }
    }

    private void addPatchGroupManagers(PatchRequest<GroupRecord> patchRequest, List<String> groupManagersExternalIds) {
        List<GroupManager> groupManagers = groupManagersExternalIds
                .stream()
                .map(x -> {
                    GroupManager groupManager = new GroupManager();
                    groupManager.setValue(x);

                    return groupManager;
                })
                .collect(Collectors.toList());
        patchRequest.addOperation(PatchOp.REPLACE, ScimConstant.URN_ADINSURE_GROUP + ":groupManagers", groupManagers);
    }

    private void addPatchSubstituteUsers(PatchRequest<GroupRecord> patchRequest, List<SubstituteUser> substituteUsers) {
        List<GroupSubstituteUser> groupSubstituteUsers = substituteUsers
                .stream()
                .map(x -> {
                    GroupSubstituteUser groupSubstituteUser = new GroupSubstituteUser();
                    groupSubstituteUser.setValue("{\"UserId\":\"" + x.getUser() + "\",\"SubstituteUserId\":\""
                            + x.getSubstituteUser() + "\"}");

                    return groupSubstituteUser;
                })
                .collect(Collectors.toList());
        patchRequest.addOperation(PatchOp.REPLACE, ScimConstant.URN_ADINSURE_GROUP + ":substituteUsers",
                groupSubstituteUsers);
    }

    public void updateGroup(ScimGroupAdapter groupModel, List<String> groupManagersExternalIds,
            List<SubstituteUser> substituteUsers) throws ScimException {
        if (scimService == null) {
            return;
        }

        String externalId = groupModel.getExternalId();
        if (externalId == null || externalId.isEmpty()) {
            externalId = tryToSetExternalGroupIdFromOriginalGroup(groupModel.getGroupModel().getName(), groupModel);
        }

        if (externalId == null) {
            log.infof("Group %s does not exist in the SCIM2 provider", groupModel.getGroupModel().getName());
            return;
        }

        GroupRecord originalGroupRecord = scimService.readGroup(externalId);
        if (originalGroupRecord != null) {
            updateGroupName(groupModel, originalGroupRecord);
            updateGroup(groupModel, originalGroupRecord, groupManagersExternalIds, substituteUsers);
        }
    }

    public boolean joinGroup(ScimGroupAdapter groupModel, ScimUserAdapter userModel,
            List<String> groupManagersExternalIds, List<SubstituteUser> substituteUsers) throws ScimException {
        if (scimService == null) {
            return false;
        }

        String externalUserId = userModel.getExternalId();
        String externalGroupId = groupModel.getExternalId();

        if (externalUserId != null && externalGroupId != null) {
            PatchRequest<GroupRecord> patchRequest = new PatchRequest<>(GroupRecord.class);
            GroupMember groupMember = new GroupMember();
            groupMember.setDisplay(userModel.getUsername());
            groupMember.setValue(userModel.getExternalId());
            patchRequest.addOperation(PatchOp.ADD, "members", Arrays.asList(groupMember));
            addPatchGroupManagers(patchRequest, groupManagersExternalIds);
            addPatchSubstituteUsers(patchRequest, substituteUsers);

            PatchResponse<GroupRecord> response = scimService.patchGroup(externalGroupId, patchRequest);
            return response.getStatus() == 200;
        }

        return false;
    }

    public boolean leaveGroup(ScimGroupAdapter groupModel, ScimUserAdapter userModel,
            List<String> groupManagersExternalIds, List<SubstituteUser> substituteUsers) throws ScimException {
        if (scimService == null) {
            return false;
        }

        String externalUserId = userModel.getExternalId();
        String externalGroupId = groupModel.getExternalId();

        if (externalUserId != null && externalGroupId != null) {
            PatchRequest<GroupRecord> patchRequest = new PatchRequest<>(GroupRecord.class);

            patchRequest.addOperation(
                    PatchOp.REMOVE,
                    String.format("members[value eq \"%s\"]", externalUserId),
                    null);
            addPatchGroupManagers(patchRequest, groupManagersExternalIds);
            addPatchSubstituteUsers(patchRequest, substituteUsers);

            PatchResponse<GroupRecord> response = scimService.patchGroup(externalGroupId, patchRequest);
            return response.getStatus() == 200;
        }

        return false;
    }

    public void deleteGroup(String id) throws ScimException {
        if (scimService == null) {
            return;
        }

        if (id != null) {
            scimService.deleteGroup(id);
        }
    }
}
