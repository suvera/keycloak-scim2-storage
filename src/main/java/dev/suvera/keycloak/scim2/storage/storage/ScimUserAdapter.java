package dev.suvera.keycloak.scim2.storage.storage;

import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

public class ScimUserAdapter extends AbstractUserAdapterFederatedStorage {

    private static final String EXTERNAL_ID_ATTRIBUTE = "EXTERNAL_ID";
    private UserModel localUser;

    public ScimUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel, UserModel localUser) {
        super(session, realm, storageProviderModel);
        this.localUser = localUser;
    }

    @Override
    public String getUsername() {
        return localUser.getUsername();
    }

    @Override
    public void setUsername(String username) {
        localUser.setUsername(username);
    }

    public void setExternalId(String externalId) {
        setSingleAttribute(EXTERNAL_ID_ATTRIBUTE, externalId);
    }

    public String getExternalId() {
        return getFirstAttribute(EXTERNAL_ID_ATTRIBUTE);
    }

    public void removeExternalId() {
        removeAttribute(EXTERNAL_ID_ATTRIBUTE);
    }

    public UserModel getLocalUserModel() {
        return localUser;
    }

    public Stream<ScimGroupAdapter> getScimGroupsStream() {
        return getGroupsStream().map(g -> new ScimGroupAdapter(session, g, realm.getId(), storageProviderModel.getId()));
    }
}
