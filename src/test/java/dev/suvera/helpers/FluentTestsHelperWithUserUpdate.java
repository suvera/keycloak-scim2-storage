package dev.suvera.helpers;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.FluentTestsHelper;

public class FluentTestsHelperWithUserUpdate extends FluentTestsHelper {
    public FluentTestsHelperWithUserUpdate(String keycloakBaseUrl, String adminUserName, String adminPassword, String adminRealm, String adminClient, String testRealm) {
        super(keycloakBaseUrl, adminUserName, adminPassword, adminRealm, adminClient, testRealm);
    }

    @Override
    public FluentTestsHelperWithUserUpdate init() {
        super.init();
        return this;
    }

    public FluentTestsHelperWithUserUpdate updateUserEmail(String userName, String email) {
        assert isInitialized;
        UserRepresentation userRepresentation = keycloak.realms().realm(testRealm).users().search(userName).get(0);
        userRepresentation.setEmail(email);
        keycloak.realms().realm(testRealm).users().get(userRepresentation.getId()).update(userRepresentation);
        return this;
    }
}
