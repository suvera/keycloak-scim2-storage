package dev.suvera.keycloak.scim2.storage.storage;

import org.keycloak.models.KeycloakSession;

public class ScimSyncJobQueueManagerFactory {
    public static ScimSyncJobQueueManager create(KeycloakSession session) {
        return new ScimSyncJobQueueManager(session);
    }
}
