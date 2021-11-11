package dev.suvera.keycloak.scim2.storage.storage;

import dev.suvera.scim2.schema.ex.ScimException;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * author: suvera
 * date: 10/15/2020 8:54 AM
 */
public class SkssStorageProviderFactory implements UserStorageProviderFactory<SkssStorageProvider> {
    protected static final List<ProviderConfigProperty> configMetadata;

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property()
                .name("endPoint")
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("SCIM 2.0 endPoint")
                .helpText("External SCIM 2.0 base " +
                        "URL (/ServiceProviderConfig  /Schemas and /ResourcesTypes should be accessible)")
                .add()

                .property()
                .name("username")
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Username")
                .helpText("Basic Auth username to access the scim resources")
                .add()

                .property()
                .name("password")
                .type(ProviderConfigProperty.PASSWORD)
                .label("Password")
                .helpText("Basic Auth password to access the scim resources")
                .add()

                .property()
                .name("bearerToken")
                .type(ProviderConfigProperty.PASSWORD)
                .label("Bearer Token")
                .helpText("Bearer Token based Authentication")
                .add()

                .build();
    }

    private ExecutorService backendService;
    private volatile boolean jobStarted = false;

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
        if (config.get("endPoint") == null || config.get("endPoint").isEmpty()) {
            throw new ComponentValidationException("SCIM 2.0 endPoint is required.", "endPoint");
        }

        ScimClient2 scimClient = new ScimClient2(config);
        try {
            scimClient.validate();
        } catch (ScimException e) {
            throw new ComponentValidationException(e.getMessage(), e);
        }
    }

    @Override
    public SkssStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        //startBackendJob(keycloakSession);
        return new SkssStorageProvider(keycloakSession, componentModel);
    }

    private synchronized void startBackendJob(KeycloakSession session) {
        if (jobStarted) {
            return;
        }

        jobStarted = true;

        backendService.execute(
                new Scim2SyncJob(session)
        );
    }

    @Override
    public String getId() {
        return "skss-scim2-storage";
    }

    @Override
    public void init(Config.Scope config) {
        backendService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void close() {
        backendService.shutdown();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        startBackendJob(factory.create());
    }
}
