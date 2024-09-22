package dev.suvera.keycloak.scim2.storage.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

/**
 * author: suvera
 * date: 10/14/2020 12:32 PM
 */
public class SkssEntityProvider implements JpaEntityProvider {
    public static final String FACTORY_ID = "skss-entity-provider";

    public List<Class<?>> getEntities() {
        return Collections.singletonList(SkssJobQueue.class);
    }

    public String getChangelogLocation() {
        return "META-INF/skss-changelog.xml";
    }

    public void close() {
    }

    public String getFactoryId() {
        return FACTORY_ID;
    }
}
