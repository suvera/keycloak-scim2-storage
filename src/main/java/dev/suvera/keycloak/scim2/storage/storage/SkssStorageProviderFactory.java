package dev.suvera.keycloak.scim2.storage.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

import java.util.Date;
import java.util.List;

/**
 * author: suvera
 * date: 10/15/2020 8:54 AM
 */
public class SkssStorageProviderFactory implements UserStorageProviderFactory<SkssStorageProvider>, ImportSynchronization {
    protected static final List<ProviderConfigProperty> configMetadata;

    public static final String PROVIDER_ID = "skss-scim2-storage";

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property()
                .name("endPoint")
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("SCIM 2.0 url")
                .helpText("External SCIM 2.0 base URL")
                .add()

                .property()
                .name("authorityUrl")
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Authority url")
                .helpText("URL to the authority that protects SCIM server")
                .add()

                .property()
                .name("username")
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Username")
                .helpText("Username to access the scim resources")
                .add()

                .property()
                .name("password")
                .type(ProviderConfigProperty.PASSWORD)
                .label("Password")
                .helpText("Password to access the scim resources")
                .add()

                .property()
                .name("clientId")
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Client Id")
                .helpText("Client id to access the scim resources")
                .add()

                .property()
                .name("clientSecret")
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Client Secret")
                .helpText("Client secret to access the scim resources")
                .add()

                .build();
    }

    //private ScimSyncRunner syncRunner;

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
        if (config.get("endPoint") == null || config.get("endPoint").isEmpty()) {
            throw new ComponentValidationException("SCIM 2.0 endpoint is required.", "endPoint");
        }
        if (config.get("authorityUrl") == null || config.get("authorityUrl").isEmpty()) {
            throw new ComponentValidationException("Authority URL is required.", "authorityUrl");
        }
        if (config.get("clientId") == null || config.get("clientId").isEmpty()) {
            throw new ComponentValidationException("Client ID is required.", "clientId");
        }
    }

    @Override
    public SkssStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        return new SkssStorageProvider(keycloakSession, componentModel, JobEnqueuerFactory.create(keycloakSession));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId,
            UserStorageProviderModel model) {
        ScimSyncRunner syncRunner = new ScimSyncRunner(sessionFactory, model);
        return syncRunner.syncAll(realmId);
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId,
            UserStorageProviderModel model) {
        
        ScimSyncRunner syncRunner = new ScimSyncRunner(sessionFactory, model);
        return syncRunner.syncSince(lastSync, realmId);
    }
}
