package dev.suvera.keycloak.scim2.storage.storage;

import dev.suvera.keycloak.scim2.storage.jpa.SkssJobQueue;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import javax.persistence.EntityManager;

/**
 * author: suvera
 * date: 10/15/2020 8:43 AM
 */
public class SkssStorageProvider implements UserStorageProvider, UserRegistrationProvider {
    private static final Logger log = Logger.getLogger(SkssStorageProvider.class);
    private final KeycloakSession session;
    private final ComponentModel model;
    private final EntityManager em;

    public SkssStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
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
        /*if (scimClient == null) {
            return;
        }
        try {
            scimClient.deleteGroup(group);
        } catch (ScimException e) {
            log.error("", e);
        }*/
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
        UserModel localUser = createAdapter(realmModel, username);

        SkssJobQueue entity = new SkssJobQueue();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setAction("userCreate");
        entity.setRealmId(realmModel.getName());
        entity.setComponentId(model.getId());
        entity.setUserId(localUser.getId());
        entity.setProcessed(0);
        em.persist(entity);
        log.info(username + " scheduled to be added on SCIM");

        return localUser;
    }

    protected UserModel createAdapter(RealmModel realm, String username) {
        UserModel local = session.userLocalStorage().getUserByUsername(realm, username);
        if (local == null) {
            log.info("Adding user " + username);
            local = session.userLocalStorage().addUser(realm, username);
            local.setFederationLink(model.getId());
        }

        return local;
    }

    /**
     * When user is removed
     */
    @Override
    public boolean removeUser(RealmModel realmModel, UserModel userModel) {
        // if user has been synchronized to SCIM server it'll have this external id entry
        if (userModel.getFirstAttribute("skss_id_" + model.getId()) != null) {
            SkssJobQueue entity = new SkssJobQueue();
            entity.setId(KeycloakModelUtils.generateId());
            entity.setAction("userDelete");
            entity.setRealmId(realmModel.getId());
            entity.setComponentId(model.getId());
            entity.setUserId(userModel.getId());
            entity.setProcessed(0);
            entity.setExternalId(userModel.getFirstAttribute("skss_id_" + model.getId()));
            em.persist(entity);
            em.flush();
            log.info(userModel.getUsername() + " scheduled to be removed on SCIM");
        }

        log.info("Removing user " + userModel.getUsername());
        return session.userLocalStorage().removeUser(realmModel, userModel);
    }
}
