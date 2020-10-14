package dev.suvera.keycloak.scim2.storage.spi.impl;

import dev.suvera.keycloak.scim2.storage.spi.SkssService;
import dev.suvera.keycloak.scim2.storage.spi.SkssServiceProviderFactory;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * author: suvera
 * date: 10/14/2020 1:06 PM
 */
public class SkssServiceProviderFactoryImpl implements SkssServiceProviderFactory {

    public SkssService create(KeycloakSession session) {
        return new SkssServiceImpl(session);
    }

    public void init(Scope config) {

    }

    public void postInit(KeycloakSessionFactory factory) {

    }

    public void close() {

    }

    public String getId() {
        return "SkssServiceProviderFactory";
    }
}
