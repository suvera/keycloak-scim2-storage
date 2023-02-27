package dev.suvera.keycloak.scim2.storage.ldap;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapperFactory;

public class LDAPEventMapperFactory implements LDAPStorageMapperFactory<LDAPEventMapper> {
    public static final String PROVIDER_ID = "ldap-to-scim-event-mapper";

    @Override
    public LDAPEventMapper create(KeycloakSession session, ComponentModel model) {
        return new LDAPEventMapper(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
}
