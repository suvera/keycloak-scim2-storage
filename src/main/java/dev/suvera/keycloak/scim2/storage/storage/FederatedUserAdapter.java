package dev.suvera.keycloak.scim2.storage.storage;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import dev.suvera.keycloak.scim2.storage.jpa.FederatedUserEntity;

public class FederatedUserAdapter extends AbstractUserAdapterFederatedStorage {
    private static final Logger logger = Logger.getLogger(FederatedUserAdapter.class);
    protected FederatedUserEntity entity;
    protected String keycloakId;

    public FederatedUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, FederatedUserEntity entity) {
        super(session, realm, model);
        this.entity = entity;
        keycloakId = StorageId.keycloakId(model, entity.getId());
    }

    public String getExternalId() {
        return entity.getExternalId();
    }

    public void setExternalId(String externalId) {
        entity.setExternalId(externalId);
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        entity.setUsername(username);

    }

    @Override
    public void setEmail(String email) {
        entity.setEmail(email);
    }

    @Override
    public String getEmail() {
        return entity.getEmail();
    }

    @Override
    public String getId() {
        return keycloakId;
    }
}