package dev.suvera.keycloak.scim2.storage.storage;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ComponentEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import dev.suvera.keycloak.scim2.storage.ldap.LDAPEventMapperFactory;

public final class ComponentModelUtils {
    public static Stream<String> getLDAPComponentsWithScimEventsEnabled(KeycloakSessionFactory factory, String realmId) {
        return getComponents(factory, realmId, LDAPEventMapperFactory.PROVIDER_ID)
        .map(c -> c.getParentId());
    }

    public static Stream<ComponentEntity> getComponents(KeycloakSessionFactory factory, String realmId, String providerId) {
        AtomicReference<List<ComponentEntity>> componentEntityList = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(factory, kcSession -> {
            EntityManager em = kcSession.getProvider(JpaConnectionProvider.class).getEntityManager();
            String sql = "select ce from ComponentEntity ce where ce.providerId = :providerId and lower(ce.realm.name) = lower(:realmId)";

            componentEntityList.set(em.createQuery(sql, ComponentEntity.class)
                    .setParameter("providerId", providerId)
                    .setParameter("realmId", realmId)
                    .getResultList());
        });

        return componentEntityList.get().stream();
    }

    public static Stream<ComponentModel> getComponents(KeycloakSessionFactory factory, RealmModel realm, String providerId) {
        return getComponents(factory, realm.getId(), providerId)
        .map(c -> realm.getComponent(c.getId()));
    }
}
