package dev.suvera.keycloak.scim2.storage.rest;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * author: suvera
 * date: 10/14/2020 12:42 PM
 */
public class SkssRealmResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "skss";

    public String getId() {
        return ID;
    }

    public RealmResourceProvider create(KeycloakSession session) {
        return new SkssRealmResourceProvider(session);
    }

    public void init(Scope config) {
    }

    public void postInit(KeycloakSessionFactory factory) {
    }

    public void close() {
    }
}
