package dev.suvera.keycloak.scim2.storage.idp;

import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import dev.suvera.keycloak.scim2.storage.storage.JobEnqueuer;
import dev.suvera.keycloak.scim2.storage.storage.JobEnqueuerFactory;

public class IdentityProviderEventMapper implements IdentityProviderMapper {
    private static final Logger log = Logger.getLogger(IdentityProviderEventMapper.class);
    private static final String[] COMPATIBLE_PROVIDERS = { ANY_PROVIDER };

    @Override
    public void close() { }

    @Override
    public IdentityProviderMapper create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Scope config) { }

    @Override
    public void postInit(KeycloakSessionFactory factory) { }

    @Override
    public String getId() {
        return "idp-to-scim-event-mapper";
    }

    @Override
    public String getHelpText() {
        return "Listens to when user gets updated and forwards the event to the SCIM provisioning plugin";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Event mapper";
    }

    @Override
    public String getDisplayType() {
        return "IdP to SCIM event mapper";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) { }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) { }

    @Override
    public void updateBrokeredUserLegacy(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        log.debugf("Updating user with legacy: %s", user.getUsername());
        JobEnqueuer job = JobEnqueuerFactory.create(session);
        job.enqueueUserCreateJob(realm, user);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        log.debugf("Updating user: %s", user.getUsername());
        JobEnqueuer job = JobEnqueuerFactory.create(session);
        job.enqueueUserCreateJob(realm, user);
    }
    
}
