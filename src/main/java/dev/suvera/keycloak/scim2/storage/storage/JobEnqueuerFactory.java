package dev.suvera.keycloak.scim2.storage.storage;

import org.keycloak.models.KeycloakSession;

public class JobEnqueuerFactory {
    private JobEnqueuerFactory() {
    }

    public static JobEnqueuer create(KeycloakSession session) {
        return new JobEnqueuer(session);
    }
}
