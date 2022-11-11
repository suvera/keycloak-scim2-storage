package dev.suvera.keycloak.scim2.storage.storage;

import dev.suvera.keycloak.scim2.storage.jpa.FederatedUserEntity;
import dev.suvera.keycloak.scim2.storage.jpa.SkssJobQueue;
import dev.suvera.scim2.schema.ex.ScimException;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * author: suvera
 * date: 10/15/2020 8:43 AM
 */
public class SkssStorageProvider implements UserStorageProvider,
        UserRegistrationProvider,
        CredentialInputUpdater,
        UserLookupProvider,
        UserQueryProvider {
    private static final Logger log = Logger.getLogger(SkssStorageProvider.class);
    private final KeycloakSession keycloakSession;
    private final ComponentModel componentModel;
    private final EntityManager em;
    private ScimClient2 scimClient;
    public static final String PASSWORD_CACHE_KEY = FederatedUserAdapter.class.getName() + ".password";

    public SkssStorageProvider(KeycloakSession keycloakSession, ComponentModel componentModel) {
        this.keycloakSession = keycloakSession;
        this.componentModel = componentModel;
        em = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();

        try {
            scimClient = ScimClient2Factory.getClient(componentModel);
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
        FederatedUserEntity federatedEntity = new FederatedUserEntity();
        federatedEntity.setId(KeycloakModelUtils.generateId());
        federatedEntity.setUsername(username);
        em.persist(federatedEntity);
        log.info("added user: " + username);

        SkssJobQueue entity = new SkssJobQueue();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setAction("userCreate");
        entity.setRealmId(realmModel.getName());
        entity.setComponentId(componentModel.getId());
        entity.setUserId(federatedEntity.getId());
        entity.setProcessed(0);
        em.persist(entity);

        return new FederatedUserAdapter(keycloakSession, realmModel, componentModel, federatedEntity);
    }

    /**
     * When user is removed
     */
    @Override
    public boolean removeUser(RealmModel realmModel, UserModel userModel) {
        String persistenceId = StorageId.externalId(userModel.getId());
        FederatedUserEntity federatedEntity = em.find(FederatedUserEntity.class, persistenceId);
        if (federatedEntity == null) return false;

        SkssJobQueue entity = new SkssJobQueue();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setAction("userDelete");
        entity.setRealmId(realmModel.getId());
        entity.setComponentId(componentModel.getId());
        entity.setUserId(persistenceId);
        entity.setProcessed(0);
        entity.setExternalId(federatedEntity.getExternalId());
        em.persist(entity);

        em.remove(federatedEntity);

        return true;
    }

    @Override
    public boolean supportsCredentialType(String s) {
        return false;
    }

    @Override
    public boolean updateCredential(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        //noinspection deprecation
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
        log.info("getUserById: " + id);
        String persistenceId = StorageId.externalId(id);
        FederatedUserEntity federatedEntity = em.find(FederatedUserEntity.class, persistenceId);
        if (federatedEntity == null) {
            log.info("could not find user by id: " + id);
            return null;
        }
        return new FederatedUserAdapter(keycloakSession, realm, componentModel, federatedEntity);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        log.info("getUserByUsername: " + username);
        TypedQuery<FederatedUserEntity> query = em.createNamedQuery("getUserByUsername", FederatedUserEntity.class);
        query.setParameter("username", username);
        List<FederatedUserEntity> result = query.getResultList();
        if (result.isEmpty()) {
            log.info("could not find username: " + username);
            return null;
        }

        return new FederatedUserAdapter(keycloakSession, realm, componentModel, result.get(0));
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        TypedQuery<FederatedUserEntity> query = em.createNamedQuery("getUserByEmail", FederatedUserEntity.class);
        query.setParameter("email", email);
        List<FederatedUserEntity> result = query.getResultList();
        if (result.isEmpty()) return null;
        return new FederatedUserAdapter(keycloakSession, realm, componentModel, result.get(0));
    }
    @Override
    public int getUsersCount(RealmModel realm) {
        Object count = em.createNamedQuery("getUserCount")
                .getSingleResult();
        return ((Number)count).intValue();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, -1, -1);
    }

     @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        TypedQuery<FederatedUserEntity> query = em.createNamedQuery("getAllUsers", FederatedUserEntity.class);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<FederatedUserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (FederatedUserEntity entity : results) users.add(new FederatedUserAdapter(keycloakSession, realm, componentModel, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        TypedQuery<FederatedUserEntity> query = em.createNamedQuery("searchForUser", FederatedUserEntity.class);
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<FederatedUserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (FederatedUserEntity entity : results) users.add(new FederatedUserAdapter(keycloakSession, realm, componentModel, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }
}
