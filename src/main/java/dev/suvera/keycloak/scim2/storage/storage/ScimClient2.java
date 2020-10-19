package dev.suvera.keycloak.scim2.storage.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import dev.suvera.scim2.client.Scim2Client;
import dev.suvera.scim2.client.Scim2ClientBuilder;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.group.GroupRecord;
import dev.suvera.scim2.schema.data.user.UserRecord;
import dev.suvera.scim2.schema.ex.ScimException;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * author: suvera
 * date: 10/15/2020 11:25 AM
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ScimClient2 {
    private static final Logger log = Logger.getLogger(ScimClient2.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ComponentModel componentModel;
    private Scim2Client scimService = null;
    private ScimException scimException = null;

    public ScimClient2(ComponentModel componentModel) {
        this.componentModel = componentModel;

        String endPoint = componentModel.get("endPoint");
        String username = componentModel.get("username");
        String password = componentModel.get("password");

        log.info("SCIM 2.0 endPoint: " + endPoint);

        Scim2ClientBuilder builder = new Scim2ClientBuilder(endPoint)
                .usernamePassword(username, password)
                .allowSelfSigned(true);

        try {
            scimService = builder.build();
        } catch (ScimException e) {
            scimException = e;
        }
    }

    public void validate() throws ScimException {
        if (scimException != null) {
            throw scimException;
        }
    }

    private void buildScimUser(SkssUserModel userModel, UserRecord user) {
        user.setUserName(userModel.getUsername());
        //user.setPassword();

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
        for (GroupModel groupModel : userModel.getGroups()) {
            try {
                createGroup(groupModel);
            } catch (ScimException e) {
                log.error("", e);
            }

            UserRecord.UserGroup grp = new UserRecord.UserGroup();
            grp.setDisplay(groupModel.getName());
            grp.setValue(groupModel.getId());
            grp.setType("direct");

            groups.add(grp);
        }
        user.setGroups(groups);

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
                        UserRecord.UserAddress.class
                );
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
                        UserRecord.UserPhoneNumber.class
                );
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
    }

    private boolean isAttributeNotNull(SkssUserModel userModel, String name) {
        String val = userModel.getFirstAttribute(name);

        return !(val == null || val.isEmpty() || val.equals("null"));
    }

    private String strVal(String val) {
        return (val == null ? "" : val);
    }

    public void createUser(SkssUserModel userModel) throws ScimException {
        if (scimService == null) {
            return;
        }
        if (userModel.getExternalUserId(componentModel.getId()) != null) {
            log.info("User already exist in the SCIM2 provider " + userModel.getUsername());
            return;
        }

        UserRecord user = new UserRecord();
        buildScimUser(userModel, user);

        user = scimService.createUser(user);

        userModel.saveExternalUserId(
                componentModel.getId(),
                user.getId()
        );
        log.info("User record successfully sync'd to SKIM service provider. " + user.getId());
    }

    public void updateUser(SkssUserModel userModel) throws ScimException {
        if (scimService == null) {
            return;
        }
        String id = userModel.getExternalUserId(componentModel.getId());

        if (id == null) {
            log.info("User user does not exist in the SCIM2 provider " + userModel.getUsername());
            return;
        }

        UserRecord user = getUser(userModel);
        if (user == null) {
            return;
        }

        buildScimUser(userModel, user);

        user = scimService.replaceUser(id, user);

        userModel.saveExternalUserId(
                componentModel.getId(),
                user.getId()
        );
    }

    public UserRecord getUser(SkssUserModel userModel) throws ScimException {
        String id = userModel.getExternalUserId(componentModel.getId());

        if (id == null) {
            log.info("User user does not exist in the SCIM2 provider " + userModel.getUsername());
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

    public void createGroup(GroupModel groupModel) throws ScimException {
        if (scimService == null) {
            return;
        }

        if (groupModel.getFirstAttribute("skss_id_" + componentModel.getId()) != null) {
            return;
        }

        GroupRecord grp = new GroupRecord();
        grp.setDisplayName(groupModel.getName());

        grp = scimService.createGroup(grp);

        groupModel.setSingleAttribute(
                "skss_id_" + componentModel.getId(),
                grp.getId()
        );
    }

    public void updateGroup(GroupModel groupModel) throws ScimException {
        if (scimService == null) {
            return;
        }

        String id = groupModel.getFirstAttribute("skss_id_" + componentModel.getId());

        if (id == null) {
            log.info("User user does not exist in the SCIM2 provider " + groupModel.getName());
            return;
        }

        GroupRecord grp = scimService.readGroup(id);

        grp = scimService.replaceGroup(id, grp);
        grp.setDisplayName(groupModel.getName());

        groupModel.setSingleAttribute(
                "skss_id_" + componentModel.getId(),
                grp.getId()
        );
    }

    public void deleteGroup(GroupModel groupModel) throws ScimException {
        if (scimService == null) {
            return;
        }

        String id = groupModel.getFirstAttribute("skss_id_" + componentModel.getId());
        if (id != null) {
            scimService.deleteGroup(id);
        }
    }
}
