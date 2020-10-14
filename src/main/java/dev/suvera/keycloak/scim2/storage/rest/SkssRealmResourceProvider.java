package dev.suvera.keycloak.scim2.storage.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * author: suvera
 * date: 10/14/2020 12:42 PM
 */
public class SkssRealmResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public SkssRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    public Object getResource() {
        return new SkssRestResource(session);
    }

    public void close() {
    }
}
