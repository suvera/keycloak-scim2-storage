package dev.suvera.keycloak.scim2.storage.jpa;

import org.keycloak.Config.Scope;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * author: suvera
 * date: 10/14/2020 12:37 PM
 */
public class SkssJpaEntityProviderFactory implements JpaEntityProviderFactory {
    protected static final String ID = "skss-entity-provider";

    public JpaEntityProvider create(KeycloakSession session) {
        return new SkssEntityProvider();
    }

    public String getId() {
        return ID;
    }

    public void init(Scope config) {
    }

    public void postInit(KeycloakSessionFactory factory) {
    }

    public void close() {
    }
}
