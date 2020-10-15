package dev.suvera.keycloak.scim2.storage.storage;

import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.types.UserResource;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Collections;
import java.util.Set;

/**
 * author: suvera
 * date: 10/15/2020 8:43 AM
 */
public class SkssStorageProvider implements UserStorageProvider,
        UserRegistrationProvider,
        CredentialInputUpdater,
        UserLookupProvider
{
    private static final Logger log = Logger.getLogger(SkssStorageProvider.class);
    private final KeycloakSession keycloakSession;
    private final ComponentModel componentModel;
    private final Scim2Client scimClient;

    public SkssStorageProvider(KeycloakSession keycloakSession, ComponentModel componentModel) {
        this.keycloakSession = keycloakSession;
        this.componentModel = componentModel;
        scimClient = new Scim2Client(componentModel);
        try {
            scimClient.validate();
        } catch (ScimException e) {
            log.error("", e);
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
        try {
            scimClient.createGroup(group);
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
        SkssUserModel model = createAdapter(realmModel, username);
        try {
            scimClient.createUser(model);
        } catch (ScimException e) {
            log.error("", e);
        }

        //return null;
        return model;
    }

    /**
     * When user is removed
     */
    @Override
    public boolean removeUser(RealmModel realmModel, UserModel userModel) {
        try {
            scimClient.deleteUser(userModel);
        } catch (ScimException e) {
            log.error("", e);
        }
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

    protected SkssUserModel createAdapter(RealmModel realm, String username) {
        UserModel localModel = keycloakSession.users().getUserByUsername(username, realm);
        //UserModel localModel = keycloakSession.userLocalStorage().getUserByUsername(username, realm);

        if (localModel == null) {
            throw new RuntimeException("Could not find user " + username + " in session.");
        }

        return new SkssUserModel(keycloakSession, realm, componentModel, localModel);
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
