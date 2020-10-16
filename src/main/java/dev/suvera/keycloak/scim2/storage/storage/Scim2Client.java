package dev.suvera.keycloak.scim2.storage.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unboundid.scim2.client.ScimService;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.types.*;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * author: suvera
 * date: 10/15/2020 11:25 AM
 */
@SuppressWarnings("FieldCanBeLocal")
public class Scim2Client {
    private static final Logger log = Logger.getLogger(Scim2Client.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ComponentModel componentModel;
    private ScimService scimService = null;
    private ServiceProviderConfigResource spConfig;
    private ResourceTypeResource userResource;
    private ResourceTypeResource groupResource;

    public Scim2Client(ComponentModel componentModel) {
        this.componentModel = componentModel;

        String endPoint = componentModel.get("endPoint");
        String username = componentModel.get("username");
        String password = componentModel.get("password");

        log.info("SCIM 2.0 endPoint: " + endPoint);

        SSLContext sslcontext;
        try {
            sslcontext = SSLContext.getInstance("TLS");

            sslcontext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new java.security.SecureRandom());
        } catch (Exception e) {
            log.error("", e);
            return;
        }

        Client client = ClientBuilder.newBuilder()
                .sslContext(sslcontext)
                .hostnameVerifier((s1, s2) -> true)
                .register((ClientRequestFilter) requestContext -> {
                    if (username == null || username.isEmpty()) {
                        return;
                    }

                    log.info("SCIM 2.0 username: ****** , password: ******");
                    String token = username + ":" + password;
                    token = "Basic " + DatatypeConverter.printBase64Binary(token.getBytes(StandardCharsets.UTF_8));

                    MultivaluedMap<String, Object> headers = requestContext.getHeaders();
                    headers.add("Authorization", token);
                }).build();


        WebTarget target = client.target(endPoint);
        this.scimService = new ScimService(target);
    }

    public void validate() throws ScimException {
        if (scimService == null) {
            throw new ScimException(0, "Scim service is not working", null);
        }

        try {
            spConfig = scimService.getServiceProviderConfig();
            ListResponse<ResourceTypeResource> resourceTypes = scimService.getResourceTypes();

            for (ResourceTypeResource resource : resourceTypes.getResources()) {

                log.info("Schema: " + resource.getSchema().toString());

                if (resource.getSchema().toString().equals("urn:ietf:params:scim:schemas:core:2.0:User")) {
                    this.userResource = resource;
                } else if (resource.getSchema().toString().equals("urn:ietf:params:scim:schemas:core:2.0:Group")) {
                    this.groupResource = resource;
                }
            }
        } catch (ScimException e) {
            log.error("", e);
            scimService = null;
            throw e;
        }
    }

    private void buildScimUser(SkssUserModel userModel, SkssUserResource user) {
        user.setUserName(userModel.getUsername());
        //user.setPassword();

        Name name = new Name()
                .setGivenName(userModel.getFirstName() == null ? userModel.getUsername()
                        : userModel.getFirstName())
                .setFamilyName(userModel.getLastName());

        if (isAttributeNotNull(userModel, "honorificPrefix")) {
            name.setHonorificPrefix(userModel.getFirstAttribute("honorificPrefix"));
        }
        if (isAttributeNotNull(userModel, "honorificSuffix")) {
            name.setHonorificSuffix(userModel.getFirstAttribute("honorificSuffix"));
        }
        user.setName(name);

        if (userModel.getEmail() != null) {
            Email email = new Email()
                    .setType("work")
                    .setPrimary(true)
                    .setValue(userModel.getEmail());
            user.setEmails(Collections.singletonList(email));
        } else {
            user.setEmails(Collections.emptyList());
        }

        user.setExternalId(userModel.getId());
        user.setActive(userModel.isEnabled());

        List<Group> groups = new ArrayList<>();
        for (GroupModel groupModel : userModel.getGroups()) {
            try {
                createGroup(groupModel);
            } catch (ScimException e) {
                log.error("", e);
            }

            Group grp = new Group();
            grp.setDisplay(groupModel.getName());
            grp.setValue(groupModel.getId());
            grp.setType("direct");

            groups.add(grp);
        }
        user.setGroups(groups);

        List<Role> roles = new ArrayList<>();
        for (RoleModel roleModel : userModel.getRoleMappings()) {
            Role role = new Role();
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
            List<Address> addresses = new ArrayList<>();
            try {
                Address addr = objectMapper.readValue(
                        userModel.getFirstAttribute("addresses_primary"),
                        Address.class
                );
                if (addr.getPrimary() == null) {
                    addr.setPrimary(false);
                }
                addresses.add(addr);
            } catch (JsonProcessingException e) {
                log.error("", e);
            }

            user.setAddresses(addresses);
        } else {
            user.setAddresses(Collections.emptyList());
        }

        if (isAttributeNotNull(userModel, "phoneNumbers_primary")) {
            List<PhoneNumber> phones = new ArrayList<>();
            try {
                PhoneNumber phone = objectMapper.readValue(
                        userModel.getFirstAttribute("phoneNumbers_primary"),
                        PhoneNumber.class
                );
                if (phone.getPrimary() == null) {
                    phone.setPrimary(false);
                }
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

        SkssUserResource user = new SkssUserResource();
        buildScimUser(userModel, user);

        user = scimService.create(userResource.getEndpoint().getPath(), user);

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

        SkssUserResource user = getUser(userModel);
        if (user == null) {
            return;
        }

        buildScimUser(userModel, user);

        user = scimService.replace(user);

        userModel.saveExternalUserId(
                componentModel.getId(),
                user.getId()
        );
    }

    public SkssUserResource getUser(SkssUserModel userModel) throws ScimException {
        String id = userModel.getExternalUserId(componentModel.getId());

        if (id == null) {
            log.info("User user does not exist in the SCIM2 provider " + userModel.getUsername());
            return null;
        }

        return scimService.retrieve(userResource.getEndpoint().getPath(), id, SkssUserResource.class);
    }

    public void deleteUser(String skssId) throws ScimException {
        if (scimService == null) {
            return;
        }
        scimService.delete(userResource.getEndpoint().getPath(), skssId);
    }

    public void createGroup(GroupModel groupModel) throws ScimException {
        if (scimService == null) {
            return;
        }

        if (groupModel.getFirstAttribute("skss_id_" + componentModel.getId()) != null) {
            return;
        }

        GroupResource grp = new GroupResource();
        grp.setDisplayName(groupModel.getName());

        grp = scimService.create(groupResource.getEndpoint().getPath(), grp);

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

        GroupResource grp = scimService.retrieve(groupResource.getEndpoint().getPath(), id, GroupResource.class);

        grp = scimService.replace(grp);
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
        scimService.delete(groupResource.getEndpoint().getPath(), id);
    }
}
