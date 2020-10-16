package dev.suvera.keycloak.scim2.storage.storage;

import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.types.UserResource;
import dev.suvera.keycloak.scim2.storage.jpa.SkssJobQueue;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.Set;

/**
 * author: suvera
 * date: 10/15/2020 8:43 AM
 */
public class SkssStorageProvider implements UserStorageProvider,
        UserRegistrationProvider,
        CredentialInputUpdater,
        UserLookupProvider {
    private static final Logger log = Logger.getLogger(SkssStorageProvider.class);
    private final KeycloakSession keycloakSession;
    private final ComponentModel componentModel;
    private final EntityManager em;
    private Scim2Client scimClient;

    public SkssStorageProvider(KeycloakSession keycloakSession, ComponentModel componentModel) {
        this.keycloakSession = keycloakSession;
        this.componentModel = componentModel;
        em = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();

        try {
            scimClient = Scim2ClientFactory.getClient(componentModel);
        } catch (ScimException e) {
            log.error("", e);
            scimClient = null;
        }
    }

    @Override
    public void close() {
    }

    /**
     * when a realm is removed
     */
    @Override
    public void preRemove(RealmModel realm) {
        // TODO:
    }

    /**
     * when a group is removed
     */
    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        if (scimClient == null) {
            return;
        }
        try {
            scimClient.deleteGroup(group);
        } catch (ScimException e) {
            log.error("", e);
        }
    }

    /**
     * when a role is removed
     */
    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // TODO:
    }

    /**
     * When a User is added
     */
    @Override
    public UserModel addUser(RealmModel realmModel, String username) {
        SkssJobQueue entity = new SkssJobQueue();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setAction("userCreate");
        entity.setRealmId(realmModel.getId());

        entity.setComponentId(componentModel.getId());
        entity.setUsername(username);
        entity.setProcessed(0);

        em.persist(entity);
        em.flush();

        return null;
    }

    /**
     * When user is removed
     */
    @Override
    public boolean removeUser(RealmModel realmModel, UserModel userModel) {
        if (userModel.getFirstAttribute("skss_id_" + componentModel.getId()) != null) {
            return false;
        }

        SkssJobQueue entity = new SkssJobQueue();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setAction("userDelete");
        entity.setRealmId(realmModel.getId());

        entity.setComponentId(componentModel.getId());
        entity.setUsername(userModel.getUsername());
        entity.setProcessed(0);
        entity.setExternalId(userModel.getFirstAttribute("skss_id_" + componentModel.getId()));

        em.persist(entity);
        em.flush();

        return true;
    }

    @Override
    public boolean supportsCredentialType(String s) {
        return false;
    }

    @Override
    public boolean updateCredential(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        if (credentialInput.getType().equals(CredentialModel.PASSWORD))
            throw new ReadOnlyException("user is read only for this update");

        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realmModel, UserModel userModel, String s) {
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realmModel, UserModel userModel) {
        return Collections.emptySet();
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        UserModel localModel = keycloakSession.userLocalStorage().getUserById(id, realm);
        if (localModel == null) {
            return null;
        }

        return getUser(new SkssUserModel(
                keycloakSession,
                realm,
                componentModel,
                localModel
        ));
    }

    private SkssUserModel getUser(SkssUserModel skssModel) {
        if (scimClient == null) {
            return null;
        }

        try {
            UserResource userResource = scimClient.getUser(skssModel);
            if (userResource == null) {
                return null;
            }

            skssModel.setUserResource(userResource);

            return skssModel;
        } catch (ScimException e) {
            log.error("", e);
            return null;
        }
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        UserModel localModel = keycloakSession.userLocalStorage().getUserByUsername(username, realm);

        if (localModel == null) {
            return null;
        }

        return getUser(new SkssUserModel(keycloakSession, realm, componentModel, localModel));
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        UserModel localModel = keycloakSession.userLocalStorage().getUserByEmail(email, realm);
        if (localModel == null) {
            return null;
        }
        return getUser(new SkssUserModel(
                keycloakSession,
                realm,
                componentModel,
                localModel
        ));
    }
}
