package dev.suvera.keycloak.scim2.storage.storage;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

/**
 * author: suvera
 * date: 10/15/2020 8:43 AM
 */
public class SkssStorageProvider implements UserStorageProvider, UserRegistrationProvider {
    private static final Logger log = Logger.getLogger(SkssStorageProvider.class);
    private final KeycloakSession session;
    private final ComponentModel model;
    private JobEnqueuer jobQueue;

    public SkssStorageProvider(KeycloakSession session, ComponentModel model, JobEnqueuer jobQueue) {
        this.session = session;
        this.model = model;
        this.jobQueue = jobQueue;
    }

    @Override
    public void close() {
    }

    /**
     * when a realm is removed
     */
    @Override
    public void preRemove(RealmModel realm) {
    }

    /**
     * when a group is removed
     */
    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
    }

    /**
     * when a role is removed
     */
    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
    }

    /**
     * When a User is added
     */
    @Override
    public UserModel addUser(RealmModel realmModel, String username) {
        UserModel localUser = createAdapter(realmModel, username);

        jobQueue.enqueueUserCreateJob(realmModel, model, localUser.getId());

        return localUser;
    }

    protected UserModel createAdapter(RealmModel realm, String username) {
        UserModel local = session.users().getUserByUsername(realm, username);
        if (local == null) {
            log.info("Adding user " + username);
            local = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, username);
            local.setFederationLink(model.getId());
        }

        return local;
    }

    /**
     * When user is removed
     */
    @Override
    public boolean removeUser(RealmModel realmModel, UserModel userModel) {
        log.info("Removing user " + userModel.getUsername());
        
        ScimUserAdapter scimUser = new ScimUserAdapter(session, realmModel, model, userModel);
        if (scimUser.getExternalId() != null) {
            jobQueue.enqueueUserDeleteJob(
                realmModel,
                model,
                userModel.getId(),
                scimUser.getExternalId());
            scimUser.removeExternalId();
        }

        return true;
    }
}
