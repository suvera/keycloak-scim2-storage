package dev.suvera.keycloak.scim2.storage.jpa;

import java.util.Arrays;
import java.util.List;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

/**
 * author: suvera
 * date: 10/14/2020 12:32 PM
 */
public class SkssEntityProvider implements JpaEntityProvider {

    public List<Class<?>> getEntities() {
        return Arrays.asList(ScimSyncJobQueue.class, FederatedGroupAttributeEntity.class);
    }

    public String getChangelogLocation() {
        return "META-INF/skss-changelog.xml";
    }

    public void close() {
    }

    public String getFactoryId() {
        return SkssJpaEntityProviderFactory.ID;
    }
}
